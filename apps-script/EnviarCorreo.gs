const TOKEN = 'CAMBIA_ESTE_TOKEN';

function doPost(e) {
  const datos = JSON.parse(e.postData.contents);
  if (datos.token !== TOKEN) {
    return ContentService.createTextOutput('no autorizado');
  }
  MailApp.sendEmail({
    to: datos.para,
    subject: datos.asunto,
    body: datos.cuerpo,
    name: 'Taller Mecánico'
  });
  return ContentService.createTextOutput('ok');
}
