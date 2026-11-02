package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abarca.tallermecanico.util.FirestoreOps
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class ColeccionFirestore<T>(
    private val nombre: String,
    private val idDe: (T) -> Int,
    private val aMapa: (T) -> Map<String, Any?>,
    private val deDocumento: (DocumentSnapshot) -> T?,
    private val alRecibir: ((QuerySnapshot) -> Unit)? = null
) {

    private val coleccion get() = FirebaseFirestore.getInstance().collection(nombre)

    private val _items = MutableLiveData<List<T>>(emptyList())
    val items: LiveData<List<T>> = _items

    private val _cargando = MutableLiveData(true)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?> = _error

    val valor: List<T> get() = _items.value.orEmpty()

    private var registro: ListenerRegistration? = null
    private var ultimoId = 0

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
            _items.value = snap.documents.mapNotNull(deDocumento).sortedBy(idDe)
            _cargando.value = false
            alRecibir?.invoke(snap)
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

    fun siguienteId(): Int {
        ultimoId = maxOf(ultimoId, valor.maxOfOrNull(idDe) ?: 0) + 1
        return ultimoId
    }

    fun guardar(item: T) = guardarVarios(listOf(item))

    fun guardarVarios(nuevos: List<T>) {
        if (nuevos.isEmpty()) return
        val ids = nuevos.map(idDe).toSet()
        _items.value = (valor.filter { idDe(it) !in ids } + nuevos).sortedBy(idDe)
        val lote = FirebaseFirestore.getInstance().batch()
        nuevos.forEach { lote.set(coleccion.document(idDe(it).toString()), aMapa(it)) }
        lote.commit().addOnFailureListener { FirebaseCrashlytics.getInstance().recordException(it) }
    }

    fun crear(item: T, alTerminar: (Throwable?) -> Unit) {
        FirestoreOps.guardar(coleccion.document(idDe(item).toString()), aMapa(item), true, alTerminar)
    }

    fun eliminar(id: Int) = eliminarDonde { idDe(it) == id }

    fun eliminarDonde(condicion: (T) -> Boolean) {
        val objetivo = valor.filter(condicion)
        if (objetivo.isEmpty()) return
        val ids = objetivo.map(idDe).toSet()
        _items.value = valor.filter { idDe(it) !in ids }
        val lote = FirebaseFirestore.getInstance().batch()
        ids.forEach { lote.delete(coleccion.document(it.toString())) }
        lote.commit().addOnFailureListener { FirebaseCrashlytics.getInstance().recordException(it) }
    }
}
