package com.abarca.tallermecanico.util

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object TallerActual {

    var id: String? = null
        private set
    var nombre: String = ""
        private set
    var codigo: String = ""
        private set

    fun coleccion(nombreColeccion: String): CollectionReference =
        FirebaseFirestore.getInstance()
            .collection("talleres")
            .document(checkNotNull(id) { "No hay un taller activo" })
            .collection(nombreColeccion)

    fun fijar(idTaller: String, nombreTaller: String = "", codigoTaller: String = "") {
        id = idTaller
        nombre = nombreTaller
        codigo = codigoTaller
        if (codigoTaller.isEmpty()) cargarDatos(idTaller)
    }

    fun limpiar() {
        id = null
        nombre = ""
        codigo = ""
    }

    private fun cargarDatos(idTaller: String) {
        FirebaseFirestore.getInstance().collection("talleres").document(idTaller).get()
            .addOnSuccessListener {
                if (id != idTaller) return@addOnSuccessListener
                nombre = it.getString("nombre").orEmpty()
                codigo = it.getString("codigo").orEmpty()
            }
    }
}
