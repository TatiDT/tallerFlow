package com.abarca.tallermecanico.data.entity

import kotlin.math.roundToLong

enum class EstadoAdicional(val display: String) {
    PENDIENTE("Pendiente"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado")
}

data class Adicional(
    val id: Int,
    val descripcion: String,
    val tareaId: Int,
    val fallaId: Int? = null,
    val precio: Double? = null,
    val estado: EstadoAdicional = EstadoAdicional.PENDIENTE,
    val fecha: String
)

data class Presupuesto(
    val id: Int = 0,
    val vehiculoId: Int,
    /** Copia de los tratamientos elegidos, con el precio vigente al momento de presupuestar. */
    val tratamientos: List<Tratamiento>,
    val fechaCreacion: String,
    /** Aprobado por el cliente (lo registra Admin). Aprobado ya no se modifica. */
    val aprobado: Boolean = false,
    val fechaAprobacion: String = "",
    val adicionales: List<Adicional> = emptyList()
) {
    val adicionalesAprobados: List<Adicional> get() = adicionales.filter { it.estado == EstadoAdicional.APROBADO }
    val adicionalesPendientes: List<Adicional> get() = adicionales.filter { it.estado == EstadoAdicional.PENDIENTE }

    val neto: Double get() = tratamientos.sumOf { it.precio } + adicionalesAprobados.sumOf { it.precio ?: 0.0 }
    val iva: Double get() = calcularIva(neto)
    val total: Double get() = neto + iva

    companion object {
        const val TASA_IVA = 0.19

        fun calcularIva(neto: Double): Double = (neto * TASA_IVA).roundToLong().toDouble()
    }
}
