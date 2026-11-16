package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.util.PasswordUtils
import com.abarca.tallermecanico.util.Sesion
import com.abarca.tallermecanico.util.TallerActual
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.WriteBatch

object UsuarioRepository {

    private val coleccion = ColeccionFirestore(
        nombre = "usuarios",
        idDe = Usuario::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aUsuario() }
    )

    val usuarios: LiveData<List<Usuario>> = coleccion.items
    val cargando: LiveData<Boolean> = coleccion.cargando
    val error: LiveData<Throwable?> = coleccion.error
    val listo: LiveData<Boolean> = coleccion.listo

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    fun mapaDe(usuario: Usuario): Map<String, Any?> = usuario.aMapa()

    fun insert(usuario: Usuario) {
        val nuevo = usuario.copy(id = coleccion.siguienteId())
        coleccion.guardar(nuevo) { lote -> escribirAcceso(lote, nuevo) }
    }

    fun update(usuario: Usuario, verificadorPropio: String? = null) {
        val anterior = getById(usuario.id)
        coleccion.guardar(usuario) { lote ->
            if (anterior != null && !anterior.username.equals(usuario.username, ignoreCase = true)) {
                borrarAcceso(lote, anterior)
            }
            escribirAcceso(lote, usuario)
            val cuenta = FirebaseAuth.getInstance().currentUser
            if (verificadorPropio != null && cuenta != null && cuenta.isAnonymous &&
                !usuario.usaCuentaCorreo && Sesion.usuarioActual?.id == usuario.id
            ) {
                lote.set(
                    TallerActual.coleccion("miembros").document(cuenta.uid),
                    mapOf("usuarioId" to usuario.id, "verificador" to verificadorPropio)
                )
            }
        }
    }

    fun delete(usuario: Usuario) = coleccion.eliminar(usuario.id) { lote -> borrarAcceso(lote, usuario) }

    fun getById(id: Int): Usuario? = coleccion.valor.find { it.id == id }

    fun isUsernameDuplicate(username: String, excludeId: Int = 0): Boolean =
        coleccion.valor.any { it.username.equals(username, ignoreCase = true) && it.id != excludeId }

    fun isEmailDuplicate(email: String, excludeId: Int = 0): Boolean =
        email.isNotBlank() && coleccion.valor.any { it.email.equals(email, ignoreCase = true) && it.id != excludeId }

    fun autenticarPorEmail(email: String): Usuario? =
        coleccion.valor.find { it.email.isNotBlank() && it.email.equals(email, ignoreCase = true) && it.activo }

    private fun escribirAcceso(lote: WriteBatch, usuario: Usuario) {
        val sal = PasswordUtils.salDe(usuario.password) ?: return
        lote.set(
            TallerActual.coleccion("accesos").document(PasswordUtils.claveAcceso(usuario.username)),
            mapOf("usuarioId" to usuario.id, "sal" to sal)
        )
    }

    private fun borrarAcceso(lote: WriteBatch, usuario: Usuario) {
        if (usuario.usaCuentaCorreo) return
        lote.delete(TallerActual.coleccion("accesos").document(PasswordUtils.claveAcceso(usuario.username)))
    }

    private fun Usuario.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "nombre" to nombre,
        "apellido" to apellido,
        "username" to username,
        "email" to email,
        "password" to password,
        "rol" to rol.name,
        "activo" to activo
    )

    private fun DocumentSnapshot.aUsuario(): Usuario? {
        val usuarioId = id.toIntOrNull() ?: return null
        val rol = runCatching { RolUsuario.valueOf(getString("rol") ?: "") }.getOrNull() ?: return null
        return Usuario(
            id = usuarioId,
            nombre = getString("nombre") ?: "",
            apellido = getString("apellido") ?: "",
            username = getString("username") ?: "",
            email = getString("email") ?: "",
            password = getString("password") ?: "",
            rol = rol,
            activo = getBoolean("activo") ?: true
        )
    }
}
