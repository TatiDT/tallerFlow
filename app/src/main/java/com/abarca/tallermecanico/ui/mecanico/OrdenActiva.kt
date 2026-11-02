package com.abarca.tallermecanico.ui.mecanico

import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.data.entity.Vehiculo

data class OrdenActiva(
    val vehiculo: Vehiculo,
    val cliente: Cliente,
    val progreso: Int
)
