package com.abarca.tallermecanico.data.repository

import com.abarca.tallermecanico.BuildConfig
import com.abarca.tallermecanico.TallerApp
import com.abarca.tallermecanico.util.TallerActual
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
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

class ErrorTiendas(val tipo: Tipo) : Exception(tipo.name) {
    enum class Tipo { SIN_INTERNET, SERVICIO_SATURADO, SERVICIO_CAIDO, SIN_UBICACION }
}

object TiendaCercanaRepository {

    const val RADIO_POR_DEFECTO = 5000
    private const val URL_OVERPASS = "https://overpass-api.de/api/interpreter"
    private const val VIGENCIA_CACHE_MS = 24L * 60 * 60 * 1000
    private const val NOMBRE_POR_DEFECTO = "Tienda de repuestos"
    private const val RADIO_TIERRA_METROS = 6371000.0

    suspend fun buscar(radioMetros: Int = RADIO_POR_DEFECTO): List<TiendaCercana> {
        val lat = BuildConfig.TALLER_LAT
        val lng = BuildConfig.TALLER_LNG
        if (lat == 0.0 && lng == 0.0) throw ErrorTiendas(ErrorTiendas.Tipo.SIN_UBICACION)

        leerCache(radioMetros, lat, lng)?.let { return it }

        val tiendas = withContext(Dispatchers.IO) { consultarOverpass(radioMetros, lat, lng) }
        guardarCache(radioMetros, lat, lng, tiendas)
        return tiendas
    }

    fun distanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * RADIO_TIERRA_METROS * asin(sqrt(a))
    }

    private fun consultarOverpass(radio: Int, lat: Double, lng: Double): List<TiendaCercana> {
        val consulta = "[out:json][timeout:25];nwr[\"shop\"=\"car_parts\"](around:$radio,$lat,$lng);out center tags;"
        val cuerpo = ("data=" + URLEncoder.encode(consulta, "UTF-8")).toByteArray(Charsets.UTF_8)
        var conexion: HttpURLConnection? = null
        try {
            conexion = (URL(URL_OVERPASS).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 35000
                doOutput = true
                setRequestProperty("User-Agent", TallerApp.userAgentTaller())
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conexion.outputStream.use { it.write(cuerpo) }
            val codigo = conexion.responseCode
            when (codigo) {
                HttpURLConnection.HTTP_OK -> Unit
                429, HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_SATURADO)
                else -> throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_CAIDO)
            }
            val texto = conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return interpretar(texto, lat, lng)
        } catch (e: IOException) {
            throw ErrorTiendas(ErrorTiendas.Tipo.SIN_INTERNET)
        } finally {
            conexion?.disconnect()
        }
    }

    private fun interpretar(texto: String, lat: Double, lng: Double): List<TiendaCercana> {
        val elementos = try {
            JSONObject(texto).optJSONArray("elements")
        } catch (e: Exception) {
            throw ErrorTiendas(ErrorTiendas.Tipo.SERVICIO_CAIDO)
        } ?: return emptyList()

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

    private suspend fun leerCache(radio: Int, lat: Double, lng: Double): List<TiendaCercana>? {
        if (TallerActual.id == null) return null
        return try {
            val documento = TallerActual.coleccion("cacheTiendas").document(radio.toString()).get().esperar()
            if (!documento.exists()) return null
            val fecha = documento.getTimestamp("fecha")?.toDate()?.time ?: return null
            if (System.currentTimeMillis() - fecha > VIGENCIA_CACHE_MS) return null
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
