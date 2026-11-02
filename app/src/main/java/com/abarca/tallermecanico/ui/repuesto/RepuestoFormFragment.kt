package com.abarca.tallermecanico.ui.repuesto

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Repuesto
import com.abarca.tallermecanico.databinding.FragmentRepuestoFormBinding
import com.abarca.tallermecanico.ui.viewmodel.RepuestoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.FirestoreOps
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RepuestoFormFragment : Fragment() {

    private var _binding: FragmentRepuestoFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RepuestoViewModel by activityViewModels()
    private var entityId = -1
    private var currentRepuesto: Repuesto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        entityId = arguments?.getInt("entityId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRepuestoFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = if (entityId == -1) getString(R.string.title_nuevo_repuesto)
        else getString(R.string.title_editar_repuesto)

        if (entityId != -1) {
            viewModel.getById(entityId)?.let { repuesto ->
                currentRepuesto = repuesto
                binding.etNombreRepuesto.setText(repuesto.nombre)
                binding.etCodigoRepuesto.setText(repuesto.codigo)
                binding.etCategoria.setText(repuesto.categoria)
                binding.etPrecio.setText(repuesto.precio.toString())
                binding.etStock.setText(repuesto.stock.toString())
            }
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun guardar() {
        val nombre = binding.etNombreRepuesto.text?.toString()?.trim() ?: ""
        val codigo = binding.etCodigoRepuesto.text?.toString()?.trim()?.uppercase() ?: ""
        val categoria = binding.etCategoria.text?.toString()?.trim() ?: ""
        val precioStr = binding.etPrecio.text?.toString()?.trim() ?: ""
        val stockStr = binding.etStock.text?.toString()?.trim() ?: ""
        var isValid = true

        if (nombre.isEmpty()) { binding.tilNombreRepuesto.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilNombreRepuesto.error = null
        if (codigo.isEmpty()) { binding.tilCodigoRepuesto.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilCodigoRepuesto.error = null
        if (categoria.isEmpty()) { binding.tilCategoria.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilCategoria.error = null
        val precio = precioStr.toIntOrNull()
        if (precio == null || precio !in 1..10_000_000) { binding.tilPrecio.error = getString(R.string.error_precio_invalido); isValid = false }
        else binding.tilPrecio.error = null
        val stock = stockStr.toIntOrNull()
        if (stock == null || stock !in 0..100_000) { binding.tilStock.error = getString(R.string.error_stock_invalido); isValid = false }
        else binding.tilStock.error = null

        if (!isValid) return

        if (viewModel.isCodigoDuplicate(codigo, currentRepuesto?.id ?: 0)) {
            binding.tilCodigoRepuesto.error = getString(R.string.error_codigo_duplicado)
            return
        }

        val repuesto = Repuesto(
            id = currentRepuesto?.id ?: 0,
            nombre = nombre,
            codigo = codigo,
            categoria = categoria,
            precio = precio!!,
            stock = stock!!
        )
        val esNuevo = currentRepuesto == null
        val ctx = requireContext().applicationContext
        mostrarCarga(true)
        val alTerminar: (Throwable?) -> Unit = { error ->
            if (error == null) {
                if (esNuevo) AnalyticsLogger.logRegistroCreado(ctx, "repuesto")
                else AnalyticsLogger.logRegistroActualizado(ctx, "repuesto")
                Toast.makeText(ctx, R.string.msg_guardado_ok, Toast.LENGTH_SHORT).show()
                if (_binding != null) findNavController().popBackStack()
            } else {
                Toast.makeText(ctx, FirestoreOps.mensajeError(ctx, error), Toast.LENGTH_LONG).show()
                if (_binding != null) mostrarCarga(false)
            }
        }
        if (esNuevo) viewModel.insert(repuesto, alTerminar) else viewModel.update(repuesto, alTerminar)
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !cargando
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (entityId != -1) inflater.inflate(R.menu.menu_form, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                    val repuesto = currentRepuesto ?: return@setPositiveButton
                    val ctx = requireContext().applicationContext
                    mostrarCarga(true)
                    viewModel.delete(repuesto) { error ->
                        if (error == null) {
                            AnalyticsLogger.logRegistroEliminado(ctx, "repuesto")
                            Toast.makeText(ctx, R.string.msg_eliminado_ok, Toast.LENGTH_SHORT).show()
                            if (_binding != null) findNavController().popBackStack()
                        } else {
                            Toast.makeText(ctx, FirestoreOps.mensajeError(ctx, error), Toast.LENGTH_LONG).show()
                            if (_binding != null) mostrarCarga(false)
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
