package com.abarca.tallermecanico.ui.repuesto

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.FormatoBusqueda
import com.abarca.tallermecanico.data.entity.TiendaOnline
import com.abarca.tallermecanico.databinding.FragmentTiendaOnlineFormBinding
import com.abarca.tallermecanico.ui.viewmodel.TiendaOnlineViewModel
import com.abarca.tallermecanico.util.Aviso
import com.abarca.tallermecanico.util.ConsultaTienda
import com.abarca.tallermecanico.util.FirestoreOps

// Formulario para crear o editar una tienda en línea.
class TiendaOnlineFormFragment : Fragment() {

    private var _binding: FragmentTiendaOnlineFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TiendaOnlineViewModel by activityViewModels()
    private var entityId = -1
    private var tiendaActual: TiendaOnline? = null
    private var formato = FormatoBusqueda.ESPACIOS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entityId = arguments?.getInt("entityId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTiendaOnlineFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val esNueva = entityId == -1
        requireActivity().title = getString(
            if (esNueva) R.string.title_nueva_tienda_online else R.string.title_editar_tienda_online
        )

        if (!esNueva) tiendaActual = viewModel.getById(entityId)
        tiendaActual?.let {
            formato = it.formato
            binding.etNombreTienda.setText(it.nombre)
            binding.etUrlTienda.setText(it.urlBusqueda)
            binding.etOrdenTienda.setText(it.orden.toString())
            binding.switchActivaTienda.isChecked = it.activa
        }
        if (tiendaActual == null) binding.etOrdenTienda.setText(viewModel.siguienteOrden().toString())

        configurarFormato(savedInstanceState)
        binding.btnProbarTienda.setOnClickListener { probar() }
        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun etiquetaFormato(formato: FormatoBusqueda) = getString(
        if (formato == FormatoBusqueda.GUIONES) R.string.formato_guiones else R.string.formato_espacios
    )

    private fun configurarFormato(savedInstanceState: Bundle?) {
        savedInstanceState?.getString("formato")?.let { formato = FormatoBusqueda.deClave(it) }
        val opciones = FormatoBusqueda.entries
        binding.actvFormatoTienda.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones.map { etiquetaFormato(it) })
        )
        binding.actvFormatoTienda.setText(etiquetaFormato(formato), false)
        binding.actvFormatoTienda.setOnItemClickListener { _, _, posicion, _ -> formato = opciones[posicion] }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("formato", formato.clave)
    }

    // Valida la URL; devuelve null y deja el error en el campo si no cumple.
    private fun urlValidada(): String? {
        val url = binding.etUrlTienda.text?.toString()?.trim().orEmpty()
        return if (ConsultaTienda.urlValida(url)) {
            binding.tilUrlTienda.error = null
            url
        } else {
            binding.tilUrlTienda.error = getString(R.string.error_url_tienda)
            null
        }
    }

    // Abre la URL con una consulta de ejemplo para comprobar que la tienda funciona.
    private fun probar() {
        val url = urlValidada() ?: return
        val destino = ConsultaTienda.url(url, formato, getString(R.string.consulta_prueba_tienda))
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destino)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.msg_sin_app_para_abrir, Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardar() {
        val nombre = binding.etNombreTienda.text?.toString()?.trim().orEmpty()
        val orden = binding.etOrdenTienda.text?.toString()?.trim()?.toIntOrNull()
        var valido = true

        if (nombre.isEmpty()) {
            binding.tilNombreTienda.error = getString(R.string.error_campo_requerido)
            valido = false
        } else binding.tilNombreTienda.error = null

        val url = urlValidada()
        if (url == null) valido = false

        if (orden == null || orden !in 0..1000) {
            binding.tilOrdenTienda.error = getString(R.string.error_orden_tienda)
            valido = false
        } else binding.tilOrdenTienda.error = null

        if (!valido || url == null || orden == null) return

        val tienda = TiendaOnline(
            id = tiendaActual?.id ?: viewModel.siguienteId(),
            nombre = nombre,
            urlBusqueda = url,
            formato = formato,
            orden = orden,
            activa = binding.switchActivaTienda.isChecked
        )
        val contexto = requireContext().applicationContext
        mostrarCarga(true)
        viewModel.guardar(tienda) { error ->
            if (error == null) {
                Aviso.mostrar(contexto, R.string.msg_guardado_ok)
                if (_binding != null) findNavController().popBackStack()
            } else {
                Aviso.mostrar(contexto, FirestoreOps.mensajeError(contexto, error))
                if (_binding != null) mostrarCarga(false)
            }
        }
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !cargando
        binding.btnProbarTienda.isEnabled = !cargando
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
