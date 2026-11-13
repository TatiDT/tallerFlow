package com.abarca.tallermecanico.data.repository

import com.abarca.tallermecanico.data.entity.Usuario
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.security.SecureRandom

object TallerRepository {

    private const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LARGO_CODIGO = 6
    private const val INTENTOS_CODIGO = 8

    private val db get() = FirebaseFirestore.getInstance()

    fun idPorCodigo(codigo: String, alTerminar: (String?, Throwable?) -> Unit) {
        db.collection("codigos").document(codigo.trim().uppercase()).get()
            .addOnSuccessListener { alTerminar(it.getString("tallerId"), null) }
            .addOnFailureListener { alTerminar(null, it) }
    }

    fun crear(uid: String, nombreTaller: String, admin: Usuario, alTerminar: (String?, Throwable?) -> Unit) {
        reservarCodigo(uid, INTENTOS_CODIGO) { codigo, error ->
            if (codigo == null) {
                alTerminar(null, error)
                return@reservarCodigo
            }
            val taller = db.collection("talleres").document(uid)
            val lote = db.batch()
            lote.set(taller, mapOf("nombre" to nombreTaller, "ownerUid" to uid, "codigo" to codigo))
            lote.set(taller.collection("usuarios").document(admin.id.toString()), UsuarioRepository.mapaDe(admin))
            lote.commit()
                .addOnSuccessListener { alTerminar(codigo, null) }
                .addOnFailureListener {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    alTerminar(null, it)
                }
        }
    }

    private fun reservarCodigo(uid: String, intentos: Int, alTerminar: (String?, Throwable?) -> Unit) {
        if (intentos == 0) {
            alTerminar(null, IllegalStateException("No hay código disponible"))
            return
        }
        val codigo = nuevoCodigo()
        val ref = db.collection("codigos").document(codigo)
        db.runTransaction { tx ->
            if (tx.get(ref).exists()) {
                throw FirebaseFirestoreException("Código en uso", FirebaseFirestoreException.Code.ABORTED)
            }
            tx.set(ref, mapOf("tallerId" to uid))
            null
        }
            .addOnSuccessListener { alTerminar(codigo, null) }
            .addOnFailureListener {
                if ((it as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.ABORTED) {
                    reservarCodigo(uid, intentos - 1, alTerminar)
                } else {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    alTerminar(null, it)
                }
            }
    }

    private fun nuevoCodigo(): String {
        val azar = SecureRandom()
        return (1..LARGO_CODIGO).map { ALFABETO[azar.nextInt(ALFABETO.length)] }.joinToString("")
    }
}
