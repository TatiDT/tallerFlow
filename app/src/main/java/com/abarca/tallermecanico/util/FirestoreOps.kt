package com.abarca.tallermecanico.util

import android.content.Context
import com.abarca.tallermecanico.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreOps {

    fun guardar(ref: DocumentReference, datos: Map<String, Any?>, crear: Boolean, alTerminar: (Throwable?) -> Unit) {
        FirebaseFirestore.getInstance().runTransaction { tx ->
            if (crear && tx.get(ref).exists()) {
                throw FirebaseFirestoreException("Identificador en uso", FirebaseFirestoreException.Code.ABORTED)
            }
            tx.set(ref, datos)
            null
        }.addOnCompleteListener { registrar(it.exception, alTerminar) }
    }

    fun eliminar(ref: DocumentReference, alTerminar: (Throwable?) -> Unit) {
        FirebaseFirestore.getInstance().runTransaction { tx ->
            tx.delete(ref)
            null
        }.addOnCompleteListener { registrar(it.exception, alTerminar) }
    }

    private fun registrar(error: Throwable?, alTerminar: (Throwable?) -> Unit) {
        if (error != null) FirebaseCrashlytics.getInstance().recordException(error)
        alTerminar(error)
    }

    fun mensajeError(context: Context, error: Throwable): String {
        val codigo = (error as? FirebaseFirestoreException)?.code
        return when (codigo) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> context.getString(R.string.error_sin_conexion)
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> context.getString(R.string.error_sin_permiso)
            else -> context.getString(R.string.error_guardado)
        }
    }
}
