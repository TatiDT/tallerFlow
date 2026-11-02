package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Tarea
import com.abarca.tallermecanico.data.repository.TareaRepository

class TareaViewModel : ViewModel() {

    val tareas: LiveData<List<Tarea>> = TareaRepository.tareas

    fun insert(tarea: Tarea): Boolean = TareaRepository.insert(tarea)

    fun agregarPendiente(vehiculoId: Int, descripcion: String) =
        TareaRepository.agregarPendiente(vehiculoId, descripcion)

    fun existe(vehiculoId: Int, descripcion: String): Boolean = TareaRepository.existe(vehiculoId, descripcion)

    fun update(tarea: Tarea) = TareaRepository.update(tarea)

    fun getByVehiculoId(vehiculoId: Int): List<Tarea> = TareaRepository.getByVehiculoId(vehiculoId)

    fun progreso(vehiculoId: Int): Int = TareaRepository.progreso(vehiculoId)
}
