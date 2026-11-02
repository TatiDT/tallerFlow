package com.abarca.tallermecanico

import android.util.Log
import com.abarca.tallermecanico.util.Notificaciones
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MensajeriaService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "Token: $token")
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val titulo = mensaje.notification?.title ?: mensaje.data["titulo"] ?: getString(R.string.app_name)
        val cuerpo = mensaje.notification?.body ?: mensaje.data["mensaje"] ?: return
        Notificaciones.mostrar(this, titulo, cuerpo)
    }
}
