package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.Tarea
import com.abarca.tallermecanico.util.Texto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

object TareaRepository {

    private val coleccion = ColeccionFirestore(
        nombre = "tareas",
        idDe = Tarea::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aTarea() }
    )

    val tareas: LiveData<List<Tarea>> = coleccion.items

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    /** Inserta la tarea si no existe otra igual para el vehículo. Devuelve false si está repetida. */
    fun insert(tarea: Tarea): Boolean {
        if (existe(tarea.vehiculoId, tarea.descripcion)) return false
        coleccion.guardar(tarea.copy(id = coleccion.siguienteId()))
        return true
    }

    fun insertChecklist(tareas: List<Tarea>) {
        coleccion.guardarVarios(tareas.map { it.copy(id = coleccion.siguienteId()) })
    }

    fun insertSinValidar(tarea: Tarea): Int {
        val id = coleccion.siguienteId()
        coleccion.guardar(tarea.copy(id = id))
        return id
    }

    fun delete(tareaId: Int) = coleccion.eliminar(tareaId)

    fun existe(vehiculoId: Int, descripcion: String): Boolean {
        val buscada = Texto.normalizar(descripcion)
        return getByVehiculoId(vehiculoId).any { Texto.normalizar(it.descripcion) == buscada }
    }

    fun agregarPendiente(vehiculoId: Int, descripcion: String) {
        if (insert(Tarea(vehiculoId = vehiculoId, descripcion = descripcion))) return
        val buscada = Texto.normalizar(descripcion)
        coleccion.guardarVarios(
            coleccion.valor
                .filter { it.vehiculoId == vehiculoId && Texto.normalizar(it.descripcion) == buscada }
                .map { it.copy(completada = false) }
        )
    }

    fun update(tarea: Tarea) = coleccion.guardar(tarea)

    fun getByVehiculoId(vehiculoId: Int): List<Tarea> =
        coleccion.valor.filter { it.vehiculoId == vehiculoId }

    fun progreso(vehiculoId: Int): Int {
        val propias = getByVehiculoId(vehiculoId)
        if (propias.isEmpty()) return 0
        return (propias.count { it.completada } * 100) / propias.size
    }

    fun deleteByVehiculoId(vehiculoId: Int) = coleccion.eliminarDonde { it.vehiculoId == vehiculoId }

    private fun Tarea.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "vehiculoId" to vehiculoId,
        "descripcion" to descripcion,
        "completada" to completada
    )

    private fun DocumentSnapshot.aTarea(): Tarea? {
        val tareaId = id.toIntOrNull() ?: return null
        return Tarea(
            id = tareaId,
            vehiculoId = getLong("vehiculoId")?.toInt() ?: return null,
            descripcion = getString("descripcion") ?: "",
            completada = getBoolean("completada") ?: false
        )
    }
}
