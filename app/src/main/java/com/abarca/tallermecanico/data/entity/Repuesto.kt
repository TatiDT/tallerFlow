package com.abarca.tallermecanico.data.entity

data class Repuesto(
    val id: Int = 0,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val precio: Int,
    val stock: Int
)
