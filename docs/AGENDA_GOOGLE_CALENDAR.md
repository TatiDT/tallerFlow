# Agenda con Google Calendar

Las citas y las intervenciones se guardan en Firestore y se sincronizan con el Google Calendar de la cuenta Google del taller. Google envía al cliente (y al mecánico, en intervenciones) la invitación, el aviso de reprogramación y el de cancelación.

## Cómo funciona

- No hay backend. La app llama directamente a Google Calendar API v3 con OAuth 2.0 (scope `https://www.googleapis.com/auth/calendar.events`) usando la API de autorización de Google Play Services.
- No se usa cuenta de servicio ni existe `client_secret` ni refresh token en la app. El access token dura cerca de 1 hora y se renueva solo mientras el permiso siga concedido.
- Operaciones: `events.insert` (crear), `events.patch` (reprogramar) y `events.delete` (cancelar), todas con `sendUpdates=all`. Zona horaria `America/Santiago`.
- Primero se guarda en Firestore. Si falla Google (sin conexión, permiso revocado, cuenta no conectada), el registro queda con `syncError`, la lista muestra "Sin sincronizar" y el detalle ofrece **Reintentar sincronización**.
- Roles: `ADMIN` y `JEFE_TALLER` crean, reprograman y cancelan. Solo `ADMIN` ve el botón para conectar o desconectar la cuenta Google y el botón "Sincronizar pendientes".
- La conexión es **por dispositivo**. Si un jefe de taller crea una cita desde un teléfono que no tiene la cuenta conectada, la cita queda pendiente y el administrador la sincroniza desde su teléfono con "Sincronizar pendientes".

## Archivos

| Archivo | Función |
| --- | --- |
| `util/CalendarioGoogle.kt` | Autorización, llamadas REST a Calendar y mensajes de error |
| `util/SincronizadorAgenda.kt` | Decide si crear, reprogramar o cancelar y guarda el resultado |
| `util/FechaAgenda.kt` | Fechas y horas en `America/Santiago` |
| `data/entity/EventoAgenda.kt` | Modelo de cita e intervención |
| `data/repository/AgendaRepository.kt` | Colecciones `citas` e `intervenciones` en Firestore |
| `ui/agenda/` | Lista, formulario y detalle |
| `firestore.rules` | Reglas de `citas` e `intervenciones` |

Las colecciones están dentro del taller: `talleres/{tallerId}/citas` y `talleres/{tallerId}/intervenciones`. Los ids son numéricos, igual que el resto de la app, y `ordenTrabajoId` es el id del vehículo.

## Configurar Google Cloud Console

1. **Proyecto.** Entra a <https://console.cloud.google.com> y selecciona el proyecto de Firebase de TallerFlow (un proyecto de Firebase es un proyecto de Google Cloud). Si prefieres uno separado, créalo con *Seleccionar proyecto → Proyecto nuevo*, pero entonces el cliente OAuth debe estar en el mismo proyecto que usa la app.
2. **Habilitar la API.** *APIs y servicios → Biblioteca → Google Calendar API → Habilitar*.
3. **Pantalla de consentimiento.** *APIs y servicios → Pantalla de consentimiento de OAuth* (o *Google Auth Platform → Branding/Público*):
   - Tipo de usuario: **Externo**.
   - Nombre de la app: TallerFlow. Correo de asistencia y de contacto: el del taller.
   - *Acceso a los datos → Agregar o quitar permisos*: agrega `https://www.googleapis.com/auth/calendar.events`.
   - Estado de publicación: **En prueba**.
   - *Usuarios de prueba*: agrega la cuenta Google del taller (la dueña del calendario). Solo las cuentas listadas pueden autorizar la app.
4. **Credenciales.** *APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth*:
   - Tipo de aplicación: **Android**.
   - Nombre del paquete: `com.abarca.tallermecanico`.
   - Huella SHA-1 del certificado con que se firma la app:
     - Debug: `./gradlew signingReport` y copia el SHA1 de la variante `debug`.
     - Release: `keytool -list -v -keystore mi-clave-taller.jks` y copia el SHA1.
     - Si publicas en Google Play con Play App Signing, agrega también el SHA-1 que muestra *Play Console → Integridad de la app*.
   - Crea un cliente por cada SHA-1 distinto. Firebase puede haber creado ya alguno: revisa la lista antes.
5. **Dónde poner cada valor.**
   - ID de cliente Android: no se escribe en ningún archivo. Google lo asocia solo por paquete + SHA-1.
   - `client_secret`: no existe para clientes Android y no se necesita.
   - Dirección y teléfono del taller (van en el evento): agrega a `local.properties`, que no se sube al repositorio:

     ```
     taller.direccion=Av. Ejemplo 1234, Santiago
     taller.telefono=+56 9 1234 5678
     ```

6. **Reglas de Firestore.** Publica el contenido de `firestore.rules` en *Firebase Console → Firestore → Reglas*, o con `firebase deploy --only firestore:rules`. `ReglasFirebase.txt` es una copia antigua (sin la estructura `talleres/{tallerId}`) y no debe publicarse.
7. **Conectar la cuenta en la app.** Inicia sesión como administrador, abre la pestaña **Agenda** y pulsa **Conectar cuenta Google del taller**. Elige la cuenta del taller y acepta el permiso. Google mostrará "Google no ha verificado esta aplicación": pulsa *Continuar*, es normal en modo prueba.

### Limitaciones a tener en cuenta

- En modo **prueba** con un scope sensible, Google caduca la autorización a los **7 días**. El administrador debe volver a conectar la cuenta cada semana. Para evitarlo hay que pasar la pantalla de consentimiento a *En producción* y completar la verificación de Google.
- Los recordatorios del evento (correo 24 h antes y notificación 1 h antes) se aplican al calendario del organizador (la cuenta del taller). Al cliente le llegan la invitación y las actualizaciones; sus recordatorios dependen de la configuración de su propio calendario.
- El recordatorio adicional con la extensión "Trigger Email" y una función programada **no se implementó**: requiere plan Blaze y Cloud Functions, y el proyecto no tiene backend.

## Reglas de Firestore

Ver `firestore.rules`, bloques `citaValida()`, `intervencionValida()` y `agendaComunValida()`. Resumen:

- Solo usuarios con sesión de Firebase pueden leer o escribir (igual que el resto de colecciones del proyecto).
- Solo se aceptan los campos del modelo. El correo del cliente debe tener formato válido, la duración va de 15 a 720 minutos y el estado debe ser uno de `agendada`, `reprogramada`, `cancelada` o `realizada`.
- No se puede borrar: cancelar cambia el estado a `cancelada`.
- La fecha futura se valida en la app. No se valida en las reglas porque un documento creado sin conexión llega a Firestore después, y el evento podría quedar bloqueado.
- Los roles (`ADMIN` y `JEFE_TALLER`) se controlan en la app, como en el resto del proyecto: Firestore solo distingue sesión válida o no.

## Plan de pruebas

Prerrequisitos: reglas publicadas, cuenta del taller como usuario de prueba, cuenta conectada desde el administrador, un cliente con un correo **distinto** a la cuenta del taller (si es la misma, Google no envía el correo) y un vehículo de ese cliente. Para intervenciones, el vehículo debe tener un mecánico asignado y estar en estado Ingresado o En reparación.

| # | Prueba | Pasos | Resultado esperado |
| --- | --- | --- | --- |
| 1 | Crear cita | Agenda → Nueva cita → elegir cliente, vehículo, servicio, fecha y hora futuras, duración 60 → Guardar | Aviso "Guardado y sincronizado". La cita aparece en la lista con "Sincronizada con Google Calendar". En el calendario del taller hay un evento "Cita TallerFlow – {servicio} ({patente})" con ubicación, descripción y el cliente como invitado. En Firestore el documento tiene `eventId` y `syncError` vacío. **El cliente recibe el correo de invitación en su bandeja.** |
| 2 | Validaciones | Probar correo sin formato válido, fecha de hoy con hora pasada, duración 5 y campos vacíos | El formulario marca cada error y no guarda nada |
| 3 | Reprogramar | Abrir la cita → Reprogramar → nueva fecha y hora | Estado "Reprogramada". El evento cambia de horario en Calendar (mismo `eventId`). **El cliente recibe el correo de actualización.** |
| 4 | Cancelar | Abrir la cita → Cancelar cita → Confirmar | Estado "Cancelada". El evento desaparece del calendario. **El cliente recibe el correo de cancelación.** No aparecen botones de reprogramar ni cancelar |
| 5 | Programar intervención | Vehículos → abrir un vehículo con mecánico → menú ⋮ → Programar intervención → fecha, hora, duración y mecánico → Guardar | Evento "Intervención TallerFlow – {trabajo} ({patente})". **Cliente y mecánico (si tiene correo) reciben la invitación.** Aparece en Agenda con el tipo "Intervención" |
| 6 | Error sin conexión | Activar modo avión → crear una cita → Guardar | La cita se guarda igual. Aviso "Guardado, pero sin sincronizar… Sin conexión con Google". En la lista aparece "Sin sincronizar" en color de alerta |
| 7 | Reintentar | Desactivar el modo avión → abrir esa cita → Reintentar sincronización | Se crea el evento, el indicador pasa a "Sincronizada" y `syncError` queda vacío. El cliente recibe la invitación |
| 8 | Permiso revocado | En <https://myaccount.google.com/permissions> quitar el acceso a TallerFlow → crear una cita | Queda con `syncError` "Permiso vencido o revocado…". Tras reconectar la cuenta y pulsar Reintentar, se sincroniza |
| 9 | Dispositivo sin cuenta | Con un jefe de taller en otro teléfono, crear una cita | Queda "La cuenta Google del taller no está conectada en este dispositivo". En el teléfono del administrador, Agenda → Sincronizar pendientes la sincroniza |
| 10 | Desconectar | Administrador → Agenda → Desconectar | La cabecera muestra "no está conectado". Las citas nuevas quedan pendientes |
| 11 | Roles | Entrar como mecánico, repuestos y diagnosis | No tienen la pestaña Agenda. El jefe de taller la ve, pero sin botón de conectar cuenta |
| 12 | Renovación de token | Dejar la app abierta más de 1 hora sin cerrar sesión y crear una cita | Se sincroniza sin pedir autorización otra vez |
