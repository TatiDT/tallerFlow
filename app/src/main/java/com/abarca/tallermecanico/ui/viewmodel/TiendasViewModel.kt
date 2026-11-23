package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abarca.tallermecanico.data.repository.ErrorTiendas
import com.abarca.tallermecanico.data.repository.TiendaCercana
import com.abarca.tallermecanico.data.repository.TiendaCercanaRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed class EstadoTiendas {
    object Cargando : EstadoTiendas()
    data class Listo(val tiendas: List<TiendaCercana>) : EstadoTiendas()
    data class Error(val tipo: ErrorTiendas.Tipo) : EstadoTiendas()
}

class TiendasViewModel : ViewModel() {

    private val _estado = MutableLiveData<EstadoTiendas>()
    val estado: LiveData<EstadoTiendas> = _estado

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = EstadoTiendas.Cargando
        viewModelScope.launch {
            _estado.value = try {
                EstadoTiendas.Listo(TiendaCercanaRepository.buscar())
            } catch (e: ErrorTiendas) {
                EstadoTiendas.Error(e.tipo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                EstadoTiendas.Error(ErrorTiendas.Tipo.SERVICIO_CAIDO)
            }
        }
    }
}
