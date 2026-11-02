package com.abarca.tallermecanico.ui.cliente

import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.databinding.FragmentClienteFormBinding
import com.abarca.tallermecanico.ui.viewmodel.ClienteViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Rut
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ClienteFormFragment : Fragment() {

    private var _binding: FragmentClienteFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClienteViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private var entityId = -1
    private var currentCliente: Cliente? = null
    private val soloLectura get() = !Sesion.tieneRol(RolUsuario.ADMIN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        entityId = arguments?.getInt("entityId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClienteFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = if (entityId == -1) getString(R.string.title_nuevo_cliente)
        else getString(R.string.title_editar_cliente)

        if (entityId != -1) {
            viewModel.getById(entityId)?.let { cliente ->
                currentCliente = cliente
                binding.etNombre.setText(cliente.nombre)
                binding.etApellido.setText(cliente.apellido)
                binding.etRut.setText(cliente.rut)
                binding.etTelefono.setText(cliente.telefono)
                binding.etEmail.setText(cliente.email)
            }
        }

        if (soloLectura) {
            listOf(binding.tilNombre, binding.tilApellido, binding.tilRut, binding.tilTelefono, binding.tilEmail)
                .forEach { it.isEnabled = false }
            binding.btnGuardar.visibility = View.GONE
        }
        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun guardar() {
        val nombre = binding.etNombre.text?.toString()?.trim() ?: ""
        val apellido = binding.etApellido.text?.toString()?.trim() ?: ""
        val rut = binding.etRut.text?.toString()?.trim() ?: ""
        val telefono = binding.etTelefono.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        var isValid = true

        if (nombre.isEmpty()) { binding.tilNombre.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilNombre.error = null
        if (apellido.isEmpty()) { binding.tilApellido.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilApellido.error = null
        if (rut.isEmpty()) { binding.tilRut.error = getString(R.string.error_campo_requerido); isValid = false }
        else if (!Rut.esValido(rut)) { binding.tilRut.error = getString(R.string.error_rut_invalido); isValid = false }
        else binding.tilRut.error = null
        if (telefono.isEmpty()) { binding.tilTelefono.error = getString(R.string.error_campo_requerido); isValid = false }
        else if (telefono.count { it.isDigit() } < 8) { binding.tilTelefono.error = getString(R.string.error_telefono_invalido); isValid = false }
        else binding.tilTelefono.error = null
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalido); isValid = false
        } else binding.tilEmail.error = null

        if (!isValid) return

        if (viewModel.isRutDuplicate(rut, currentCliente?.id ?: 0)) {
            binding.tilRut.error = getString(R.string.error_rut_duplicado)
            return
        }

        val cliente = Cliente(
            id = currentCliente?.id ?: 0,
            nombre = nombre,
            apellido = apellido,
            rut = Rut.formatear(rut),
            telefono = telefono,
            email = email
        )
        val esNuevo = currentCliente == null
        val ctx = requireContext().applicationContext
        mostrarCarga(true)
        val alTerminar: (Throwable?) -> Unit = { error ->
            if (error == null) {
                if (esNuevo) AnalyticsLogger.logRegistroCreado(ctx, "cliente")
                else AnalyticsLogger.logRegistroActualizado(ctx, "cliente")
                Toast.makeText(ctx, R.string.msg_guardado_ok, Toast.LENGTH_SHORT).show()
                if (_binding != null) findNavController().popBackStack()
            } else {
                Toast.makeText(ctx, FirestoreOps.mensajeError(ctx, error), Toast.LENGTH_LONG).show()
                if (_binding != null) mostrarCarga(false)
            }
        }
        if (esNuevo) viewModel.insert(cliente, alTerminar) else viewModel.update(cliente, alTerminar)
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !cargando
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (entityId != -1 && !soloLectura) inflater.inflate(R.menu.menu_form, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            mostrarConfirmacionEliminar()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mostrarConfirmacionEliminar() {
        // Sus vehículos quedarían ocultos en las listas pero seguirían ocupando patente y cupo del mecánico
        if (vehiculoViewModel.vehiculos.value.orEmpty().any { it.clienteId == currentCliente?.id }) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.error_cliente_con_vehiculos)
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                val cliente = currentCliente ?: return@setPositiveButton
                val ctx = requireContext().applicationContext
                mostrarCarga(true)
                viewModel.delete(cliente) { error ->
                    if (error == null) {
                        AnalyticsLogger.logRegistroEliminado(ctx, "cliente")
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
