package com.abarca.tallermecanico.ui.presupuesto

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.abarca.tallermecanico.BuildConfig
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.remote.TransbankClient
import com.abarca.tallermecanico.data.remote.TransbankCreateRequest
import com.abarca.tallermecanico.data.repository.VehiculoRepository
import com.abarca.tallermecanico.databinding.ActivityWebpayBinding
import com.abarca.tallermecanico.util.Aviso
import kotlinx.coroutines.launch
import java.util.UUID

class WebpayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebpayBinding
    private var vehiculoId: Int = -1
    private var monto: Long = 0L
    private var tokenActual: String? = null
    private var buyOrderActual: String = ""
    private var sessionIdActual: String = ""
    private var confirmandoPago = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebpayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehiculoId = intent.getIntExtra(EXTRA_VEHICULO_ID, -1)
        monto = intent.getLongExtra(EXTRA_MONTO, 0L)
        if (vehiculoId <= 0 || monto <= 0L) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        val returnUri = Uri.parse(BuildConfig.WEBPAY_RETURN_URL)
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (esUrlRetornoEstado(uri, returnUri)) {
                    manejarRetorno(uri)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progress.visibility = View.VISIBLE
                val uri = url?.let(Uri::parse) ?: return
                if (esUrlRetornoEstado(uri, returnUri)) {
                    manejarRetorno(uri)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!confirmandoPago) {
                    binding.progress.visibility = View.GONE
                }
            }
        }

        iniciarTransaccion()
    }

    private fun iniciarTransaccion() {
        binding.progress.visibility = View.VISIBLE
        val sufijo = UUID.randomUUID().toString().replace("-", "").take(10)
        buyOrderActual = "TF-$vehiculoId-$sufijo".take(26)
        sessionIdActual = "SES-$vehiculoId-$sufijo".take(61)

        lifecycleScope.launch {
            try {
                val respuesta = TransbankClient.api.crearTransaccion(
                    TransbankCreateRequest(
                        buyOrder = buyOrderActual,
                        sessionId = sessionIdActual,
                        amount = monto,
                        returnUrl = BuildConfig.WEBPAY_RETURN_URL
                    )
                )
                tokenActual = respuesta.token
                val urlEscapada = Html.escapeHtml(respuesta.url)
                val tokenEscapado = Html.escapeHtml(respuesta.token)
                val html = """
                    <!doctype html>
                    <html lang="es">
                    <body style="font-family:sans-serif;text-align:center;padding-top:48px;">
                        <p>${getString(R.string.msg_webpay_redirigiendo)}</p>
                        <form id="webpay" method="post" action="$urlEscapada">
                            <input type="hidden" name="token_ws" value="$tokenEscapado" />
                        </form>
                        <script>document.getElementById('webpay').submit();</script>
                    </body>
                    </html>
                """.trimIndent()
                binding.webView.loadDataWithBaseURL(respuesta.url, html, "text/html", "UTF-8", null)
            } catch (_: Exception) {
                Aviso.mostrar(this@WebpayActivity, R.string.msg_webpay_error_conexion)
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun esUrlRetornoEstado(uri: Uri, returnUri: Uri): Boolean {
        val mismoHost = uri.host.equals(returnUri.host, ignoreCase = true)
        val path = uri.path.orEmpty()
        return mismoHost && path.startsWith("/retorno/estado")
    }

    private fun manejarRetorno(uri: Uri) {
        if (confirmandoPago) return
        confirmandoPago = true
        binding.progress.visibility = View.VISIBLE

        val tokenWs = uri.getQueryParameter("token_ws")
        val tbkToken = uri.getQueryParameter("tbk_token") ?: uri.getQueryParameter("TBK_TOKEN")

        if (tokenWs.isNullOrBlank()) {
            val mensaje = if (!tbkToken.isNullOrBlank()) R.string.msg_webpay_cancelado else R.string.msg_webpay_rechazado
            Aviso.mostrar(this, mensaje)
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        if (tokenWs != tokenActual) {
            Aviso.mostrar(this, R.string.msg_webpay_rechazado)
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val res = TransbankClient.api.confirmarTransaccion(tokenWs)
                val coincide = res.buyOrder == buyOrderActual &&
                    res.sessionId == sessionIdActual &&
                    res.amount == monto
                if (res.aprobado && coincide) {
                    VehiculoRepository.marcarPagado(vehiculoId) { error ->
                        if (error == null) {
                            Aviso.mostrar(this@WebpayActivity, R.string.msg_webpay_aprobado)
                            setResult(RESULT_OK)
                        } else {
                            Aviso.mostrar(this@WebpayActivity, R.string.msg_webpay_error_guardar)
                            setResult(RESULT_CANCELED)
                        }
                        finish()
                    }
                } else {
                    Aviso.mostrar(this@WebpayActivity, R.string.msg_webpay_rechazado)
                    setResult(RESULT_CANCELED)
                    finish()
                }
            } catch (_: Exception) {
                Aviso.mostrar(this@WebpayActivity, R.string.msg_webpay_error_conexion)
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_VEHICULO_ID = "vehiculo_id"
        private const val EXTRA_MONTO = "monto"

        fun crearIntent(context: Context, vehiculoId: Int, monto: Long): Intent =
            Intent(context, WebpayActivity::class.java)
                .putExtra(EXTRA_VEHICULO_ID, vehiculoId)
                .putExtra(EXTRA_MONTO, monto)
    }
}
