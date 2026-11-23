package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.TiendaOnline
import com.abarca.tallermecanico.data.repository.TiendaOnlineRepository

class TiendaOnlineViewModel : ViewModel() {

    val tiendas: LiveData<List<TiendaOnline>> = TiendaOnlineRepository.tiendas

    fun getById(id: Int): TiendaOnline? = TiendaOnlineRepository.getById(id)

    fun siguienteId(): Int = TiendaOnlineRepository.siguienteId()

    fun siguienteOrden(): Int = TiendaOnlineRepository.siguienteOrden()

    fun guardar(tienda: TiendaOnline, alTerminar: (Throwable?) -> Unit) =
        TiendaOnlineRepository.guardar(tienda, alTerminar)

    fun eliminar(tienda: TiendaOnline, alTerminar: (Throwable?) -> Unit) =
        TiendaOnlineRepository.eliminar(tienda, alTerminar)
}
