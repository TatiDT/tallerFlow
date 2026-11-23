package com.abarca.tallermecanico.data.entity

// Cómo se escribe la consulta dentro de la URL de la tienda.
enum class FormatoBusqueda(val clave: String) {
    ESPACIOS("espacios"),
    GUIONES("guiones");

    companion object {
        fun deClave(clave: String?) = entries.firstOrNull { it.clave == clave } ?: ESPACIOS
    }
}

// Tienda en línea donde se puede buscar un repuesto. urlBusqueda lleva el marcador {consulta}.
data class TiendaOnline(
    val id: Int = 0,
    val nombre: String,
    val urlBusqueda: String,
    val formato: FormatoBusqueda,
    val orden: Int,
    val activa: Boolean = true
) {
    companion object {
        const val MARCADOR = "{consulta}"

        // Se usan cuando el taller aún no tiene tiendas guardadas en Firestore.
        val PREDETERMINADAS = listOf(
            TiendaOnline(
                id = 1,
                nombre = "Comparar precios (Google Shopping)",
                urlBusqueda = "https://www.google.com/search?tbm=shop&q=$MARCADOR",
                formato = FormatoBusqueda.ESPACIOS,
                orden = 1
            ),
            TiendaOnline(
                id = 2,
                nombre = "Mercado Libre",
                urlBusqueda = "https://listado.mercadolibre.cl/$MARCADOR",
                formato = FormatoBusqueda.GUIONES,
                orden = 2
            )
        )
    }
}
