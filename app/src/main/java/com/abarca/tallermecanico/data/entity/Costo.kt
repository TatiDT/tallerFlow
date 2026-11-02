package com.abarca.tallermecanico.data.entity

data class Costo(
    val id: Int = 0,
    val vehiculoId: Int,
    val descripcion: String,
    val monto: Double,
    val fecha: String
)
