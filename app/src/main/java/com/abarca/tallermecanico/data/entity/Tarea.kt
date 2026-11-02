package com.abarca.tallermecanico.data.entity

data class Tarea(
    val id: Int = 0,
    val vehiculoId: Int,
    val descripcion: String,
    val completada: Boolean = false
)
