package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.Diagnostico
import com.abarca.tallermecanico.util.Texto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

object DiagnosticoRepository {

    private val coleccion = ColeccionFirestore(
        nombre = "diagnosticos",
        idDe = Diagnostico::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aDiagnostico() }
    )

    val diagnosticos: LiveData<List<Diagnostico>> = coleccion.items

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    /** Devuelve false si el hallazgo ya está registrado para el vehículo. */
    fun insert(diagnostico: Diagnostico): Boolean {
        val buscado = Texto.normalizar(diagnostico.descripcion)
        if (getByVehiculoId(diagnostico.vehiculoId).any { Texto.normalizar(it.descripcion) == buscado }) return false
        coleccion.guardar(diagnostico.copy(id = coleccion.siguienteId()))
        return true
    }

    fun getByVehiculoId(vehiculoId: Int): List<Diagnostico> =
        coleccion.valor.filter { it.vehiculoId == vehiculoId }

    fun deleteByVehiculoId(vehiculoId: Int) = coleccion.eliminarDonde { it.vehiculoId == vehiculoId }

    private fun Diagnostico.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "vehiculoId" to vehiculoId,
        "descripcion" to descripcion,
        "fecha" to fecha
    )

    private fun DocumentSnapshot.aDiagnostico(): Diagnostico? {
        val diagnosticoId = id.toIntOrNull() ?: return null
        return Diagnostico(
            id = diagnosticoId,
            vehiculoId = getLong("vehiculoId")?.toInt() ?: return null,
            descripcion = getString("descripcion") ?: "",
            fecha = getString("fecha") ?: ""
        )
    }
}
