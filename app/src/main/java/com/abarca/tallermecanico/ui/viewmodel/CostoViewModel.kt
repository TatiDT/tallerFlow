package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Costo
import com.abarca.tallermecanico.data.repository.CostoRepository

class CostoViewModel : ViewModel() {

    val costos: LiveData<List<Costo>> = CostoRepository.costos

    fun insert(costo: Costo) = CostoRepository.insert(costo)

    fun update(costo: Costo) = CostoRepository.update(costo)

    fun delete(costo: Costo) = CostoRepository.delete(costo)

    fun getByVehiculoId(vehiculoId: Int): List<Costo> = CostoRepository.getByVehiculoId(vehiculoId)

    fun totalByVehiculoId(vehiculoId: Int): Double = CostoRepository.totalByVehiculoId(vehiculoId)
}
