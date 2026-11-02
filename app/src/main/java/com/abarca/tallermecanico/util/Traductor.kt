package com.abarca.tallermecanico.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.LruCache
import android.widget.TextView
import com.abarca.tallermecanico.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object Traductor {

    private const val PREFS = "ajustes"
    private const val CLAVE_IDIOMA = "idioma"

    private lateinit var prefs: SharedPreferences
    private val traductores = HashMap<String, Translator>()
    private val preparados = HashSet<String>()
    private val cache = LruCache<String, String>(500)

    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var idioma: Idioma
        get() = Idioma.deCodigo(prefs.getString(CLAVE_IDIOMA, null))
        set(valor) {
            prefs.edit().putString(CLAVE_IDIOMA, valor.codigo).apply()
        }

    val activo: Boolean get() = idioma != Idioma.ESPANOL

    fun traducir(texto: String, alTerminar: (String) -> Unit) {
        val destino = idioma.codigo
        if (!activo || texto.isBlank()) {
            alTerminar(texto)
            return
        }
        val clave = "$destino|$texto"
        cache.get(clave)?.let {
            alTerminar(it)
            return
        }
        val traductor = traductores.getOrPut(destino) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.SPANISH)
                    .setTargetLanguage(destino)
                    .build()
            )
        }
        val traducirTexto = {
            traductor.translate(texto).addOnSuccessListener { resultado ->
                cache.put(clave, resultado)
                alTerminar(resultado)
            }.addOnFailureListener { Log.e("Traductor", "Fallo traduccion", it) }
        }
        if (preparados.contains(destino)) {
            traducirTexto()
        } else {
            traductor.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnSuccessListener {
                    preparados.add(destino)
                    traducirTexto()
                }
                .addOnFailureListener { Log.e("Traductor", "Fallo descarga modelo", it) }
        }
    }
}

fun TextView.mostrarTexto(original: String, formato: (String) -> String = { it }) {
    text = formato(original)
    setTag(R.id.tag_traduccion, original)
    if (!Traductor.activo) return
    Traductor.traducir(original) { traducido ->
        if (getTag(R.id.tag_traduccion) == original) text = formato(traducido)
    }
}

fun Activity.mostrarSelectorIdioma() {
    val opciones = Idioma.entries
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.title_idioma)
        .setSingleChoiceItems(
            opciones.map { it.nombre }.toTypedArray(),
            opciones.indexOf(Traductor.idioma)
        ) { dialogo, posicion ->
            Traductor.idioma = opciones[posicion]
            dialogo.dismiss()
            recreate()
        }
        .setNegativeButton(R.string.btn_cancelar, null)
        .show()
}
