package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.data.repository.VehiculoRepository

class VehiculoViewModel : ViewModel() {

    val vehiculos: LiveData<List<Vehiculo>> = VehiculoRepository.vehiculos

    val cargando: LiveData<Boolean> = VehiculoRepository.cargando

    val error: LiveData<Throwable?> = VehiculoRepository.error

    fun insert(vehiculo: Vehiculo, alTerminar: (Throwable?) -> Unit) = VehiculoRepository.insert(vehiculo, alTerminar)

    fun update(vehiculo: Vehiculo, alTerminar: (Throwable?) -> Unit) = VehiculoRepository.update(vehiculo, alTerminar)

    fun delete(vehiculo: Vehiculo, alTerminar: (Throwable?) -> Unit) = VehiculoRepository.delete(vehiculo, alTerminar)

    fun getById(id: Int): Vehiculo? = VehiculoRepository.getById(id)

    fun getActivosPorMecanico(mecanicoId: Int): List<Vehiculo> =
        VehiculoRepository.getActivosPorMecanico(mecanicoId)

    fun isPatenteDuplicate(patente: String, excludeId: Int = 0): Boolean =
        VehiculoRepository.isPatenteDuplicate(patente, excludeId)

    fun tieneVehiculosSinEntregar(mecanicoId: Int): Boolean =
        VehiculoRepository.tieneVehiculosSinEntregar(mecanicoId)

    fun tieneVehiculos(mecanicoId: Int): Boolean = VehiculoRepository.tieneVehiculos(mecanicoId)

    fun superaLimiteMecanico(mecanicoId: Int, estado: EstadoVehiculo, excludeVehiculoId: Int = 0): Boolean =
        VehiculoRepository.superaLimiteMecanico(mecanicoId, estado, excludeVehiculoId)
}
