package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Falla
import com.abarca.tallermecanico.data.repository.FallaRepository

class FallaViewModel : ViewModel() {

    val fallas: LiveData<List<Falla>> = FallaRepository.fallas

    fun insert(falla: Falla) = FallaRepository.insert(falla)

    fun delete(falla: Falla): Boolean = FallaRepository.delete(falla)

    fun getByVehiculoId(vehiculoId: Int): List<Falla> = FallaRepository.getByVehiculoId(vehiculoId)
}
