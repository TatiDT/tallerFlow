package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.abarca.tallermecanico.data.entity.EstadoAgenda
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.data.entity.TipoAgenda
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

object AgendaRepository {

    private val citas = crearColeccion(TipoAgenda.CITA)
    private val intervenciones = crearColeccion(TipoAgenda.INTERVENCION)

    val eventos: LiveData<List<EventoAgenda>> = MediatorLiveData<List<EventoAgenda>>().apply {
        addSource(citas.items) { value = todos() }
        addSource(intervenciones.items) { value = todos() }
    }
    val cargando: LiveData<Boolean> = citas.cargando
    val error: LiveData<Throwable?> = citas.error

    fun todos(): List<EventoAgenda> = (citas.valor + intervenciones.valor).sortedBy { it.inicio }

    fun iniciar() {
        citas.iniciar()
        intervenciones.iniciar()
    }

    fun reiniciar() {
        citas.reiniciar()
        intervenciones.reiniciar()
    }

    fun publicarError(causa: Throwable) {
        citas.publicarError(causa)
        intervenciones.publicarError(causa)
    }

    fun siguienteId(tipo: TipoAgenda): Int = coleccion(tipo).siguienteId()

    fun guardar(evento: EventoAgenda) = coleccion(evento.tipo).guardar(evento)

    fun getById(tipo: TipoAgenda, id: Int): EventoAgenda? = coleccion(tipo).valor.find { it.id == id }

    private fun coleccion(tipo: TipoAgenda) = if (tipo == TipoAgenda.CITA) citas else intervenciones

    private fun crearColeccion(tipo: TipoAgenda) = ColeccionFirestore(
        nombre = tipo.coleccion,
        idDe = EventoAgenda::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aEvento(tipo) }
    )

    private fun EventoAgenda.aMapa(): Map<String, Any?> {
        val comun = mapOf(
            "id" to id,
            "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
            "clienteNombre" to clienteNombre,
            "clienteEmail" to clienteEmail,
            "vehiculoPatente" to vehiculoPatente,
            "vehiculoDescripcion" to vehiculoDescripcion,
            "inicio" to Timestamp(Date(inicio)),
            "duracionMin" to duracionMin,
            "estado" to estado.valor,
            "eventId" to eventId,
            "syncError" to syncError,
            "creadoEn" to Timestamp(Date(creadoEn)),
            "actualizadoEn" to Timestamp(Date(actualizadoEn))
        )
        return if (tipo == TipoAgenda.CITA) {
            comun + mapOf("servicio" to detalle)
        } else {
            comun + mapOf(
                "ordenTrabajoId" to ordenTrabajoId,
                "descripcion" to detalle,
                "mecanicoNombre" to mecanicoNombre,
                "mecanicoEmail" to mecanicoEmail
            )
        }
    }

    private fun DocumentSnapshot.aEvento(tipo: TipoAgenda): EventoAgenda? {
        val eventoId = id.toIntOrNull() ?: return null
        val inicio = getTimestamp("inicio")?.toDate()?.time ?: return null
        return EventoAgenda(
            id = eventoId,
            tipo = tipo,
            clienteNombre = getString("clienteNombre").orEmpty(),
            clienteEmail = getString("clienteEmail").orEmpty(),
            vehiculoPatente = getString("vehiculoPatente").orEmpty(),
            vehiculoDescripcion = getString("vehiculoDescripcion").orEmpty(),
            detalle = getString(if (tipo == TipoAgenda.CITA) "servicio" else "descripcion").orEmpty(),
            inicio = inicio,
            duracionMin = getLong("duracionMin")?.toInt() ?: 60,
            estado = EstadoAgenda.de(getString("estado")),
            eventId = getString("eventId").orEmpty(),
            syncError = getString("syncError").orEmpty(),
            mecanicoNombre = getString("mecanicoNombre").orEmpty(),
            mecanicoEmail = getString("mecanicoEmail").orEmpty(),
            ordenTrabajoId = getLong("ordenTrabajoId")?.toInt() ?: 0,
            creadoEn = getTimestamp("creadoEn")?.toDate()?.time ?: inicio,
            actualizadoEn = getTimestamp("actualizadoEn")?.toDate()?.time ?: inicio
        )
    }
}
