package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.util.PasswordUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

object UsuarioRepository {

    private const val ADMIN_INICIAL = "admin"
    private const val CLAVE_INICIAL = "admin123"

    private var sembrando = false

    private val coleccion = ColeccionFirestore(
        nombre = "usuarios",
        idDe = Usuario::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aUsuario() },
        alRecibir = { snap ->
            if (snap.isEmpty && !snap.metadata.isFromCache && !sembrando) sembrarAdmin()
        }
    )

    val usuarios: LiveData<List<Usuario>> = coleccion.items
    val cargando: LiveData<Boolean> = coleccion.cargando
    val error: LiveData<Throwable?> = coleccion.error

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    private fun sembrarAdmin() {
        sembrando = true
        val admin = Usuario(
            id = 1,
            nombre = "Administrador",
            apellido = "Taller",
            username = ADMIN_INICIAL,
            password = PasswordUtils.hash(CLAVE_INICIAL),
            rol = RolUsuario.ADMIN
        )
        coleccion.crear(admin) { sembrando = false }
    }

    fun insert(usuario: Usuario) = coleccion.guardar(usuario.copy(id = coleccion.siguienteId()))

    fun update(usuario: Usuario) = coleccion.guardar(usuario)

    fun delete(usuario: Usuario) = coleccion.eliminar(usuario.id)

    fun getById(id: Int): Usuario? = coleccion.valor.find { it.id == id }

    fun isUsernameDuplicate(username: String, excludeId: Int = 0): Boolean =
        coleccion.valor.any { it.username.equals(username, ignoreCase = true) && it.id != excludeId }

    fun isEmailDuplicate(email: String, excludeId: Int = 0): Boolean =
        email.isNotBlank() && coleccion.valor.any { it.email.equals(email, ignoreCase = true) && it.id != excludeId }

    fun autenticarPorEmail(email: String): Usuario? =
        coleccion.valor.find { it.email.isNotBlank() && it.email.equals(email, ignoreCase = true) && it.activo }

    fun autenticar(username: String, password: String): Usuario? {
        val usuario = coleccion.valor
            .find { it.username.equals(username, ignoreCase = true) && it.activo }
            ?: return null
        return if (PasswordUtils.verify(password, usuario.password)) usuario else null
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
