package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.data.repository.ClienteRepository

class ClienteViewModel : ViewModel() {

    val clientes: LiveData<List<Cliente>> = ClienteRepository.clientes

    val cargando: LiveData<Boolean> = ClienteRepository.cargando

    val error: LiveData<Throwable?> = ClienteRepository.error

    fun insert(cliente: Cliente, alTerminar: (Throwable?) -> Unit) = ClienteRepository.insert(cliente, alTerminar)

    fun update(cliente: Cliente, alTerminar: (Throwable?) -> Unit) = ClienteRepository.update(cliente, alTerminar)

    fun delete(cliente: Cliente, alTerminar: (Throwable?) -> Unit) = ClienteRepository.delete(cliente, alTerminar)

    fun getById(id: Int): Cliente? = ClienteRepository.getById(id)

    fun isRutDuplicate(rut: String, excludeId: Int = 0): Boolean =
        ClienteRepository.isRutDuplicate(rut, excludeId)
}
