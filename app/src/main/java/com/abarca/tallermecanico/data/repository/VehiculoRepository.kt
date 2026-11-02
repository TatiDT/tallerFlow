package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.NotificadorCorreo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object VehiculoRepository {

    private val ESTADOS_ACTIVOS = setOf(EstadoVehiculo.INGRESADO, EstadoVehiculo.EN_REPARACION, EstadoVehiculo.TERMINADO)
    const val MAX_VEHICULOS_ACTIVOS_POR_MECANICO = 2

    private val coleccion get() = FirebaseFirestore.getInstance().collection("vehiculos")

    private val _vehiculos = MutableLiveData<List<Vehiculo>>(emptyList())
    val vehiculos: LiveData<List<Vehiculo>> = _vehiculos

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
            _vehiculos.value = snap.documents.mapNotNull { it.aVehiculo() }.sortedBy { it.id }
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

    fun insert(vehiculo: Vehiculo, alTerminar: (Throwable?) -> Unit) {
        val id = (_vehiculos.value.orEmpty().maxOfOrNull { it.id } ?: 0) + 1
        val nuevo = vehiculo.copy(id = id)
        FirestoreOps.guardar(coleccion.document(id.toString()), nuevo.aMapa(), true) { error ->
            if (error == null) NotificadorCorreo.vehiculoIngresado(nuevo)
            alTerminar(error)
        }
    }

    fun update(vehiculo: Vehiculo, alTerminar: (Throwable?) -> Unit) {
        val estadoPrevio = getById(vehiculo.id)?.estado
        FirestoreOps.guardar(coleccion.document(vehiculo.id.toString()), vehiculo.aMapa(), false) { error ->
            if (error == null && estadoPrevio != null && estadoPrevio != vehiculo.estado) {
                NotificadorCorreo.estadoCambiado(vehiculo)
            }
            alTerminar(error)
        }
    }

    fun delete(vehiculo: Vehiculo, alTerminar: (Throwable?) -> Unit) {
        FirestoreOps.eliminar(coleccion.document(vehiculo.id.toString())) { error ->
            if (error == null) {
                PresupuestoRepository.deleteByVehiculoId(vehiculo.id)
                TareaRepository.deleteByVehiculoId(vehiculo.id)
                CostoRepository.deleteByVehiculoId(vehiculo.id)
                FallaRepository.deleteByVehiculoId(vehiculo.id)
                DiagnosticoRepository.deleteByVehiculoId(vehiculo.id)
            }
            alTerminar(error)
        }
    }

    fun getById(id: Int): Vehiculo? = _vehiculos.value?.find { it.id == id }

    fun getActivosPorMecanico(mecanicoId: Int): List<Vehiculo> =
        _vehiculos.value.orEmpty().filter { it.mecanicoId == mecanicoId && it.estado in ESTADOS_ACTIVOS }

    fun isPatenteDuplicate(patente: String, excludeId: Int = 0): Boolean =
        _vehiculos.value.orEmpty().any {
            it.patente == patente && it.id != excludeId && it.estado != EstadoVehiculo.ENTREGADO
        }

    fun tieneVehiculosSinEntregar(mecanicoId: Int): Boolean =
        _vehiculos.value.orEmpty().any { it.mecanicoId == mecanicoId && it.estado != EstadoVehiculo.ENTREGADO }

    fun tieneVehiculos(mecanicoId: Int): Boolean =
        _vehiculos.value.orEmpty().any { it.mecanicoId == mecanicoId }

    fun contarActivosPorMecanico(mecanicoId: Int, excludeVehiculoId: Int = 0): Int =
        _vehiculos.value.orEmpty().count {
            it.mecanicoId == mecanicoId && it.id != excludeVehiculoId && it.estado in ESTADOS_ACTIVOS
        }

    fun superaLimiteMecanico(mecanicoId: Int, estado: EstadoVehiculo, excludeVehiculoId: Int = 0): Boolean =
        estado in ESTADOS_ACTIVOS &&
            contarActivosPorMecanico(mecanicoId, excludeVehiculoId) >= MAX_VEHICULOS_ACTIVOS_POR_MECANICO

    private fun Vehiculo.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "patente" to patente,
        "marca" to marca,
        "modelo" to modelo,
        "año" to año,
        "color" to color,
        "clienteId" to clienteId,
        "estado" to estado.name,
        "kilometraje" to kilometraje,
        "mecanicoId" to mecanicoId,
        "fechaIngreso" to fechaIngreso,
        "observaciones" to observaciones,
        "fechaLimite" to fechaLimite
    )

    private fun DocumentSnapshot.aVehiculo(): Vehiculo? {
        val vehiculoId = id.toIntOrNull() ?: return null
        return Vehiculo(
            id = vehiculoId,
            patente = getString("patente") ?: "",
            marca = getString("marca") ?: "",
            modelo = getString("modelo") ?: "",
            año = getLong("año")?.toInt() ?: 0,
            color = getString("color") ?: "",
            clienteId = getLong("clienteId")?.toInt() ?: 0,
            estado = runCatching { EstadoVehiculo.valueOf(getString("estado") ?: "") }
                .getOrDefault(EstadoVehiculo.INGRESADO),
            kilometraje = getLong("kilometraje")?.toInt() ?: 0,
            mecanicoId = getLong("mecanicoId")?.toInt(),
            fechaIngreso = getString("fechaIngreso") ?: "",
            observaciones = getString("observaciones") ?: "",
            fechaLimite = getString("fechaLimite") ?: ""
        )
    }
}
