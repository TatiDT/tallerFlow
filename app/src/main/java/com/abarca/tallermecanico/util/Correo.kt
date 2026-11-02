package com.abarca.tallermecanico.util

import android.util.Log
import android.util.Patterns
import com.abarca.tallermecanico.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object Correo {

    private val ejecutor = Executors.newSingleThreadExecutor()

    val configurado: Boolean get() = BuildConfig.CORREO_URL.isNotBlank()

    fun enviar(para: String, asunto: String, cuerpo: String) {
        if (!configurado || !Patterns.EMAIL_ADDRESS.matcher(para.trim()).matches()) {
            Log.d("Correo", "Omitido: configurado=$configurado, destinatario=$para")
            return
        }
        val datos = JSONObject()
            .put("token", BuildConfig.CORREO_TOKEN)
            .put("para", para.trim())
            .put("asunto", asunto)
            .put("cuerpo", cuerpo)
            .toString()
            .toByteArray(Charsets.UTF_8)
        ejecutor.execute {
            var conexion: HttpURLConnection? = null
            try {
                conexion = (URL(BuildConfig.CORREO_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                conexion.outputStream.use { it.write(datos) }
                Log.d("Correo", "Respuesta ${conexion.responseCode}")
            } catch (e: Exception) {
                Log.e("Correo", "Fallo envio", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                conexion?.disconnect()
            }
        }
    }
}
