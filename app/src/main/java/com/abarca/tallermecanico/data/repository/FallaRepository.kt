package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.Falla
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

object FallaRepository {

    private val coleccion = ColeccionFirestore(
        nombre = "fallas",
        idDe = Falla::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aFalla() }
    )

    val fallas: LiveData<List<Falla>> = coleccion.items

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    fun insert(falla: Falla) {
        val nueva = falla.copy(id = coleccion.siguienteId())
        coleccion.guardar(nueva)
        val estado = VehiculoRepository.getById(falla.vehiculoId)?.estado
        if (estado != EstadoVehiculo.LISTO && estado != EstadoVehiculo.ENTREGADO) {
            PresupuestoRepository.agregarAdicional(nueva.vehiculoId, nueva.descripcion, fallaId = nueva.id)
        }
    }

    fun delete(falla: Falla): Boolean {
        if (!PresupuestoRepository.quitarFalla(falla.vehiculoId, falla.id)) return false
        coleccion.eliminar(falla.id)
        return true
    }

    fun getByVehiculoId(vehiculoId: Int): List<Falla> =
        coleccion.valor.filter { it.vehiculoId == vehiculoId }

    fun deleteByVehiculoId(vehiculoId: Int) = coleccion.eliminarDonde { it.vehiculoId == vehiculoId }

    private fun Falla.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "vehiculoId" to vehiculoId,
        "x" to x.toDouble(),
        "y" to y.toDouble(),
        "descripcion" to descripcion,
        "fecha" to fecha
    )

    private fun DocumentSnapshot.aFalla(): Falla? {
        val fallaId = id.toIntOrNull() ?: return null
        return Falla(
            id = fallaId,
            vehiculoId = getLong("vehiculoId")?.toInt() ?: return null,
            x = getDouble("x")?.toFloat() ?: 0f,
            y = getDouble("y")?.toFloat() ?: 0f,
            descripcion = getString("descripcion") ?: "",
            fecha = getString("fecha") ?: ""
        )
    }
}
