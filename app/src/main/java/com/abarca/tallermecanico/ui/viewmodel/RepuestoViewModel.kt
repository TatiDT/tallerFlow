package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Repuesto
import com.abarca.tallermecanico.data.repository.RepuestoRepository

class RepuestoViewModel : ViewModel() {

    val repuestos: LiveData<List<Repuesto>> = RepuestoRepository.repuestos

    val cargando: LiveData<Boolean> = RepuestoRepository.cargando

    val error: LiveData<Throwable?> = RepuestoRepository.error

    fun insert(repuesto: Repuesto, alTerminar: (Throwable?) -> Unit) = RepuestoRepository.insert(repuesto, alTerminar)

    fun update(repuesto: Repuesto, alTerminar: (Throwable?) -> Unit) = RepuestoRepository.update(repuesto, alTerminar)

    fun delete(repuesto: Repuesto, alTerminar: (Throwable?) -> Unit) = RepuestoRepository.delete(repuesto, alTerminar)

    fun getById(id: Int): Repuesto? = RepuestoRepository.getById(id)

    fun isCodigoDuplicate(codigo: String, excludeId: Int = 0): Boolean =
        RepuestoRepository.isCodigoDuplicate(codigo, excludeId)
}
