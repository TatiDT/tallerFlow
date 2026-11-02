package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Falla
import com.abarca.tallermecanico.data.entity.Presupuesto
import com.abarca.tallermecanico.data.entity.Tratamiento
import com.abarca.tallermecanico.data.repository.FallaRepository
import com.abarca.tallermecanico.data.repository.PresupuestoRepository
import com.abarca.tallermecanico.data.repository.TratamientoRepository

class PresupuestoViewModel : ViewModel() {

    val presupuestos: LiveData<List<Presupuesto>> = PresupuestoRepository.presupuestos

    val tratamientos: List<Tratamiento> = TratamientoRepository.tratamientos

    fun getByVehiculoId(vehiculoId: Int): Presupuesto? =
        PresupuestoRepository.getByVehiculoId(vehiculoId)

    fun guardar(vehiculoId: Int, tratamientos: List<Tratamiento>) =
        PresupuestoRepository.guardar(vehiculoId, tratamientos)

    fun fallas(vehiculoId: Int): List<Falla> = FallaRepository.getByVehiculoId(vehiculoId)

    fun agregarAdicional(vehiculoId: Int, descripcion: String): Boolean =
        PresupuestoRepository.agregarAdicional(vehiculoId, descripcion)

    fun responderAdicional(vehiculoId: Int, adicionalId: Int, precio: Double?, aprobado: Boolean) =
        PresupuestoRepository.responderAdicional(vehiculoId, adicionalId, precio, aprobado)

    fun tareasBloqueadas(vehiculoId: Int): Set<Int> = PresupuestoRepository.tareasBloqueadas(vehiculoId)

    fun aprobar(vehiculoId: Int) = PresupuestoRepository.aprobar(vehiculoId)
}
