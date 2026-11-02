package com.abarca.tallermecanico.ui.vehiculo

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.databinding.DialogFallaBinding
import com.abarca.tallermecanico.databinding.FragmentVehiculoFormBinding
import com.abarca.tallermecanico.ui.viewmodel.ClienteViewModel
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.ui.viewmodel.TareaViewModel
import com.abarca.tallermecanico.ui.viewmodel.UsuarioViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class VehiculoFormFragment : Fragment() {

    private var _binding: FragmentVehiculoFormBinding? = null
    private val binding get() = _binding!!
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val clienteViewModel: ClienteViewModel by activityViewModels()
    private val usuarioViewModel: UsuarioViewModel by activityViewModels()
    private val presupuestoViewModel: PresupuestoViewModel by activityViewModels()
    private val tareaViewModel: TareaViewModel by activityViewModels()

    private var entityId = -1
    private var currentVehiculo: Vehiculo? = null
    private var clientesList: List<Cliente> = emptyList()
    private var mecanicosList: List<Usuario> = emptyList()
    private var selectedClienteId = -1
    private var selectedMecanicoId = -1
    private var fechaIngreso = ""
    private var ocupado = false
    private val esJefe get() = Sesion.usuarioActual?.rol == RolUsuario.JEFE_TALLER
    private val esAdmin get() = Sesion.usuarioActual?.rol == RolUsuario.ADMIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        entityId = arguments?.getInt("entityId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehiculoFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = if (entityId == -1) getString(R.string.title_nuevo_vehiculo)
        else getString(R.string.title_editar_vehiculo)

        binding.tilEstado.isEnabled = false
        binding.actvEstado.setText(EstadoVehiculo.INGRESADO.display, false)

        binding.tilFechaLimite.visibility = if (esJefe) View.VISIBLE else View.GONE
        binding.etFechaLimite.setOnClickListener { mostrarCalendarioFechaLimite() }

        if (entityId != -1) {
            vehiculoViewModel.getById(entityId)?.let { v ->
                currentVehiculo = v
                selectedClienteId = v.clienteId
                selectedMecanicoId = v.mecanicoId ?: -1
                fechaIngreso = v.fechaIngreso
                binding.etPatente.setText(v.patente)
                binding.etMarca.setText(v.marca)
                binding.etModelo.setText(v.modelo)
                binding.etAnio.setText(v.año.toString())
                binding.etColor.setText(v.color)
                binding.etKilometraje.setText(v.kilometraje.toString())
                binding.etObservaciones.setText(v.observaciones)
                binding.etFechaLimite.setText(v.fechaLimite)
                binding.actvEstado.setText(v.estado.display, false)
            }
        } else {
            fechaIngreso = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }
        savedInstanceState?.let {
            selectedClienteId = it.getInt(KEY_CLIENTE, selectedClienteId)
            selectedMecanicoId = it.getInt(KEY_MECANICO, selectedMecanicoId)
        }
        binding.etFechaIngreso.setText(fechaIngreso)
        // Terminado/Listo/Entregado: quitar o cambiar el mecánico dejaría el trabajo sin responsable
        val estadoActual = currentVehiculo?.estado ?: EstadoVehiculo.INGRESADO
        binding.tilMecanico.isEnabled = esJefe &&
            (estadoActual == EstadoVehiculo.INGRESADO || estadoActual == EstadoVehiculo.EN_REPARACION)

        clienteViewModel.clientes.observe(viewLifecycleOwner) { clientes ->
            clientesList = clientes
            val nombres = clientes.map { "${it.nombre} ${it.apellido}" }
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, nombres)
            binding.actvCliente.setAdapter(adapter)
            binding.actvCliente.setOnItemClickListener { _, _, position, _ ->
                selectedClienteId = clientes[position].id
            }
            if (selectedClienteId != -1) {
                val idx = clientes.indexOfFirst { it.id == selectedClienteId }
                if (idx >= 0) binding.actvCliente.setText(nombres[idx], false)
            }
        }

        usuarioViewModel.usuarios.observe(viewLifecycleOwner) { usuarios ->
            mecanicosList = usuarios.filter { it.rol == RolUsuario.MECANICO && it.activo }
            val sinAsignar = getString(R.string.label_sin_asignar)
            val nombres = listOf(sinAsignar) + mecanicosList.map { "${it.nombre} ${it.apellido}" }
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, nombres)
            binding.actvMecanico.setAdapter(adapter)
            binding.actvMecanico.setOnItemClickListener { _, _, position, _ ->
                selectedMecanicoId = if (position == 0) -1 else mecanicosList[position - 1].id
                binding.tilMecanico.error = null
            }
            if (selectedMecanicoId == -1) {
                binding.actvMecanico.setText(sinAsignar, false)
            } else {
                val idx = mecanicosList.indexOfFirst { it.id == selectedMecanicoId }
                if (idx >= 0) binding.actvMecanico.setText(nombres[idx + 1], false)
            }
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CLIENTE, selectedClienteId)
        outState.putInt(KEY_MECANICO, selectedMecanicoId)
    }

    private fun mostrarCalendarioFechaLimite() {
        // MaterialDatePicker trabaja en UTC; se formatea en UTC para no correr la fecha un día
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val actual = binding.etFechaLimite.text?.toString().orEmpty()
        val seleccion = runCatching { sdf.parse(actual)?.time }.getOrNull()
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.hint_fecha_limite)
            .setSelection(seleccion)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now())
                    .build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            binding.etFechaLimite.setText(sdf.format(Date(millis)))
        }
        picker.show(childFragmentManager, "fecha_limite")
    }

    private fun guardar() {
        val patente = binding.etPatente.text?.toString()?.uppercase()?.filter { it.isLetterOrDigit() } ?: ""
        val marca = binding.etMarca.text?.toString()?.trim() ?: ""
        val modelo = binding.etModelo.text?.toString()?.trim() ?: ""
        val añoStr = binding.etAnio.text?.toString()?.trim() ?: ""
        val color = binding.etColor.text?.toString()?.trim() ?: ""
        val kmStr = binding.etKilometraje.text?.toString()?.trim() ?: ""
        val observaciones = binding.etObservaciones.text?.toString()?.trim() ?: ""
        val fechaLimiteInput = binding.etFechaLimite.text?.toString()?.trim() ?: ""
        var isValid = true

        if (patente.isEmpty()) { binding.tilPatente.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilPatente.error = null
        if (marca.isEmpty()) { binding.tilMarca.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilMarca.error = null
        if (modelo.isEmpty()) { binding.tilModelo.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilModelo.error = null
        if (color.isEmpty()) { binding.tilColor.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilColor.error = null
        val año = añoStr.toIntOrNull()
        val añoMaximo = Calendar.getInstance().get(Calendar.YEAR) + 1
        if (año == null || año < 1900 || año > añoMaximo) { binding.tilAnio.error = getString(R.string.error_anio_invalido); isValid = false }
        else binding.tilAnio.error = null
        val kilometraje = if (kmStr.isEmpty()) 0 else kmStr.toIntOrNull()
        if (kilometraje == null || kilometraje < 0) { binding.tilKilometraje.error = getString(R.string.error_km_invalido); isValid = false }
        else binding.tilKilometraje.error = null
        if (selectedClienteId == -1) { binding.tilCliente.error = getString(R.string.error_campo_requerido); isValid = false }
        else binding.tilCliente.error = null

        if (!isValid) return

        if (vehiculoViewModel.isPatenteDuplicate(patente, currentVehiculo?.id ?: 0)) {
            binding.tilPatente.error = getString(R.string.error_patente_duplicada)
            return
        }

        // Asignar mecánico (solo Jefe): requiere presupuesto aprobado y pasa el vehículo a reparación
        var nuevoEstado = currentVehiculo?.estado ?: EstadoVehiculo.INGRESADO
        val mecanicoAnterior = currentVehiculo?.mecanicoId ?: -1
        if (esJefe && selectedMecanicoId != mecanicoAnterior) {
            if (selectedMecanicoId != -1) {
                if (presupuestoViewModel.getByVehiculoId(entityId)?.aprobado != true) {
                    binding.tilMecanico.error = getString(R.string.error_presupuesto_no_aprobado)
                    return
                }
                if (nuevoEstado == EstadoVehiculo.INGRESADO) nuevoEstado = EstadoVehiculo.EN_REPARACION
            } else if (nuevoEstado == EstadoVehiculo.EN_REPARACION) {
                nuevoEstado = EstadoVehiculo.INGRESADO
            }
        }

        if (selectedMecanicoId != -1 &&
            vehiculoViewModel.superaLimiteMecanico(selectedMecanicoId, nuevoEstado, currentVehiculo?.id ?: 0)
        ) {
            binding.tilMecanico.error = getString(R.string.error_mecanico_limite)
            return
        }

        val vehiculo = Vehiculo(
            id = currentVehiculo?.id ?: 0,
            patente = patente,
            marca = marca,
            modelo = modelo,
            año = año!!,
            color = color,
            clienteId = selectedClienteId,
            estado = nuevoEstado,
            kilometraje = kilometraje!!,
            mecanicoId = selectedMecanicoId.takeIf { it != -1 },
            fechaIngreso = fechaIngreso,
            observaciones = observaciones,
            fechaLimite = if (esJefe) fechaLimiteInput else currentVehiculo?.fechaLimite ?: ""
        )
        val esNuevo = currentVehiculo == null
        val ctx = requireContext().applicationContext
        mostrarCarga(true)
        val alTerminar = alConfirmar(ctx, if (esNuevo) "crear" else "editar")
        if (esNuevo) vehiculoViewModel.insert(vehiculo, alTerminar) else vehiculoViewModel.update(vehiculo, alTerminar)
    }

    private fun mostrarCarga(cargando: Boolean) {
        ocupado = cargando
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !cargando
        activity?.invalidateOptionsMenu()
    }

    private fun alConfirmar(
        ctx: Context,
        accion: String,
        mensajeOk: Int = R.string.msg_guardado_ok,
        alExito: () -> Unit = {},
        alFallar: () -> Unit = {}
    ): (Throwable?) -> Unit = { error ->
        if (error == null) {
            alExito()
            AnalyticsLogger.logCrud(ctx, "vehiculo", accion)
            Toast.makeText(ctx, mensajeOk, Toast.LENGTH_SHORT).show()
            if (_binding != null) findNavController().popBackStack()
        } else {
            Toast.makeText(ctx, FirestoreOps.mensajeError(ctx, error), Toast.LENGTH_LONG).show()
            alFallar()
            if (_binding != null) mostrarCarga(false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (entityId != -1) {
            inflater.inflate(R.menu.menu_vehiculo_form, menu)
            val estado = vehiculoViewModel.getById(entityId)?.estado
            menu.findItem(R.id.action_inspeccion)?.isVisible = esJefe
            menu.findItem(R.id.action_costos)?.isVisible = esJefe
            menu.findItem(R.id.action_aprobar_trabajo)?.isVisible = esJefe && estado == EstadoVehiculo.TERMINADO
            menu.findItem(R.id.action_devolver)?.isVisible = esJefe && estado == EstadoVehiculo.TERMINADO
            menu.findItem(R.id.action_entregar)?.isVisible = esAdmin && estado == EstadoVehiculo.LISTO
            menu.findItem(R.id.action_delete)?.isVisible = esAdmin && estado == EstadoVehiculo.INGRESADO
            for (i in 0 until menu.size()) menu.getItem(i).isEnabled = !ocupado
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (ocupado) return true
        if (item.itemId == R.id.action_inspeccion) {
            val bundle = Bundle().apply { putInt("vehiculoId", entityId) }
            findNavController().navigate(R.id.action_vehiculo_form_to_inspeccion, bundle)
            return true
        }
        if (item.itemId == R.id.action_costos) {
            val bundle = Bundle().apply { putInt("vehiculoId", entityId) }
            findNavController().navigate(R.id.action_vehiculo_form_to_costos, bundle)
            return true
        }
        when (item.itemId) {
            R.id.action_aprobar_trabajo -> {
                val incompleto = tareaViewModel.getByVehiculoId(entityId).any { !it.completada }
                val adicionalesPendientes = presupuestoViewModel.tareasBloqueadas(entityId).isNotEmpty()
                if (incompleto || adicionalesPendientes) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(
                            if (adicionalesPendientes) R.string.msg_adicionales_pendientes else R.string.error_trabajo_incompleto
                        )
                        .setPositiveButton(R.string.btn_cerrar, null)
                        .show()
                    return true
                }
                cambiarEstado(EstadoVehiculo.LISTO, R.string.confirm_aprobar_trabajo_title,
                    R.string.confirm_aprobar_trabajo_message, "aprobar_trabajo")
                return true
            }
            R.id.action_devolver -> {
                devolverAlMecanico()
                return true
            }
            R.id.action_entregar -> {
                cambiarEstado(EstadoVehiculo.ENTREGADO, R.string.confirm_entregar_title,
                    R.string.confirm_entregar_message, "entregar")
                return true
            }
        }
        if (item.itemId == R.id.action_delete) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                    val vehiculo = currentVehiculo ?: return@setPositiveButton
                    val ctx = requireContext().applicationContext
                    mostrarCarga(true)
                    vehiculoViewModel.delete(vehiculo, alConfirmar(ctx, "eliminar", R.string.msg_eliminado_ok))
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun cambiarEstado(nuevo: EstadoVehiculo, titulo: Int, mensaje: Int, accion: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                val vehiculo = vehiculoViewModel.getById(entityId) ?: return@setPositiveButton
                val ctx = requireContext().applicationContext
                mostrarCarga(true)
                vehiculoViewModel.update(vehiculo.copy(estado = nuevo), alConfirmar(ctx, accion))
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun devolverAlMecanico() {
        val dialogBinding = DialogFallaBinding.inflate(layoutInflater)
        dialogBinding.tilDescripcionFalla.hint = getString(R.string.hint_motivo_devolucion)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_devolver_title)
            .setMessage(R.string.confirm_devolver_message)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_confirmar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val motivo = dialogBinding.etDescripcionFalla.text?.toString()?.trim() ?: ""
                if (motivo.isEmpty()) {
                    dialogBinding.tilDescripcionFalla.error = getString(R.string.error_campo_requerido)
                    return@setOnClickListener
                }
                val vehiculo = vehiculoViewModel.getById(entityId) ?: return@setOnClickListener
                val ctx = requireContext().applicationContext
                val boton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                boton.isEnabled = false
                mostrarCarga(true)
                vehiculoViewModel.update(
                    vehiculo.copy(estado = EstadoVehiculo.EN_REPARACION),
                    alConfirmar(
                        ctx,
                        "devolver_mecanico",
                        alExito = {
                            tareaViewModel.agregarPendiente(vehiculo.id, motivo)
                            dialog.dismiss()
                        },
                        alFallar = { boton.isEnabled = true }
                    )
                )
            }
        }
        dialog.show()
    }

    companion object {
        private const val KEY_CLIENTE = "selectedClienteId"
        private const val KEY_MECANICO = "selectedMecanicoId"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
