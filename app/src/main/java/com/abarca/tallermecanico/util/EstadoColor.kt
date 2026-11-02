package com.abarca.tallermecanico.util

import androidx.annotation.ColorRes
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EstadoVehiculo

@ColorRes
fun EstadoVehiculo.colorRes(): Int = when (this) {
    EstadoVehiculo.INGRESADO -> R.color.estado_ingresado
    EstadoVehiculo.EN_REPARACION -> R.color.estado_en_reparacion
    EstadoVehiculo.TERMINADO -> R.color.estado_terminado
    EstadoVehiculo.LISTO -> R.color.estado_listo
    EstadoVehiculo.ENTREGADO -> R.color.estado_entregado
}
