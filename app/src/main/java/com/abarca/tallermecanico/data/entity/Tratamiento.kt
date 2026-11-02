package com.abarca.tallermecanico.data.entity

/** Tratamiento básico del catálogo del taller. El precio es neto (sin IVA). */
data class Tratamiento(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val fallaId: Int? = null
)
