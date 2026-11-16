package com.abarca.tallermecanico.data.entity

enum class TipoAgenda(val coleccion: String, val etiqueta: String) {
    CITA("citas", "Cita"),
    INTERVENCION("intervenciones", "Intervención")
}

enum class EstadoAgenda(val valor: String, val display: String) {
    AGENDADA("agendada", "Agendada"),
    REPROGRAMADA("reprogramada", "Reprogramada"),
    CANCELADA("cancelada", "Cancelada"),
    REALIZADA("realizada", "Realizada");

    companion object {
        fun de(valor: String?): EstadoAgenda = entries.find { it.valor == valor } ?: AGENDADA
    }
}

data class EventoAgenda(
    val id: Int = 0,
    val tipo: TipoAgenda,
    val clienteNombre: String,
    val clienteEmail: String,
    val vehiculoPatente: String,
    val vehiculoDescripcion: String,
    val detalle: String,
    val inicio: Long,
    val duracionMin: Int = 60,
    val estado: EstadoAgenda = EstadoAgenda.AGENDADA,
    val eventId: String = "",
    val syncError: String = "",
    val mecanicoNombre: String = "",
    val mecanicoEmail: String = "",
    val ordenTrabajoId: Int = 0,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    val fin: Long get() = inicio + duracionMin * 60_000L

    val titulo: String
        get() = when (tipo) {
            TipoAgenda.CITA -> "Cita TallerFlow – $detalle ($vehiculoPatente)"
            TipoAgenda.INTERVENCION -> "Intervención TallerFlow – $detalle ($vehiculoPatente)"
        }

    val activa: Boolean get() = estado == EstadoAgenda.AGENDADA || estado == EstadoAgenda.REPROGRAMADA

    val requiereSincronizar: Boolean
        get() = syncError.isNotBlank() ||
            (activa && eventId.isBlank()) ||
            (estado == EstadoAgenda.CANCELADA && eventId.isNotBlank())

    val clave: String get() = "${tipo.name}-$id"
}
