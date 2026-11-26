const { createServer } = require('node:http');
const { randomBytes } = require('node:crypto');
const { Environment, IntegrationApiKeys, IntegrationCommerceCodes, Options, WebpayPlus } =
    require('transbank-sdk');

const port = Number(process.env.PORT) || 8787;
const origin = new URL(
    process.env.WEBPAY_ORIGIN ||
    process.env.RENDER_EXTERNAL_URL ||
    `http://127.0.0.1:${port}`
);
const host = process.env.WEBPAY_BIND || (process.env.PORT ? '0.0.0.0' : '127.0.0.1');
const tx = new WebpayPlus.Transaction(new Options(
    IntegrationCommerceCodes.WEBPAY_PLUS,
    IntegrationApiKeys.WEBPAY,
    Environment.Integration
));
const payments = new Map();
const escapeHtml = value => String(value).replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));

function page(res, status, title, body = '') {
    res.writeHead(status, {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'no-store',
        'Referrer-Policy': 'no-referrer'
    });
    res.end('<!doctype html><html lang="es"><meta charset="utf-8">' +
        '<meta name="viewport" content="width=device-width, initial-scale=1">' +
        '<title>Webpay de pruebas · TallerFlow</title>' +
        '<body style="max-width:36rem;margin:3rem auto;padding:0 1rem;font:18px system-ui">' +
        '<h1>' + title + '</h1>' + body + '</body></html>');
}

async function readForm(req, query) {
    if (req.method === 'GET') return query;
    let body = '';
    for await (const part of req) {
        body += part;
        if (body.length > 4096) throw new Error('Formulario demasiado grande');
    }
    return new URLSearchParams(body);
}

createServer(async (req, res) => {
    try {
        const url = new URL(req.url, origin);
        if (req.method === 'GET' && url.pathname === '/healthz') {
            res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
            return res.end('{"ok":true}');
        }
        if (url.pathname === '/retorno' && ['GET', 'POST'].includes(req.method)) {
            const fields = await readForm(req, url.searchParams);
            const params = new URLSearchParams();
            const tokenWs = fields.get('token_ws');
            const tbkToken = fields.get('TBK_TOKEN') || fields.get('tbk_token');
            if (tokenWs) params.set('token_ws', tokenWs);
            if (tbkToken) params.set('tbk_token', tbkToken);
            const destino = '/retorno/estado' + (params.toString() ? '?' + params.toString() : '');
            res.writeHead(303, { Location: destino, 'Cache-Control': 'no-store' });
            return res.end();
        }
        if (req.method === 'GET' && url.pathname === '/retorno/estado') {
            const cancelado = Boolean(url.searchParams.get('tbk_token'));
            const title = cancelado ? 'Pago cancelado' : 'Confirmando pago';
            const body = cancelado
                ? '<p>La operación fue cancelada. Puedes volver a TallerFlow.</p>'
                : '<p>Procesando la confirmación del pago en TallerFlow…</p>';
            return page(res, 200, title, body);
        }
        if (req.method === 'GET' && url.pathname === '/iniciar') {
            const amount = Number(url.searchParams.get('monto'));
            if (!Number.isSafeInteger(amount) || amount < 1 || amount > 999999999) {
                return page(res, 400, 'Monto de prueba inválido');
            }
            const id = randomBytes(12).toString('hex');
            const order = 'TF-' + id.slice(0, 20);
            const returnUrl = new URL('/resultado/' + id, origin).href;
            const result = await tx.create(order, id, amount, returnUrl);
            const checkout = new URL(result.url);
            if (checkout.origin !== 'https://webpay3gint.transbank.cl' || !result.token) {
                throw new Error('Respuesta inesperada al iniciar la transacción');
            }
            const payment = { order, amount, token: result.token };
            payments.set(id, payment);
            setTimeout(() => payments.delete(id), 45 * 60 * 1000).unref();
            return page(res, 200, 'Abriendo Webpay de pruebas',
                '<form id="webpay" method="post" action="' + escapeHtml(checkout.href) + '">' +
                '<input type="hidden" name="token_ws" value="' + escapeHtml(result.token) + '">' +
                '<button>Continuar a Transbank</button></form>' +
                '<script>document.getElementById("webpay").submit()</script>');
        }

        const callback = url.pathname.match(/^\/resultado\/([0-9a-f]{24})$/);
        if (!callback || !['GET', 'POST'].includes(req.method)) {
            return page(res, 404, 'Página no encontrada');
        }
        const id = callback[1];
        const payment = payments.get(id);
        if (!payment) return page(res, 404, 'La sesión de prueba expiró');
        const fields = await readForm(req, url.searchParams);
        const token = fields.get('token_ws');
        if (!token && fields.get('TBK_TOKEN') === payment.token) {
            return page(res, 200, 'Pago de prueba cancelado', '<p>Puedes volver a TallerFlow.</p>');
        }
        if (token !== payment.token) return page(res, 400, 'Retorno de Webpay inválido');

        payment.confirmation ??= tx.commit(token).catch(async error => {
            try {
                return await tx.status(token);
            } catch {
                throw error;
            }
        }).catch(error => {
            payment.confirmation = undefined;
            throw error;
        });
        const result = await payment.confirmation;
        const matches = result.buy_order === payment.order && result.session_id === id &&
            Number(result.amount) === payment.amount;
        if (!matches) return page(res, 502, 'Los datos de la transacción no coinciden');
        const approved = result.status === 'AUTHORIZED' && result.response_code === 0;
        const title = approved ? 'Pago de prueba aprobado' :
            result.status === 'INITIALIZED' ? 'Pago de prueba pendiente' : 'Pago de prueba rechazado';
        if (result.status === 'INITIALIZED') payment.confirmation = undefined;
        return page(res, 200, title,
            '<p>Orden: ' + escapeHtml(payment.order) + '</p>' +
            '<p>Monto: $' + payment.amount.toLocaleString('es-CL') + ' CLP</p>' +
            '<p>Ya puedes volver a TallerFlow.</p>');
    } catch {
        return page(res, 502, 'No se pudo completar la operación con Webpay',
            '<p>Comprueba la conexión a internet. Si ya intentaste pagar, vuelve a cargar esta página antes de iniciar otro pago.</p>');
    }
}).listen(port, host, () => {
    console.log('Webpay de pruebas listo en ' + origin.href);
});
