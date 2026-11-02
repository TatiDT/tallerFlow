package com.abarca.tallermecanico.ui.usuario

import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.databinding.FragmentUsuarioFormBinding
import com.abarca.tallermecanico.ui.viewmodel.UsuarioViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.PasswordUtils
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UsuarioFormFragment : Fragment() {

    private var _binding: FragmentUsuarioFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UsuarioViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()

    private var entityId = -1
    private var currentUsuario: Usuario? = null
    private var selectedRol = RolUsuario.MECANICO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        entityId = arguments?.getInt("entityId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUsuarioFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = if (entityId == -1) getString(R.string.title_nuevo_usuario)
        else getString(R.string.title_editar_usuario)

        configurarRolDropdown()

        if (entityId != -1) {
            binding.tilPassword.hint = getString(R.string.hint_password_edit)
            viewModel.getById(entityId)?.let { u ->
                currentUsuario = u
                selectedRol = u.rol
                binding.etNombre.setText(u.nombre)
                binding.etApellido.setText(u.apellido)
                binding.etUsername.setText(u.username)
                binding.etEmail.setText(u.email)
                binding.actvRol.setText(u.rol.display, false)
                binding.switchActivo.isChecked = u.activo
            }
        }
        savedInstanceState?.getString(KEY_ROL)?.let {
            selectedRol = RolUsuario.valueOf(it)
            binding.actvRol.setText(selectedRol.display, false)
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ROL, selectedRol.name)
    }

    private fun configurarRolDropdown() {
        val roles = RolUsuario.entries.map { it.display }
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, roles)
        binding.actvRol.setAdapter(adapter)
        binding.actvRol.setText(RolUsuario.MECANICO.display, false)
        binding.actvRol.setOnItemClickListener { _, _, position, _ ->
            selectedRol = RolUsuario.entries[position]
        }
    }

    private fun guardar() {
        val nombre = binding.etNombre.text?.toString()?.trim() ?: ""
        val apellido = binding.etApellido.text?.toString()?.trim() ?: ""
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val activo = binding.switchActivo.isChecked
        var isValid = true

        if (nombre.isEmpty()) { binding.tilNombre.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilNombre.error = null
        if (apellido.isEmpty()) { binding.tilApellido.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilApellido.error = null
        if (username.isEmpty()) { binding.tilUsername.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilUsername.error = null
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalido); isValid = false
        } else binding.tilEmail.error = null
        if ((entityId == -1 || password.isNotEmpty()) && password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            isValid = false
        } else binding.tilPassword.error = null

        if (!isValid) return

        // Desactivar o cambiar de rol tiene el mismo efecto que eliminar para el login y para los vehículos asignados
        currentUsuario?.let { u ->
            if (!activo || selectedRol != u.rol) {
                errorAlQuitar(u, eliminar = false)?.let { mostrarError(it); return }
            }
        }

        val excludeId = currentUsuario?.id ?: 0
        if (viewModel.isUsernameDuplicate(username, excludeId)) {
            binding.tilUsername.error = getString(R.string.error_username_duplicado)
            return
        }

        if (viewModel.isEmailDuplicate(email, excludeId)) {
            binding.tilEmail.error = getString(R.string.error_email_usuario_duplicado)
            return
        }

        // El login compara contra el hash, así que la contraseña nueva se guarda hasheada
        val passwordFinal = when {
            password.isNotEmpty() -> PasswordUtils.hash(password)
            currentUsuario != null -> currentUsuario!!.password
            else -> ""
        }

        val usuario = Usuario(
            id = currentUsuario?.id ?: 0,
            nombre = nombre,
            apellido = apellido,
            username = username,
            email = email,
            password = passwordFinal,
            rol = selectedRol,
            activo = activo
        )
        if (currentUsuario == null) {
            viewModel.insert(usuario)
            AnalyticsLogger.logCrud(requireContext(), "usuario", "crear")
        } else {
            viewModel.update(usuario)
            AnalyticsLogger.logCrud(requireContext(), "usuario", "editar")
        }
        findNavController().popBackStack()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (entityId != -1) inflater.inflate(R.menu.menu_form, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            currentUsuario?.let { u -> errorAlQuitar(u, eliminar = true)?.let { mostrarError(it); return true } }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                    currentUsuario?.let { viewModel.delete(it) }
                    AnalyticsLogger.logCrud(requireContext(), "usuario", "eliminar")
                    findNavController().popBackStack()
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** Motivo por el que el usuario no puede eliminarse, desactivarse ni cambiar de rol, o null si puede. */
    private fun errorAlQuitar(usuario: Usuario, eliminar: Boolean): Int? = when {
        usuario.id == Sesion.usuarioActual?.id -> R.string.error_usuario_propio
        usuario.rol != RolUsuario.MECANICO -> null
        vehiculoViewModel.tieneVehiculosSinEntregar(usuario.id) -> R.string.error_mecanico_con_vehiculos
        eliminar && vehiculoViewModel.tieneVehiculos(usuario.id) -> R.string.error_mecanico_con_historial
        else -> null
    }

    private fun mostrarError(mensaje: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(mensaje)
            .setPositiveButton(R.string.btn_cerrar, null)
            .show()
    }

    companion object {
        private const val KEY_ROL = "selectedRol"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
