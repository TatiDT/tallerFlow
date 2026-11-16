package com.abarca.tallermecanico.util

import android.content.Context
import com.abarca.tallermecanico.data.entity.EstadoAgenda
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.data.repository.AgendaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SincronizadorAgenda {

    // Alcance propio: la sincronización debe terminar aunque el usuario salga de la pantalla
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sincronizar(ctx: Context, evento: EventoAgenda, alTerminar: (EventoAgenda) -> Unit = {}) {
        val app = ctx.applicationContext
        alcance.launch {
            val resultado = procesar(app, evento)
            withContext(Dispatchers.Main) {
                AgendaRepository.guardar(resultado)
                alTerminar(resultado)
            }
        }
    }

    fun sincronizarPendientes(ctx: Context, alTerminar: (exitosos: Int, fallidos: Int) -> Unit) {
        val app = ctx.applicationContext
        alcance.launch {
            val pendientes = withContext(Dispatchers.Main) {
                AgendaRepository.todos().filter { it.requiereSincronizar }
            }
            var exitosos = 0
            var fallidos = 0
            for (evento in pendientes) {
                val resultado = procesar(app, evento)
                if (resultado.syncError.isBlank()) exitosos++ else fallidos++
                withContext(Dispatchers.Main) { AgendaRepository.guardar(resultado) }
            }
            withContext(Dispatchers.Main) { alTerminar(exitosos, fallidos) }
        }
    }

    private suspend fun procesar(ctx: Context, e: EventoAgenda): EventoAgenda = try {
        when {
            e.estado == EstadoAgenda.CANCELADA -> {
                if (e.eventId.isNotBlank()) CalendarioGoogle.cancelar(ctx, e.eventId)
                e.copy(eventId = "", syncError = "")
            }
            e.estado == EstadoAgenda.REALIZADA -> e.copy(syncError = "")
            e.eventId.isBlank() -> e.copy(eventId = CalendarioGoogle.crear(ctx, e), syncError = "")
            else -> try {
                CalendarioGoogle.reprogramar(ctx, e)
                e.copy(syncError = "")
            } catch (x: CalendarioGoogle.ErrorCalendario) {
                // El evento fue borrado a mano en Calendar: se vuelve a crear
                if (x.codigo == 404 || x.codigo == 410) {
                    e.copy(eventId = CalendarioGoogle.crear(ctx, e), syncError = "")
                } else {
                    throw x
                }
            }
        }
    } catch (x: CancellationException) {
        throw x
    } catch (x: Exception) {
        e.copy(syncError = CalendarioGoogle.mensajeError(ctx, x))
    }
}
