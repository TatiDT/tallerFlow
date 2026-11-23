package com.abarca.tallermecanico.util

import com.abarca.tallermecanico.data.entity.FormatoBusqueda
import com.abarca.tallermecanico.data.entity.TiendaOnline
import java.net.URLEncoder

// Arma las URL de búsqueda de las tiendas en línea. No depende de Android para poder probarse.
object ConsultaTienda {

    // Une pieza + marca + modelo + año, omitiendo lo que venga vacío.
    fun texto(pieza: String, datosVehiculo: List<String>): String =
        (listOf(pieza) + datosVehiculo)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")

    // Aplica el formato de la tienda y codifica el texto para usarlo dentro de una URL.
    fun codificar(texto: String, formato: FormatoBusqueda): String = when (formato) {
        FormatoBusqueda.ESPACIOS -> URLEncoder.encode(texto, "UTF-8")
        FormatoBusqueda.GUIONES -> URLEncoder.encode(texto.lowercase().replace(" ", "-"), "UTF-8")
    }

    fun url(urlBusqueda: String, formato: FormatoBusqueda, texto: String): String =
        urlBusqueda.replace(TiendaOnline.MARCADOR, codificar(texto, formato))

    fun url(tienda: TiendaOnline, pieza: String, datosVehiculo: List<String>): String =
        url(tienda.urlBusqueda, tienda.formato, texto(pieza, datosVehiculo))

    // Misma regla que aplican las reglas de Firestore.
    fun urlValida(urlBusqueda: String): Boolean =
        urlBusqueda.startsWith("https://") && urlBusqueda.contains(TiendaOnline.MARCADOR)
}
