package com.abarca.tallermecanico.util

import android.content.Context
import android.os.Bundle
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsLogger {

    fun logLoginExitoso(context: Context, rol: RolUsuario) {
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, rol.name)
        })
    }

    fun logLoginFallido(context: Context) {
        FirebaseAnalytics.getInstance(context).logEvent("login_fallido", null)
    }

    fun logLogout(context: Context, rol: RolUsuario?) {
        FirebaseAnalytics.getInstance(context).logEvent("logout", Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, rol?.name ?: "desconocido")
        })
    }

    fun logCrud(context: Context, entidad: String, accion: String) {
        FirebaseAnalytics.getInstance(context).logEvent("crud_accion", Bundle().apply {
            putString("entidad", entidad)
            putString("accion", accion)
        })
    }

    fun logRegistroCreado(context: Context, tipoEntidad: String) = logRegistro(context, "registro_creado", tipoEntidad)

    fun logRegistroActualizado(context: Context, tipoEntidad: String) = logRegistro(context, "registro_actualizado", tipoEntidad)

    fun logRegistroEliminado(context: Context, tipoEntidad: String) = logRegistro(context, "registro_eliminado", tipoEntidad)

    fun logListadoConsultado(context: Context, tipoEntidad: String) = logRegistro(context, "listado_consultado", tipoEntidad)

    private fun logRegistro(context: Context, evento: String, tipoEntidad: String) {
        FirebaseAnalytics.getInstance(context).logEvent(evento, Bundle().apply {
            putString("tipo_entidad", tipoEntidad)
        })
    }
}
