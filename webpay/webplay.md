# Webpay Plus de pruebas

Usa la pantalla oficial de Transbank en ambiente de integración. El resultado aparece en el navegador cuando Transbank devuelve el control al servicio local. El servicio conserva la sesión en memoria durante 45 minutos. La integración es de demostración, no registra pagos en Firebase y no utiliza tarjetas reales.

## Preparar Android Studio

1. Abre la carpeta TallerFlow descomprimida.
2. Agrega el google-services.json del proyecto Firebase de TallerFlow en app/.
3. Conecta el teléfono por USB, activa la depuración USB y acepta el aviso del teléfono. También puedes usar el emulador de Android Studio.

## Iniciar Webpay en el PC

Con Node.js 18 o posterior, abre una terminal en TallerFlow y ejecuta:

    cd webpay
    npm install --no-save --no-package-lock transbank-sdk
    node server.cjs

Deja esa terminal abierta. En otra terminal PowerShell, ejecuta:

    & "C:\Users\dlobo\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8787 tcp:8787

Ajusta la ruta de adb si tu SDK está en otra ubicación. Si hay varios dispositivos, elige uno usando adb -s SERIAL reverse tcp:8787 tcp:8787. En el navegador del dispositivo, http://127.0.0.1:8787/ debe mostrar «Página no encontrada», lo que confirma que el servicio responde.

## Realizar el pago de prueba

Ejecuta TallerFlow desde Android Studio. Como administrador, abre el presupuesto aprobado de un vehículo desde la lista de Vehículos. Pulsa Pagar con Webpay (prueba) dentro del presupuesto. En el formulario de Transbank, usa las tarjetas e identidades de prueba de la documentación oficial:

https://www.transbankdevelopers.cl/documentacion/como_empezar#como-probar

- Visa de prueba: 4051 8856 0044 6623.
- CVV: 123.
- Vencimiento si se solicita: una fecha futura, por ejemplo 12/29.
- RUT del banco de prueba si se solicita: 11.111.111-1.
- Clave del banco de prueba: 123.

Usa estos datos solo en webpay3gint.transbank.cl.

Completa o cancela la operación. Webpay volverá al servicio local a través del navegador y mostrará el resultado. Usa el botón Atrás del dispositivo para volver a TallerFlow.

Para usar una conexión de red local en lugar de USB, configura WEBPAY_ORIGIN en PowerShell con http://IP_DEL_PC:8787 y WEBPAY_BIND con la IP del PC antes de iniciar node. Añade webpay.testUrl=http://IP_DEL_PC:8787 a local.properties y recompila la copia. El teléfono y el PC deben compartir red y el puerto debe estar permitido por el firewall de Windows.
