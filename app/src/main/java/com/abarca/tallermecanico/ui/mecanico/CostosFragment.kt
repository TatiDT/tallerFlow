package com.abarca.tallermecanico.ui.mecanico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Costo
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.databinding.DialogCostoBinding
import com.abarca.tallermecanico.databinding.FragmentCostosBinding
import com.abarca.tallermecanico.ui.viewmodel.CostoViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CostosFragment : Fragment() {

    private var _binding: FragmentCostosBinding? = null
    private val binding get() = _binding!!
    private val costoViewModel: CostoViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private var vehiculoId = -1
    private val soloLectura get() = Sesion.tieneRol(RolUsuario.MECANICO) &&
        vehiculoViewModel.getById(vehiculoId)?.estado != EstadoVehiculo.EN_REPARACION

    companion object {
        fun newInstance(vehiculoId: Int) = CostosFragment().apply {
            arguments = Bundle().apply { putInt("vehiculoId", vehiculoId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehiculoId = arguments?.getInt("vehiculoId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCostosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val adapter = CostoAdapter(
            onItemClick = if (Sesion.usuarioActual?.rol == RolUsuario.JEFE_TALLER) ::mostrarDialogoEditar else null
        )
        if (soloLectura) (binding.tilDescripcionCosto.parent as View).visibility = View.GONE
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        costoViewModel.costos.observe(viewLifecycleOwner) { todos ->
            val propios = todos.filter { it.vehiculoId == vehiculoId }
            adapter.submitList(propios)
            binding.tvEmpty.visibility = if (propios.isEmpty()) View.VISIBLE else View.GONE
            val total = NumberFormat.getNumberInstance(Locale("es", "CL")).format(propios.sumOf { it.monto })
            binding.tvTotalCostos.text = getString(R.string.label_total_costos, "$$total")
        }

        binding.btnAgregarCosto.setOnClickListener {
            val descripcion = binding.etDescripcionCosto.text?.toString()?.trim() ?: ""
            val monto = binding.etMontoCosto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
            var isValid = true
            if (descripcion.isEmpty()) { binding.tilDescripcionCosto.error = getString(R.string.error_campo_requerido); isValid = false }
            else binding.tilDescripcionCosto.error = null
            if (monto == null || monto < 0) { binding.tilMontoCosto.error = getString(R.string.error_monto_invalido); isValid = false }
            else binding.tilMontoCosto.error = null

            if (!isValid) return@setOnClickListener

            val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            costoViewModel.insert(Costo(vehiculoId = vehiculoId, descripcion = descripcion, monto = monto!!, fecha = fecha))
            binding.etDescripcionCosto.text?.clear()
            binding.etMontoCosto.text?.clear()
        }
    }

    private fun mostrarDialogoEditar(costo: Costo) {
        val dialogBinding = DialogCostoBinding.inflate(layoutInflater)
        dialogBinding.etDescripcion.setText(costo.descripcion)
        dialogBinding.etMonto.setText(costo.monto.toLong().toString())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_editar_costo)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .setNeutralButton(R.string.btn_eliminar) { _, _ ->
                costoViewModel.delete(costo)
                AnalyticsLogger.logCrud(requireContext(), "costo", "eliminar")
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val descripcion = dialogBinding.etDescripcion.text?.toString()?.trim() ?: ""
                val monto = dialogBinding.etMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
                var isValid = true
                if (descripcion.isEmpty()) { dialogBinding.tilDescripcion.error = getString(R.string.error_campo_requerido); isValid = false }
                else dialogBinding.tilDescripcion.error = null
                if (monto == null || monto < 0) { dialogBinding.tilMonto.error = getString(R.string.error_monto_invalido); isValid = false }
                else dialogBinding.tilMonto.error = null
                if (!isValid) return@setOnClickListener

                costoViewModel.update(costo.copy(descripcion = descripcion, monto = monto!!))
                AnalyticsLogger.logCrud(requireContext(), "costo", "editar")
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
