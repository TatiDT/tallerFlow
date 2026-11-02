package com.abarca.tallermecanico.util

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.abarca.tallermecanico.MainActivity
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario

object Sesion {
    var usuarioActual: Usuario? = null

    fun tieneRol(vararg roles: RolUsuario): Boolean = usuarioActual?.rol in roles

    fun cerrar() {
        usuarioActual = null
    }
}

fun Activity.irAlLogin() {
    CoroutineScope(Dispatchers.Main).launch { AuthGoogle.limpiar(applicationContext) }
    startActivity(
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
    finish()
}
