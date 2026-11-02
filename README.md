# TallerFlow

Aplicación Android para la gestión de un taller mecánico: clientes, vehículos, repuestos, usuarios, diagnósticos, presupuestos y órdenes de trabajo. Los datos se guardan en Cloud Firestore.

## Funciones

- Inicio de sesión con usuario y contraseña, y con Google.
- Roles: Administración, Jefe de Taller, Mecánico, Encargado de Repuesto y Encargado de Diagnosis.
- Asistente de chat con Gemini (Firebase AI Logic) disponible desde cualquier pantalla.
- Traducción de los textos de la aplicación con ML Kit.
- Correos automáticos al cliente mediante Google Apps Script.

## Configuración para compilar

Estos archivos no se incluyen en el repositorio porque contienen datos privados:

1. `app/google-services.json`: se descarga desde la consola de Firebase del proyecto.
2. `local.properties`, con estas claves además de `sdk.dir`:

```
correo.url=URL_DE_LA_APLICACION_WEB_DE_APPS_SCRIPT
correo.token=CLAVE_COMPARTIDA_CON_EL_SCRIPT
google.webClientId=ID_DE_CLIENTE_WEB.apps.googleusercontent.com
```

3. Reglas de Firestore: el contenido de `ReglasFirebase.txt` (hasta antes de `EXPLICACION`) se publica en Firestore → Reglas.
4. Script de correo: `apps-script/EnviarCorreo.gs` se publica como aplicación web en Google Apps Script.

Para firmar una versión de producción se necesita la clave de firma propia, que tampoco se guarda en el repositorio.

## Usuario inicial

La primera vez que la aplicación se conecta a una base vacía crea el usuario `admin`. Su contraseña debe cambiarse de inmediato desde la pestaña Usuarios.
