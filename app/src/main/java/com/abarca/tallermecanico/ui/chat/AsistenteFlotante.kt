package com.abarca.tallermecanico.ui.chat

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.databinding.ViewAsistenteFlotanteBinding

class AsistenteFlotante @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewAsistenteFlotanteBinding.inflate(LayoutInflater.from(context), this)
    private val adapter = ChatAdapter()

    init {
        binding.recyclerView.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter
        binding.fabAsistente.setOnClickListener { alternar() }
        binding.btnCerrarPanel.setOnClickListener { alternar() }

        ViewCompat.setOnApplyWindowInsetsListener(this) { vista, insets ->
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            vista.setPadding(vista.paddingLeft, vista.paddingTop, vista.paddingRight, (teclado - barras).coerceAtLeast(0))
            ajustarAltura()
            insets
        }
    }

    fun vincular(actividad: AppCompatActivity) {
        val viewModel = ViewModelProvider(actividad)[ChatViewModel::class.java]

        viewModel.mensajes.observe(actividad) { mensajes ->
            adapter.submitList(mensajes) {
                if (mensajes.isNotEmpty()) binding.recyclerView.scrollToPosition(mensajes.size - 1)
            }
            binding.tvBienvenida.visibility = if (mensajes.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.enviando.observe(actividad) { enviando ->
            binding.progress.visibility = if (enviando) View.VISIBLE else View.GONE
            binding.btnEnviar.isEnabled = !enviando
        }
        viewModel.error.observe(actividad) { hayError ->
            binding.tvError.visibility = if (hayError) View.VISIBLE else View.GONE
        }

        binding.btnEnviar.setOnClickListener {
            val texto = binding.etMensaje.text?.toString().orEmpty()
            if (texto.isBlank()) return@setOnClickListener
            binding.etMensaje.setText("")
            viewModel.enviar(texto)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ajustarAltura()
    }

    private fun alternar() {
        val abrir = binding.panel.visibility != View.VISIBLE
        binding.panel.visibility = if (abrir) View.VISIBLE else View.GONE
        binding.fabAsistente.text = context.getString(if (abrir) R.string.btn_cerrar_asistente else R.string.menu_asistente)
        if (abrir) {
            ajustarAltura()
        } else {
            val teclado = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            teclado.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun ajustarAltura() {
        val densidad = resources.displayMetrics.density
        val disponible = height - paddingBottom - (110 * densidad).toInt()
        val deseada = (height * 0.6f).toInt()
        val altura = minOf(deseada, disponible).coerceAtLeast((180 * densidad).toInt())
        val parametros = binding.panel.layoutParams
        if (parametros.height != altura) {
            parametros.height = altura
            binding.panel.layoutParams = parametros
        }
    }
}
