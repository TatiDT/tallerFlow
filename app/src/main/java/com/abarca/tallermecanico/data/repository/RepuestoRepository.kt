package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abarca.tallermecanico.data.entity.Repuesto
import com.abarca.tallermecanico.util.FirestoreOps
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object RepuestoRepository {

    private val coleccion get() = FirebaseFirestore.getInstance().collection("repuestos")

    private val _repuestos = MutableLiveData<List<Repuesto>>(emptyList())
    val repuestos: LiveData<List<Repuesto>> = _repuestos

    private val _cargando = MutableLiveData(true)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?> = _error

    private var registro: ListenerRegistration? = null

    fun iniciar() {
        if (registro != null) return
        registro = coleccion.addSnapshotListener { snap, error ->
            if (error != null || snap == null) {
                error?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                registro = null
                publicarError(error ?: IllegalStateException("Snapshot nulo"))
                return@addSnapshotListener
            }
            _error.value = null
            _repuestos.value = snap.documents.mapNotNull { it.aRepuesto() }.sortedBy { it.id }
            _cargando.value = false
        }
    }

    fun publicarError(causa: Throwable) {
        _error.value = causa
        _cargando.value = false
    }

    fun reiniciar() {
        registro?.remove()
        registro = null
        _error.value = null
        _cargando.value = true
    }

    fun insert(repuesto: Repuesto, alTerminar: (Throwable?) -> Unit) {
        val id = (_repuestos.value.orEmpty().maxOfOrNull { it.id } ?: 0) + 1
        val nuevo = repuesto.copy(id = id)
        FirestoreOps.guardar(coleccion.document(id.toString()), nuevo.aMapa(), true, alTerminar)
    }

    fun update(repuesto: Repuesto, alTerminar: (Throwable?) -> Unit) {
        FirestoreOps.guardar(coleccion.document(repuesto.id.toString()), repuesto.aMapa(), false, alTerminar)
    }

    fun delete(repuesto: Repuesto, alTerminar: (Throwable?) -> Unit) {
        FirestoreOps.eliminar(coleccion.document(repuesto.id.toString()), alTerminar)
    }

    fun getById(id: Int): Repuesto? = _repuestos.value?.find { it.id == id }

    fun isCodigoDuplicate(codigo: String, excludeId: Int = 0): Boolean =
        _repuestos.value.orEmpty().any { it.codigo.equals(codigo, ignoreCase = true) && it.id != excludeId }

    private fun Repuesto.aMapa(): Map<String, Any> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "nombre" to nombre,
        "codigo" to codigo,
        "categoria" to categoria,
        "precio" to precio,
        "stock" to stock
    )

    private fun DocumentSnapshot.aRepuesto(): Repuesto? {
        val repuestoId = id.toIntOrNull() ?: return null
        return Repuesto(
            id = repuestoId,
            nombre = getString("nombre") ?: "",
            codigo = getString("codigo") ?: "",
            categoria = getString("categoria") ?: "",
            precio = getLong("precio")?.toInt() ?: 0,
            stock = getLong("stock")?.toInt() ?: 0
        )
    }
}
