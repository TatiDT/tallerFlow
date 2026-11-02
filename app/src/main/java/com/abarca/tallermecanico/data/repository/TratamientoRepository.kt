package com.abarca.tallermecanico.data.repository

import com.abarca.tallermecanico.data.entity.Tratamiento

object TratamientoRepository {

    /** Los tratamientos "Otro" ingresados a mano reciben ids desde aquí, fuera del rango del catálogo. */
    private var ultimoOtroId = 999

    fun nuevoIdOtro(): Int {
        val existente = PresupuestoRepository.presupuestos.value.orEmpty()
            .flatMap { it.tratamientos }.maxOfOrNull { it.id } ?: 0
        ultimoOtroId = maxOf(ultimoOtroId, existente) + 1
        return ultimoOtroId
    }

    fun esDelCatalogo(id: Int): Boolean = tratamientos.any { it.id == id }

    /** Catálogo fijo de tratamientos básicos, precios netos en CLP. */
    val tratamientos: List<Tratamiento> = listOf(
        Tratamiento(1, "Cambio de aceite y filtro", 50000.0),
        Tratamiento(2, "Cambio de filtro de aire", 15000.0),
        Tratamiento(3, "Alineación y balanceo", 35000.0),
        Tratamiento(4, "Cambio de pastillas de freno", 60000.0),
        Tratamiento(5, "Cambio de líquido de frenos", 30000.0),
        Tratamiento(6, "Cambio de bujías", 40000.0),
        Tratamiento(7, "Revisión y carga de batería", 20000.0),
        Tratamiento(8, "Scanner computarizado", 25000.0),
        Tratamiento(9, "Revisión sistema de refrigeración", 30000.0),
        Tratamiento(10, "Cambio de correa de distribución", 180000.0)
    )
}
