package com.abarca.tallermecanico.util

import com.google.mlkit.nl.translate.TranslateLanguage

enum class Idioma(val codigo: String, val nombre: String) {
    ESPANOL(TranslateLanguage.SPANISH, "Español (original)"),
    INGLES(TranslateLanguage.ENGLISH, "English"),
    PORTUGUES(TranslateLanguage.PORTUGUESE, "Português"),
    FRANCES(TranslateLanguage.FRENCH, "Français"),
    ALEMAN(TranslateLanguage.GERMAN, "Deutsch"),
    ITALIANO(TranslateLanguage.ITALIAN, "Italiano");

    companion object {
        fun deCodigo(codigo: String?): Idioma = entries.firstOrNull { it.codigo == codigo } ?: ESPANOL
    }
}
