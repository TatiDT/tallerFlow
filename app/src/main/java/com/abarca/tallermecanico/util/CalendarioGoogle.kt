package com.abarca.tallermecanico.util

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.abarca.tallermecanico.BuildConfig
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.data.entity.TipoAgenda
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CalendarioGoogle {

    private const val SCOPE = "https://www.googleapis.com/auth/calendar.events"
    private const val URL_EVENTOS = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
    private const val ZONA = "America/Santiago"
    private const val PREFS = "calendario_google"

    class NoAutorizado : Exception("La cuenta Google del taller no está conectada en este dispositivo")

    class ErrorCalendario(val codigo: Int, mensaje: String) : Exception(mensaje)

    fun conectado(ctx: Context): Boolean = prefs(ctx).getBoolean("conectado", false)

    fun cuenta(ctx: Context): String = prefs(ctx).getString("cuenta", "").orEmpty()

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun peticion(ctx: Context): AuthorizationRequest {
        val constructor = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE)))
        cuenta(ctx).takeIf { it.isNotBlank() }?.let { constructor.setAccount(Account(it, "com.google")) }
        return constructor.build()
    }

    suspend fun solicitarAutorizacion(ctx: Context): AuthorizationResult =
        Identity.getAuthorizationClient(ctx)
            .authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build())
            .esperar()

    fun resultadoDeIntent(ctx: Context, datos: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(ctx).getAuthorizationResultFromIntent(datos)

    fun registrarConexion(ctx: Context, resultado: AuthorizationResult) {
        prefs(ctx).edit()
            .putBoolean("conectado", true)
            .putString("cuenta", resultado.toGoogleSignInAccount()?.email.orEmpty())
            .apply()
    }

    suspend fun desconectar(ctx: Context) {
        val cuenta = cuenta(ctx)
        if (cuenta.isNotBlank()) {
            runCatching {
                val peticion = RevokeAccessRequest.builder()
                    .setAccount(Account(cuenta, "com.google"))
                    .setScopes(listOf(Scope(SCOPE)))
                    .build()
                Identity.getAuthorizationClient(ctx).revokeAccess(peticion).esperar()
            }
        }
        prefs(ctx).edit().clear().apply()
    }

    // El access token dura ~1 hora; authorize() devuelve uno vigente sin pedir nada al usuario
    // mientras el permiso siga concedido, así que no hay refresh token ni client_secret en la app.
    private suspend fun token(ctx: Context): String {
        if (!conectado(ctx)) throw NoAutorizado()
        val resultado = Identity.getAuthorizationClient(ctx).authorize(peticion(ctx)).esperar()
        if (resultado.hasResolution()) throw NoAutorizado()
        return resultado.accessToken ?: throw NoAutorizado()
    }

    suspend fun crear(ctx: Context, evento: EventoAgenda): String {
        val respuesta = solicitar(ctx, "POST", "$URL_EVENTOS?sendUpdates=all", cuerpoEvento(evento))
        return respuesta?.optString("id").orEmpty().ifBlank { throw IOException("Google no devolvió el id del evento") }
    }

    suspend fun reprogramar(ctx: Context, evento: EventoAgenda) {
        val cuerpo = JSONObject().put("start", momento(evento.inicio)).put("end", momento(evento.fin))
        solicitar(ctx, "PATCH", "$URL_EVENTOS/${evento.eventId}?sendUpdates=all", cuerpo)
    }

    suspend fun cancelar(ctx: Context, eventId: String) {
        try {
            solicitar(ctx, "DELETE", "$URL_EVENTOS/$eventId?sendUpdates=all", null)
        } catch (e: ErrorCalendario) {
            if (e.codigo != 404 && e.codigo != 410) throw e
        }
    }

    fun mensajeError(ctx: Context, causa: Throwable): String = when {
        causa is NoAutorizado -> ctx.getString(R.string.cal_error_no_conectado)
        causa is IOException -> ctx.getString(R.string.cal_error_sin_conexion)
        causa is ApiException && causa.statusCode == 7 -> ctx.getString(R.string.cal_error_sin_conexion)
        causa is ErrorCalendario && (causa.codigo == 401 || causa.codigo == 403) -> ctx.getString(R.string.cal_error_permiso)
        causa is ErrorCalendario -> ctx.getString(R.string.cal_error_google, causa.codigo)
        else -> ctx.getString(R.string.cal_error_generico)
    }

    private fun cuerpoEvento(e: EventoAgenda): JSONObject {
        val asistentes = JSONArray()
        listOf(e.clienteEmail, e.mecanicoEmail)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEach { asistentes.put(JSONObject().put("email", it)) }

        val recordatorios = JSONObject()
            .put("useDefault", false)
            .put(
                "overrides",
                JSONArray()
                    .put(JSONObject().put("method", "email").put("minutes", 24 * 60))
                    .put(JSONObject().put("method", "popup").put("minutes", 60))
            )

        return JSONObject()
            .put("summary", e.titulo)
            .put("description", descripcion(e))
            .put("start", momento(e.inicio))
            .put("end", momento(e.fin))
            .put("attendees", asistentes)
            .put("reminders", recordatorios)
            .apply { if (BuildConfig.TALLER_DIRECCION.isNotBlank()) put("location", BuildConfig.TALLER_DIRECCION) }
    }

    private fun descripcion(e: EventoAgenda): String {
        val lineas = mutableListOf("Vehículo: ${e.vehiculoDescripcion} (${e.vehiculoPatente})")
        lineas += if (e.tipo == TipoAgenda.CITA) "Servicio: ${e.detalle}" else "Trabajo: ${e.detalle}"
        if (e.mecanicoNombre.isNotBlank()) lineas += "Mecánico: ${e.mecanicoNombre}"
        if (BuildConfig.TALLER_TELEFONO.isNotBlank()) lineas += "Contacto del taller: ${BuildConfig.TALLER_TELEFONO}"
        return lineas.joinToString("\n")
    }

    private fun momento(millis: Long): JSONObject {
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone(ZONA) }
        return JSONObject().put("dateTime", formato.format(Date(millis))).put("timeZone", ZONA)
    }

    private suspend fun solicitar(ctx: Context, metodo: String, url: String, cuerpo: JSONObject?): JSONObject? =
        withContext(Dispatchers.IO) {
            var respuesta = ejecutar(metodo, url, cuerpo, token(ctx))
            // 401: el token pudo vencer justo ahora; se pide uno nuevo y se reintenta una vez
            if (respuesta.first == 401) respuesta = ejecutar(metodo, url, cuerpo, token(ctx))
            val (codigo, texto) = respuesta
            if (codigo !in 200..299) throw ErrorCalendario(codigo, extraerMensaje(texto))
            texto.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
        }

    private fun ejecutar(metodo: String, url: String, cuerpo: JSONObject?, token: String): Pair<Int, String> {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 15000
            conexion.readTimeout = 20000
            conexion.setRequestProperty("Authorization", "Bearer $token")
            conexion.setRequestProperty("Accept", "application/json")
            // HttpURLConnection no soporta PATCH: Google acepta POST con este encabezado
            if (metodo == "PATCH") {
                conexion.requestMethod = "POST"
                conexion.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                conexion.requestMethod = metodo
            }
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            return codigo to (flujo?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            conexion.disconnect()
        }
    }

    private fun extraerMensaje(texto: String): String =
        runCatching { JSONObject(texto).getJSONObject("error").getString("message") }.getOrDefault(texto.take(200))

    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { continuacion.resume(it) }
        addOnFailureListener { continuacion.resumeWithException(it) }
    }
}
