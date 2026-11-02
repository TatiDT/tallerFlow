package com.abarca.tallermecanico.data.entity

data class Falla(
    val id: Int = 0,
    val vehiculoId: Int,
    val x: Float,
    val y: Float,
    val descripcion: String,
    val fecha: String
)
