package com.abarca.tallermecanico.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ChatViewModel : ViewModel() {

    private val modelo = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODELO,
        systemInstruction = content {
            text(
                "Eres el asistente de un taller mecánico. Respondes siempre en español, " +
                    "de forma breve y clara, sobre diagnóstico de fallas, mantenimiento, " +
                    "repuestos y procedimientos de reparación. Si la consulta no tiene " +
                    "relación con vehículos o el taller, indícalo amablemente."
            )
        }
    )
    private val chat = modelo.startChat()

    private var siguienteId = 0L

    private val _mensajes = MutableLiveData<List<ChatMensaje>>(emptyList())
    val mensajes: LiveData<List<ChatMensaje>> = _mensajes

    private val _enviando = MutableLiveData(false)
    val enviando: LiveData<Boolean> = _enviando

    private val _error = MutableLiveData(false)
    val error: LiveData<Boolean> = _error

    fun enviar(texto: String) {
        val consulta = texto.trim()
        if (consulta.isEmpty() || _enviando.value == true) return
        agregar(consulta, esUsuario = true)
        _enviando.value = true
        _error.value = false
        viewModelScope.launch {
            try {
                Log.d("Asistente", "Enviando consulta a $MODELO")
                val respuesta = withTimeout(TIEMPO_LIMITE_MS) { chat.sendMessage(consulta).text }
                Log.d("Asistente", "Respuesta recibida: ${respuesta?.length ?: 0} caracteres")
                if (respuesta.isNullOrBlank()) _error.value = true else agregar(respuesta.trim(), esUsuario = false)
            } catch (e: Exception) {
                Log.e("Asistente", "Fallo Gemini", e)
                _error.value = true
            } finally {
                _enviando.value = false
            }
        }
    }

    private fun agregar(texto: String, esUsuario: Boolean) {
        _mensajes.value = _mensajes.value.orEmpty() + ChatMensaje(siguienteId++, texto, esUsuario)
    }

    companion object {
        private const val MODELO = "gemini-3.5-flash-lite"
        private const val TIEMPO_LIMITE_MS = 45_000L
    }
}
