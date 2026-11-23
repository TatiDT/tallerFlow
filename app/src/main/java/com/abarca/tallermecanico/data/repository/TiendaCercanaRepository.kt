package com.abarca.tallermecanico.data.repository

import com.abarca.tallermecanico.BuildConfig
import com.abarca.tallermecanico.TallerApp
import com.abarca.tallermecanico.util.TallerActual
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TiendaCercana(
    val id: Long,
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val direccion: String? = null,
    val telefono: String? = null,
    val horario: String? = null,
    val sitioWeb: String? = null,
    val correo: String? = null,
    val distanciaMetros: Double
)

class ErrorTiendas(val tipo: Tipo, detalle: String? = null) : Exception(detalle ?: tipo.name) {
    enum class Tipo { SIN_INTERNET, TIEMPO_AGOTADO, SERVICIO_SATURADO, SERVICIO_CAIDO, SIN_UBICACION }
}

object TiendaCercanaRepository {

    const val RADIO_POR_DEFECTO = 5000
    // Servidores públicos de Overpass: se prueban en orden si el anterior falla.
    private val SERVIDORES_OVERPASS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )
    private const val RONDAS = 2
    private const val ESPERA_ENTRE_RONDAS_MS = 1500L
    private const val RETRASO_RESPALDO_MS = 3000L
    // Códigos HTTP con los que Overpass avisa que está ocupado o sobrecargado.
    private val CODIGOS_SATURADO = setOf(429, 502, 503, 504)
    private const val VIGENCIA_CACHE_MS = 24L * 60 * 60 * 1000
    private const val NOMBRE_POR_DEFECTO = "Tienda de repuestos"
    private const val RADIO_TIERRA_METROS = 6371000.0

    suspend fun buscar(radioMetros: Int = RADIO_POR_DEFECTO): List<TiendaCercana> {
        val lat = BuildConfig.TALLER_LAT
        val lng = BuildConfig.TALLER_LNG
        if (lat == 0.0 && lng == 0.0) throw ErrorTiendas(ErrorTiendas.Tipo.SIN_UBICACION)

        leerCache(radioMetros, lat, lng)?.let { return it }

        val tiendas = try {
            consultarConReintentos(radioMetros, lat, lng)
        } catch (e: ErrorTiendas) {
            // Sin internet es un problema del usuario y no se reporta; el resto sí, para poder diagnosticarlo.
            if (e.tipo != ErrorTiendas.Tipo.SIN_INTERNET) FirebaseCrashlytics.getInstance().recordException(e)
            // Mejor mostrar datos guardados hace más de 24 h que mostrar un error.
            leerCache(radioMetros, lat, lng, ignorarVigencia = true)?.let { return it }
            throw e
        }
        guardarCache(radioMetros, lat, lng, tiendas)
        return tiendas
    }

    // Repite la consulta si todos los servidores fallan, salvo que simplemente no haya conexión.
    private suspend fun consultarConReintentos(radio: Int, lat: Double, lng: Double): List<TiendaCercana> {
        var ultimoError: ErrorTiendas? = null
        for (ronda in 1..RONDAS) {
            try {
                return consultarEnParalelo(radio, lat, lng)
            } catch (e: ErrorTiendas) {
                ultimoError = e
                FirebaseCrashlytics.getInstance().log("Overpass ronda $ronda: ${e.message}")
                if (e.tipo == ErrorTiendas.Tipo.SIN_INTERNET) break
                if (ronda < RONDAS) delay(ESPERA_ENTRE_RONDAS_MS)
            }
        }
        throw checkNotNull(ultimoError)
    }

    // Lanza la consulta al servidor principal y, si no responde en RETRASO_RESPALDO_MS, suma los de respaldo.
    // Gana la primera respuesta correcta; así un servidor lento no hace esperar al usuario.
    private suspend fun consultarEnParalelo(radio: Int, lat: Double, lng: Double): List<TiendaCercana> {
        // Alcance propio: las conexiones bloqueadas no se pueden interrumpir y no deben retrasar el resultado.
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val resultados = Channel<Result<List<TiendaCercana>>>(SERVIDORES_OVERPASS.size)
        try {
            SERVIDORES_OVERPASS.forEachIndexed { indice, servidor ->
                alcance.launch {
                    delay(indice * RETRASO_RESPALDO_MS)
                    resultados.send(runCatching { consultarOverpass(servidor, radio, lat, lng) })
                }
            }
            val errores = mutableListOf<ErrorTiendas>()
            repeat(SERVIDORES_OVERPASS.size) {
                val resultado = resultados.receive()
                resultado.getOrNull()?.let { return it }
                val causa = resultado.exceptionOrNull()
                errores += causa as? ErrorTiendas ?: ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_CAIDO, causa.toString())
            }
            throw errores.firstOrNull { it.tipo != ErrorTiendas.Tipo.SIN_INTERNET } ?: errores.last()
        } finally {
            alcance.cancel()
        }
    }

    fun distanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * RADIO_TIERRA_METROS * asin(sqrt(a))
    }

    private fun consultarOverpass(servidor: String, radio: Int, lat: Double, lng: Double): List<TiendaCercana> {
        // "qt" evita ordenar el resultado en el servidor, lo que hace la consulta más liviana.
        val consulta = "[out:json][timeout:15];nwr[\"shop\"=\"car_parts\"](around:$radio,$lat,$lng);out center tags qt;"
        val cuerpo = ("data=" + URLEncoder.encode(consulta, "UTF-8")).toByteArray(Charsets.UTF_8)
        var conexion: HttpURLConnection? = null
        try {
            conexion = (URL(servidor).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("User-Agent", TallerApp.userAgentTaller())
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conexion.outputStream.use { it.write(cuerpo) }
            val codigo = conexion.responseCode
            when {
                codigo == HttpURLConnection.HTTP_OK -> Unit
                codigo in CODIGOS_SATURADO -> throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_SATURADO, "HTTP $codigo")
                else -> throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_CAIDO, "HTTP $codigo")
            }
            val texto = conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return interpretar(texto, lat, lng)
        } catch (e: SocketTimeoutException) {
            throw ErrorTiendas(ErrorTiendas.Tipo.TIEMPO_AGOTADO, "Tiempo agotado: ${e.message}")
        } catch (e: IOException) {
            throw ErrorTiendas(ErrorTiendas.Tipo.SIN_INTERNET, "${e.javaClass.simpleName}: ${e.message}")
        } finally {
            conexion?.disconnect()
        }
    }

    private fun interpretar(texto: String, lat: Double, lng: Double): List<TiendaCercana> {
        val json = try {
            JSONObject(texto)
        } catch (e: Exception) {
            throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_CAIDO, "Respuesta no es JSON: ${texto.take(80)}")
        }
        // Overpass responde 200 con un "remark" cuando la consulta se queda sin tiempo o memoria.
        if (json.optString("remark").contains("runtime error", ignoreCase = true)) {
            throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_SATURADO, json.optString("remark").take(120))
        }
        val elementos = json.optJSONArray("elements") ?: return emptyList()

        val tiendas = mutableListOf<TiendaCercana>()
        for (i in 0 until elementos.length()) {
            val elemento = elementos.optJSONObject(i) ?: continue
            val centro = elemento.optJSONObject("center")
            val latTienda = if (elemento.has("lat")) elemento.optDouble("lat") else centro?.optDouble("lat")
            val lngTienda = if (elemento.has("lon")) elemento.optDouble("lon") else centro?.optDouble("lon")
            if (latTienda == null || lngTienda == null || latTienda.isNaN() || lngTienda.isNaN()) continue

            val etiquetas = elemento.optJSONObject("tags") ?: JSONObject()
            fun etiqueta(clave: String): String? = etiquetas.optString(clave, "").trim().ifEmpty { null }

            val calle = etiqueta("addr:street")
            val numero = etiqueta("addr:housenumber")
            val ciudad = etiqueta("addr:city")
            val direccion = listOfNotNull(
                listOfNotNull(calle, numero).joinToString(" ").ifEmpty { null },
                ciudad
            ).joinToString(", ").ifEmpty { null }

            tiendas += TiendaCercana(
                id = elemento.optLong("id"),
                nombre = etiqueta("name") ?: NOMBRE_POR_DEFECTO,
                lat = latTienda,
                lng = lngTienda,
                direccion = direccion,
                telefono = etiqueta("phone"),
                horario = etiqueta("opening_hours"),
                sitioWeb = etiqueta("website"),
                correo = etiqueta("email"),
                distanciaMetros = distanciaMetros(lat, lng, latTienda, lngTienda)
            )
        }
        return tiendas.sortedBy { it.distanciaMetros }
    }

    private suspend fun leerCache(
        radio: Int,
        lat: Double,
        lng: Double,
        ignorarVigencia: Boolean = false
    ): List<TiendaCercana>? {
        if (TallerActual.id == null) return null
        return try {
            val documento = TallerActual.coleccion("cacheTiendas").document(radio.toString()).get().esperar()
            if (!documento.exists()) return null
            val fecha = documento.getTimestamp("fecha")?.toDate()?.time ?: return null
            if (!ignorarVigencia && System.currentTimeMillis() - fecha > VIGENCIA_CACHE_MS) return null
            if (documento.getDouble("lat") != lat || documento.getDouble("lng") != lng) return null
            val lista = documento.get("tiendas") as? List<*> ?: return null
            lista.mapNotNull { desdeMapa(it as? Map<*, *>) }.sortedBy { it.distanciaMetros }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun guardarCache(radio: Int, lat: Double, lng: Double, tiendas: List<TiendaCercana>) {
        if (TallerActual.id == null) return
        try {
            val datos = mapOf(
                "radio" to radio,
                "lat" to lat,
                "lng" to lng,
                "fecha" to Timestamp(Date()),
                "tiendas" to tiendas.take(200).map { aMapa(it) }
            )
            TallerActual.coleccion("cacheTiendas").document(radio.toString()).set(datos).esperar()
        } catch (e: Exception) {
        }
    }

    private fun aMapa(tienda: TiendaCercana): Map<String, Any> {
        val mapa = mutableMapOf<String, Any>(
            "id" to tienda.id,
            "nombre" to tienda.nombre,
            "lat" to tienda.lat,
            "lng" to tienda.lng,
            "distanciaMetros" to tienda.distanciaMetros
        )
        tienda.direccion?.let { mapa["direccion"] = it }
        tienda.telefono?.let { mapa["telefono"] = it }
        tienda.horario?.let { mapa["horario"] = it }
        tienda.sitioWeb?.let { mapa["sitioWeb"] = it }
        tienda.correo?.let { mapa["correo"] = it }
        return mapa
    }

    private fun desdeMapa(mapa: Map<*, *>?): TiendaCercana? {
        mapa ?: return null
        return TiendaCercana(
            id = (mapa["id"] as? Number)?.toLong() ?: return null,
            nombre = mapa["nombre"] as? String ?: NOMBRE_POR_DEFECTO,
            lat = (mapa["lat"] as? Number)?.toDouble() ?: return null,
            lng = (mapa["lng"] as? Number)?.toDouble() ?: return null,
            direccion = mapa["direccion"] as? String,
            telefono = mapa["telefono"] as? String,
            horario = mapa["horario"] as? String,
            sitioWeb = mapa["sitioWeb"] as? String,
            correo = mapa["correo"] as? String,
            distanciaMetros = (mapa["distanciaMetros"] as? Number)?.toDouble() ?: return null
        )
    }

    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { continuacion.resume(it) }
        addOnFailureListener { continuacion.resumeWithException(it) }
    }
}
