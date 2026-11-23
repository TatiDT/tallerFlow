package com.abarca.tallermecanico

import com.abarca.tallermecanico.data.entity.FormatoBusqueda
import com.abarca.tallermecanico.data.entity.TiendaOnline
import com.abarca.tallermecanico.util.ConsultaTienda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsultaTiendaTest {

    private val google = TiendaOnline.PREDETERMINADAS[0]
    private val mercadoLibre = TiendaOnline.PREDETERMINADAS[1]

    @Test
    fun textoOmiteDatosVacios() {
        assertEquals("Pastillas de freno Toyota 2018", ConsultaTienda.texto("Pastillas de freno", listOf("Toyota", "", "2018")))
        assertEquals("Filtro", ConsultaTienda.texto("  Filtro ", emptyList()))
    }

    @Test
    fun formatoEspaciosUsaMas() {
        val url = ConsultaTienda.url(google, "Pastillas de freno", listOf("Toyota", "Yaris", "2018"))
        assertEquals("https://www.google.com/search?tbm=shop&q=Pastillas+de+freno+Toyota+Yaris+2018", url)
    }

    @Test
    fun formatoGuionesUsaMinusculasYGuiones() {
        val url = ConsultaTienda.url(mercadoLibre, "Pastillas de freno", listOf("Toyota", "Yaris", "2018"))
        assertEquals("https://listado.mercadolibre.cl/pastillas-de-freno-toyota-yaris-2018", url)
    }

    @Test
    fun codificaCaracteresEspeciales() {
        assertEquals("aceite+5w%2F30+%C3%B1", ConsultaTienda.codificar("aceite 5w/30 ñ", FormatoBusqueda.ESPACIOS))
    }

    @Test
    fun validaUrlDeTienda() {
        assertTrue(ConsultaTienda.urlValida("https://tienda.cl/buscar?q={consulta}"))
        assertFalse(ConsultaTienda.urlValida("http://tienda.cl/buscar?q={consulta}"))
        assertFalse(ConsultaTienda.urlValida("https://tienda.cl/buscar"))
    }
}
