package com.abarca.tallermecanico.data.repository

import com.abarca.tallermecanico.util.PasswordUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

object AccesoPersonal {
    private val db get() = FirebaseFirestore.getInstance()

    fun ingresar(tallerId: String, username: String, password: String, alTerminar: (Int?, Throwable?) -> Unit) {
        conCuenta { cuenta, errorCuenta ->
            if (cuenta == null) {
                alTerminar(null, errorCuenta ?: IllegalStateException("Sin sesión de Firebase"))
                return@conCuenta
            }
            val taller = db.collection("talleres").document(tallerId)
            taller.collection("accesos").document(PasswordUtils.claveAcceso(username)).get()
                .addOnSuccessListener { acceso ->
                    val usuarioId = acceso.getLong("usuarioId")?.toInt()
                    val verificador = acceso.getString("sal")?.let { PasswordUtils.hashConSal(password, it) }
                    if (usuarioId == null || verificador == null) {
                        alTerminar(null, null)
                        return@addOnSuccessListener
                    }
                    taller.collection("miembros").document(cuenta.uid)
                        .set(mapOf("usuarioId" to usuarioId, "verificador" to verificador))
                        .addOnSuccessListener { alTerminar(usuarioId, null) }
                        .addOnFailureListener { error ->
                            val denegado = (error as? FirebaseFirestoreException)?.code ==
                                FirebaseFirestoreException.Code.PERMISSION_DENIED
                            if (denegado) alTerminar(null, null) else alTerminar(null, error)
                        }
                }
                .addOnFailureListener { alTerminar(null, it) }
        }
    }

    private fun conCuenta(alTerminar: (FirebaseUser?, Throwable?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { alTerminar(it, null); return }
        auth.signInAnonymously()
            .addOnSuccessListener { alTerminar(it.user, null) }
            .addOnFailureListener { alTerminar(null, it) }
    }
}
