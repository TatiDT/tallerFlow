package com.abarca.tallermecanico.util

object Texto {
    /** Para comparar descripciones ignorando mayúsculas y espacios repetidos. */
    fun normalizar(texto: String): String = texto.trim().lowercase().replace(Regex("\\s+"), " ")
}
