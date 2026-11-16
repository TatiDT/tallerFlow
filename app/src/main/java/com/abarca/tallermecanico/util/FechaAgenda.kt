package com.abarca.tallermecanico.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FechaAgenda {

    val zona: TimeZone = TimeZone.getTimeZone("America/Santiago")
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val local = Locale("es", "CL")

    fun fechaHora(millis: Long): String = formatear("EEE dd/MM/yyyy HH:mm", millis, zona)

    fun fecha(millis: Long): String = formatear("dd/MM/yyyy", millis, zona)

    fun hora(millis: Long): String = formatear("HH:mm", millis, zona)

    fun fechaDeSelector(fechaUtc: Long): String = formatear("dd/MM/yyyy", fechaUtc, utc)

    fun hora(hora: Int, minuto: Int): String = String.format(Locale.US, "%02d:%02d", hora, minuto)

    // MaterialDatePicker entrega medianoche UTC del día elegido; se reinterpreta en la zona del taller
    fun combinar(fechaUtc: Long, hora: Int, minuto: Int): Long {
        val dia = Calendar.getInstance(utc).apply { timeInMillis = fechaUtc }
        return Calendar.getInstance(zona).apply {
            clear()
            set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH), hora, minuto, 0)
        }.timeInMillis
    }

    fun aSelectorUtc(millis: Long): Long {
        val dia = Calendar.getInstance(zona).apply { timeInMillis = millis }
        return Calendar.getInstance(utc).apply {
            clear()
            set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun horaDe(millis: Long): Int = Calendar.getInstance(zona).apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    fun minutoDe(millis: Long): Int = Calendar.getInstance(zona).apply { timeInMillis = millis }.get(Calendar.MINUTE)

    private fun formatear(patron: String, millis: Long, zonaHoraria: TimeZone): String =
        SimpleDateFormat(patron, local).apply { timeZone = zonaHoraria }.format(Date(millis))
}
