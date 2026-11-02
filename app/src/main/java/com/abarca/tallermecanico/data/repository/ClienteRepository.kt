package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Rut
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object ClienteRepository {

    private val coleccion get() = FirebaseFirestore.getInstance().collection("clientes")

    private val _clientes = MutableLiveData<List<Cliente>>(emptyList())
    val clientes: LiveData<List<Cliente>> = _clientes

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
            _clientes.value = snap.documents.mapNotNull { it.aCliente() }.sortedBy { it.id }
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

    fun insert(cliente: Cliente, alTerminar: (Throwable?) -> Unit) {
        val id = (_clientes.value.orEmpty().maxOfOrNull { it.id } ?: 0) + 1
        val nuevo = cliente.copy(id = id)
        FirestoreOps.guardar(coleccion.document(id.toString()), nuevo.aMapa(), true, alTerminar)
    }

    fun update(cliente: Cliente, alTerminar: (Throwable?) -> Unit) {
        FirestoreOps.guardar(coleccion.document(cliente.id.toString()), cliente.aMapa(), false, alTerminar)
    }

    fun delete(cliente: Cliente, alTerminar: (Throwable?) -> Unit) {
        FirestoreOps.eliminar(coleccion.document(cliente.id.toString()), alTerminar)
    }

    fun getById(id: Int): Cliente? = _clientes.value?.find { it.id == id }

    fun isRutDuplicate(rut: String, excludeId: Int = 0): Boolean =
        _clientes.value.orEmpty().any { Rut.normalizar(it.rut) == Rut.normalizar(rut) && it.id != excludeId }

    private fun Cliente.aMapa(): Map<String, Any> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "nombre" to nombre,
        "apellido" to apellido,
        "rut" to rut,
        "telefono" to telefono,
        "email" to email
    )

    private fun DocumentSnapshot.aCliente(): Cliente? {
        val clienteId = id.toIntOrNull() ?: return null
        return Cliente(
            id = clienteId,
            nombre = getString("nombre") ?: "",
            apellido = getString("apellido") ?: "",
            rut = getString("rut") ?: "",
            telefono = getString("telefono") ?: "",
            email = getString("email") ?: ""
        )
    }
}
