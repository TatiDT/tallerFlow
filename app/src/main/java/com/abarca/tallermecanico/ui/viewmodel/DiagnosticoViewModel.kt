package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Diagnostico
import com.abarca.tallermecanico.data.repository.DiagnosticoRepository

class DiagnosticoViewModel : ViewModel() {

    val diagnosticos: LiveData<List<Diagnostico>> = DiagnosticoRepository.diagnosticos

    fun insert(diagnostico: Diagnostico): Boolean = DiagnosticoRepository.insert(diagnostico)

    fun getByVehiculoId(vehiculoId: Int): List<Diagnostico> = DiagnosticoRepository.getByVehiculoId(vehiculoId)
}
