package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.Costo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

object CostoRepository {

    private val coleccion = ColeccionFirestore(
        nombre = "costos",
        idDe = Costo::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aCosto() }
    )

    val costos: LiveData<List<Costo>> = coleccion.items

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    fun insert(costo: Costo) = coleccion.guardar(costo.copy(id = coleccion.siguienteId()))

    fun update(costo: Costo) = coleccion.guardar(costo)

    fun delete(costo: Costo) = coleccion.eliminar(costo.id)

    fun getByVehiculoId(vehiculoId: Int): List<Costo> =
        coleccion.valor.filter { it.vehiculoId == vehiculoId }

    fun totalByVehiculoId(vehiculoId: Int): Double =
        getByVehiculoId(vehiculoId).sumOf { it.monto }

    fun deleteByVehiculoId(vehiculoId: Int) = coleccion.eliminarDonde { it.vehiculoId == vehiculoId }

    private fun Costo.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "vehiculoId" to vehiculoId,
        "descripcion" to descripcion,
        "monto" to monto,
        "fecha" to fecha
    )

    private fun DocumentSnapshot.aCosto(): Costo? {
        val costoId = id.toIntOrNull() ?: return null
        return Costo(
            id = costoId,
            vehiculoId = getLong("vehiculoId")?.toInt() ?: return null,
            descripcion = getString("descripcion") ?: "",
            monto = getDouble("monto") ?: 0.0,
            fecha = getString("fecha") ?: ""
        )
    }
}
