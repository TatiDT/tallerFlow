package com.abarca.tallermecanico.util

object Rut {
    fun normalizar(rut: String): String = rut.uppercase().filter { it.isDigit() || it == 'K' }

    fun esValido(rut: String): Boolean {
        val limpio = normalizar(rut)
        if (limpio.length < 2) return false
        val cuerpo = limpio.dropLast(1)
        if (!cuerpo.all { it.isDigit() }) return false
        return digitoVerificador(cuerpo) == limpio.last()
    }

    fun formatear(rut: String): String {
        val limpio = normalizar(rut)
        val cuerpo = limpio.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
        return "$cuerpo-${limpio.last()}"
    }

    private fun digitoVerificador(cuerpo: String): Char {
        var suma = 0
        var factor = 2
        for (c in cuerpo.reversed()) {
            suma += (c - '0') * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        return when (val dv = 11 - suma % 11) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + dv
        }
    }
}
