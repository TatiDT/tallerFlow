package com.abarca.tallermecanico.util

import java.text.NumberFormat
import java.util.Locale

object Formato {
    fun pesos(monto: Double): String = "$" + NumberFormat.getNumberInstance(Locale("es", "CL")).format(monto)
}
