package com.abarca.tallermecanico.data.entity

enum class EstadoVehiculo(val display: String) {
    INGRESADO("Ingresado"),
    EN_REPARACION("En Reparación"),
    TERMINADO("Terminado"),
    LISTO("Listo"),
    ENTREGADO("Entregado")
}

data class Vehiculo(
    val id: Int = 0,
    val patente: String,
    val marca: String,
    val modelo: String,
    val año: Int,
    val color: String,
    val clienteId: Int,
    val estado: EstadoVehiculo = EstadoVehiculo.INGRESADO,
    val kilometraje: Int = 0,
    val mecanicoId: Int? = null,
    val fechaIngreso: String = "",
    val observaciones: String = "",
    val fechaLimite: String = ""
)

data class VehiculoConCliente(
    val vehiculo: Vehiculo,
    val cliente: Cliente,
    val mecanico: Usuario? = null,
    val presupuesto: Presupuesto? = null,
    val fallasPorCotizar: Int = 0,
    val adicionalesPendientes: Int = 0
)
