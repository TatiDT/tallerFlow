package com.abarca.tallermecanico.ui.mecanico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Falla
import com.abarca.tallermecanico.databinding.DialogFallaBinding
import com.abarca.tallermecanico.databinding.FragmentInspeccionBinding
import com.abarca.tallermecanico.ui.viewmodel.FallaViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.Sesion
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InspeccionFragment : Fragment() {

    private var _binding: FragmentInspeccionBinding? = null
    private val binding get() = _binding!!
    private val fallaViewModel: FallaViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private var vehiculoId = -1
    private val soloLectura get() = Sesion.tieneRol(RolUsuario.MECANICO) &&
        vehiculoViewModel.getById(vehiculoId)?.estado != EstadoVehiculo.EN_REPARACION

    companion object {
        fun newInstance(vehiculoId: Int) = InspeccionFragment().apply {
            arguments = Bundle().apply { putInt("vehiculoId", vehiculoId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehiculoId = arguments?.getInt("vehiculoId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInspeccionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editable = !soloLectura
        val adapter = FallaAdapter { falla -> if (editable) confirmarEliminarFalla(falla) }
        if (!editable) binding.tvAyudaInspeccion.visibility = View.GONE
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.flMarcadores.setOnTouchListener { v, event ->
            if (editable && event.action == MotionEvent.ACTION_UP && v.width > 0 && v.height > 0) {
                val relX = (event.x / v.width).coerceIn(0f, 1f)
                val relY = (event.y / v.height).coerceIn(0f, 1f)
                mostrarDialogoFalla(relX, relY)
                v.performClick()
            }
            true
        }

        fallaViewModel.fallas.observe(viewLifecycleOwner) { todas ->
            val propias = todas.filter { it.vehiculoId == vehiculoId }
            // El número de cada falla depende de su posición: al borrar una hay que renumerar todas
            adapter.submitList(propias) { adapter.notifyItemRangeChanged(0, propias.size) }
            binding.tvEmpty.visibility = if (propias.isEmpty()) View.VISIBLE else View.GONE
            dibujarMarcadores(propias)
        }
    }

    private fun dibujarMarcadores(fallas: List<Falla>) {
        binding.flMarcadores.post {
            if (_binding == null) return@post
            binding.flMarcadores.removeAllViews()
            val w = binding.flMarcadores.width
            val h = binding.flMarcadores.height
            if (w == 0 || h == 0) return@post
            val size = (24 * resources.displayMetrics.density).toInt()
            fallas.forEachIndexed { index, falla ->
                val marker = TextView(requireContext()).apply {
                    text = (index + 1).toString()
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    textSize = 11f
                    gravity = android.view.Gravity.CENTER
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_marker_circle)
                }
                val params = FrameLayout.LayoutParams(size, size).apply {
                    leftMargin = (falla.x * w - size / 2).toInt().coerceIn(0, (w - size).coerceAtLeast(0))
                    topMargin = (falla.y * h - size / 2).toInt().coerceIn(0, (h - size).coerceAtLeast(0))
                }
                binding.flMarcadores.addView(marker, params)
            }
        }
    }

    private fun mostrarDialogoFalla(x: Float, y: Float) {
        val dialogBinding = DialogFallaBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_nueva_falla)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val descripcion = dialogBinding.etDescripcionFalla.text?.toString()?.trim() ?: ""
                if (descripcion.isEmpty()) {
                    dialogBinding.tilDescripcionFalla.error = getString(R.string.error_campo_requerido)
                    return@setOnClickListener
                }
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                fallaViewModel.insert(Falla(vehiculoId = vehiculoId, x = x, y = y, descripcion = descripcion, fecha = fecha))
                AnalyticsLogger.logCrud(requireContext(), "falla", "crear")
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmarEliminarFalla(falla: Falla) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                if (fallaViewModel.delete(falla)) {
                    AnalyticsLogger.logCrud(requireContext(), "falla", "eliminar")
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.error_falla_presupuestada)
                        .setPositiveButton(R.string.btn_cerrar, null)
                        .show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
