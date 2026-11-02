package com.abarca.tallermecanico.data.entity

data class Diagnostico(
    val id: Int = 0,
    val vehiculoId: Int,
    val descripcion: String,
    val fecha: String
)
