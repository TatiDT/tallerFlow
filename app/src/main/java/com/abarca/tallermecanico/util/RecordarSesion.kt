package com.abarca.tallermecanico.util

import android.content.Context
import android.content.SharedPreferences
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth

object RecordarSesion {

    private const val PREFS = "recordar_sesion"
    private const val CLAVE_ID = "usuario_id"
    private const val CLAVE_HASH = "usuario_hash"
    private const val CLAVE_TALLER = "taller_id"
    private const val CLAVE_ULTIMO_CODIGO = "ultimo_codigo"

    private var prefs: SharedPreferences? = null

    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    val tallerId: String? get() = prefs?.getString(CLAVE_TALLER, null)

    val hayGuardada: Boolean
        get() = prefs?.let { it.getInt(CLAVE_ID, -1) != -1 && it.getString(CLAVE_TALLER, null) != null } ?: false

    var ultimoCodigo: String
        get() = prefs?.getString(CLAVE_ULTIMO_CODIGO, "").orEmpty()
        set(valor) {
            prefs?.edit()?.putString(CLAVE_ULTIMO_CODIGO, valor)?.apply()
        }

    fun guardar(usuario: Usuario) {
        val taller = TallerActual.id ?: return
        prefs?.edit()
            ?.putInt(CLAVE_ID, usuario.id)
            ?.putString(CLAVE_HASH, usuario.password)
            ?.putString(CLAVE_TALLER, taller)
            ?.apply()
    }

    fun actualizarSiGuardada(usuario: Usuario) {
        if (prefs?.getInt(CLAVE_ID, -1) == usuario.id) guardar(usuario)
    }

    fun borrar() {
        prefs?.edit()
            ?.remove(CLAVE_ID)
            ?.remove(CLAVE_HASH)
            ?.remove(CLAVE_TALLER)
            ?.apply()
    }

    fun restaurar(): Usuario? {
        val almacenadas = prefs ?: return null
        val id = almacenadas.getInt(CLAVE_ID, -1)
        val hash = almacenadas.getString(CLAVE_HASH, null)
        if (id == -1 || hash == null) return null
        val usuario = UsuarioRepository.getById(id) ?: return null
        if (!usuario.activo || usuario.password != hash || !cuentaVigente(usuario) || !claveVigente(usuario)) {
            borrar()
            return null
        }
        return usuario
    }

    private fun claveVigente(usuario: Usuario): Boolean =
        usuario.usaCuentaCorreo || PasswordUtils.esFormatoActual(usuario.password)

    private fun cuentaVigente(usuario: Usuario): Boolean {
        if (!usuario.usaCuentaCorreo) return true
        val cuenta = FirebaseAuth.getInstance().currentUser ?: return false
        return !cuenta.isAnonymous && cuenta.email.equals(usuario.email, ignoreCase = true)
    }
}
