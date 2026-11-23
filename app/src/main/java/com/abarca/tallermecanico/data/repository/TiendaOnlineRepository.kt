package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.abarca.tallermecanico.data.entity.FormatoBusqueda
import com.abarca.tallermecanico.data.entity.TiendaOnline
import com.abarca.tallermecanico.util.TallerActual
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// Tiendas en línea del taller (talleres/{tallerId}/tiendasOnline).
// Mientras la colección está vacía se trabaja con TiendaOnline.PREDETERMINADAS.
object TiendaOnlineRepository {

    private const val NOMBRE = "tiendasOnline"

    private val coleccion = ColeccionFirestore(
        nombre = NOMBRE,
        idDe = TiendaOnline::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aTienda() }
    )

    // Lista vigente ordenada: la guardada en Firestore o, si no hay nada, las predeterminadas.
    val tiendas: LiveData<List<TiendaOnline>> = MediatorLiveData<List<TiendaOnline>>().apply {
        addSource(coleccion.items) { value = vigentes(it) }
    }

    fun iniciar() = coleccion.iniciar()
    fun reiniciar() = coleccion.reiniciar()
    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    fun getById(id: Int): TiendaOnline? = vigentes(coleccion.valor).find { it.id == id }

    fun siguienteId(): Int = (vigentes(coleccion.valor).maxOfOrNull { it.id } ?: 0) + 1

    fun siguienteOrden(): Int = (vigentes(coleccion.valor).maxOfOrNull { it.orden } ?: 0) + 1

    // Crea o actualiza una tienda. Si todavía se usaban las predeterminadas, las guarda junto con el cambio
    // para que no desaparezcan cuando la colección deje de estar vacía.
    fun guardar(tienda: TiendaOnline, alTerminar: (Throwable?) -> Unit) {
        val nuevos = if (coleccion.valor.isEmpty()) {
            TiendaOnline.PREDETERMINADAS.filter { it.id != tienda.id } + tienda
        } else {
            listOf(tienda)
        }
        escribir(nuevos, emptyList(), alTerminar)
    }

    fun eliminar(tienda: TiendaOnline, alTerminar: (Throwable?) -> Unit) {
        if (coleccion.valor.isEmpty()) {
            val restantes = TiendaOnline.PREDETERMINADAS.filter { it.id != tienda.id }
            escribir(restantes, emptyList(), alTerminar)
        } else {
            escribir(emptyList(), listOf(tienda), alTerminar)
        }
    }

    private fun escribir(
        guardar: List<TiendaOnline>,
        borrar: List<TiendaOnline>,
        alTerminar: (Throwable?) -> Unit
    ) {
        val destino = TallerActual.coleccion(NOMBRE)
        val lote = FirebaseFirestore.getInstance().batch()
        guardar.forEach { lote.set(destino.document(it.id.toString()), it.aMapa()) }
        borrar.forEach { lote.delete(destino.document(it.id.toString())) }
        lote.commit().addOnCompleteListener {
            it.exception?.let { error -> FirebaseCrashlytics.getInstance().recordException(error) }
            alTerminar(it.exception)
        }
    }

    private fun vigentes(guardadas: List<TiendaOnline>): List<TiendaOnline> =
        guardadas.ifEmpty { TiendaOnline.PREDETERMINADAS }.sortedWith(compareBy({ it.orden }, { it.id }))

    private fun TiendaOnline.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "nombre" to nombre,
        "urlBusqueda" to urlBusqueda,
        "formato" to formato.clave,
        "orden" to orden,
        "activa" to activa
    )

    private fun DocumentSnapshot.aTienda(): TiendaOnline? {
        val tiendaId = id.toIntOrNull() ?: return null
        return TiendaOnline(
            id = tiendaId,
            nombre = getString("nombre").orEmpty(),
            urlBusqueda = getString("urlBusqueda").orEmpty(),
            formato = FormatoBusqueda.deClave(getString("formato")),
            orden = getLong("orden")?.toInt() ?: 0,
            activa = getBoolean("activa") ?: true
        )
    }
}
