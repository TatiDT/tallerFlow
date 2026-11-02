package com.abarca.tallermecanico.data.entity

data class Cliente(
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val rut: String,
    val telefono: String,
    val email: String = ""
)
