package com.abarca.tallermecanico.ui.agenda

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.TipoAgenda
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.data.repository.AgendaRepository
import com.abarca.tallermecanico.databinding.FragmentAgendaFormBinding
import com.abarca.tallermecanico.ui.viewmodel.ClienteViewModel
import com.abarca.tallermecanico.ui.viewmodel.UsuarioViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.FechaAgenda
import com.abarca.tallermecanico.util.SincronizadorAgenda
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class AgendaFormFragment : Fragment() {

    private var _binding: FragmentAgendaFormBinding? = null
    private val binding get() = _binding!!
    private val clienteViewModel: ClienteViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val usuarioViewModel: UsuarioViewModel by activityViewModels()

    private lateinit var tipo: TipoAgenda
    private var vehiculoFijoId = -1
    private var clienteId = -1
    private var vehiculoId = -1
    private var mecanicoId = -1
    private var fechaUtc: Long? = null
    private var hora = -1
    private var minuto = -1

    private var clientes: List<Cliente> = emptyList()
    private var vehiculos: List<Vehiculo> = emptyList()
    private var mecanicos: List<Usuario> = emptyList()

    private val esIntervencion get() = tipo == TipoAgenda.INTERVENCION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tipo = runCatching { TipoAgenda.valueOf(arguments?.getString("tipo").orEmpty()) }.getOrDefault(TipoAgenda.CITA)
        vehiculoFijoId = arguments?.getInt("vehiculoId", -1) ?: -1
        savedInstanceState?.let {
            clienteId = it.getInt("clienteId", -1)
            vehiculoId = it.getInt("vehiculoId", -1)
            mecanicoId = it.getInt("mecanicoId", -1)
            fechaUtc = if (it.containsKey("fechaUtc")) it.getLong("fechaUtc") else null
            hora = it.getInt("hora", -1)
            minuto = it.getInt("minuto", -1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgendaFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(
            if (esIntervencion) R.string.title_programar_intervencion else R.string.title_nueva_cita
        )
        binding.tilDetalle.hint = getString(if (esIntervencion) R.string.hint_trabajo else R.string.hint_servicio)
        binding.tilMecanico.visibility = if (esIntervencion) View.VISIBLE else View.GONE

        if (vehiculoFijoId != -1 && savedInstanceState == null) {
            vehiculoViewModel.getById(vehiculoFijoId)?.let {
                vehiculoId = it.id
                clienteId = it.clienteId
                mecanicoId = it.mecanicoId ?: -1
                binding.etEmail.setText(clienteViewModel.getById(it.clienteId)?.email.orEmpty())
            }
        }
        if (vehiculoFijoId != -1) {
            binding.tilCliente.isEnabled = false
            binding.tilVehiculo.isEnabled = false
        }

        fechaUtc?.let { binding.etFecha.setText(FechaAgenda.fechaDeSelector(it)) }
        if (hora >= 0) binding.etHora.setText(FechaAgenda.hora(hora, minuto))
        binding.etFecha.setOnClickListener { elegirFecha() }
        binding.etHora.setOnClickListener { elegirHora() }

        clienteViewModel.clientes.observe(viewLifecycleOwner) { lista ->
            clientes = lista
            val nombres = lista.map { "${it.nombre} ${it.apellido}" }
            binding.actvCliente.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, nombres))
            binding.actvCliente.setOnItemClickListener { _, _, posicion, _ ->
                val cliente = lista[posicion]
                clienteId = cliente.id
                vehiculoId = -1
                binding.actvVehiculo.setText("", false)
                binding.etEmail.setText(cliente.email)
                binding.tilCliente.error = null
                cargarVehiculos()
            }
            lista.find { it.id == clienteId }?.let { binding.actvCliente.setText("${it.nombre} ${it.apellido}", false) }
        }

        vehiculoViewModel.vehiculos.observe(viewLifecycleOwner) { lista ->
            vehiculos = lista
            cargarVehiculos()
        }

        usuarioViewModel.usuarios.observe(viewLifecycleOwner) { lista ->
            mecanicos = lista.filter { it.rol == RolUsuario.MECANICO && it.activo }
            val nombres = mecanicos.map { "${it.nombre} ${it.apellido}" }
            binding.actvMecanico.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, nombres))
            binding.actvMecanico.setOnItemClickListener { _, _, posicion, _ ->
                mecanicoId = mecanicos[posicion].id
                binding.tilMecanico.error = null
            }
            mecanicos.find { it.id == mecanicoId }?.let { binding.actvMecanico.setText("${it.nombre} ${it.apellido}", false) }
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarVehiculos() {
        val delCliente = vehiculos.filter { it.clienteId == clienteId }
        val etiquetas = delCliente.map { "${it.patente} · ${it.marca} ${it.modelo}" }
        binding.actvVehiculo.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, etiquetas))
        binding.actvVehiculo.setOnItemClickListener { _, _, posicion, _ ->
            vehiculoId = delCliente[posicion].id
            binding.tilVehiculo.error = null
        }
        delCliente.indexOfFirst { it.id == vehiculoId }.takeIf { it >= 0 }?.let {
            binding.actvVehiculo.setText(etiquetas[it], false)
        }
    }

    private fun elegirFecha() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.hint_fecha)
            .setSelection(fechaUtc ?: MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
            .build()
        picker.addOnPositiveButtonClickListener {
            fechaUtc = it
            binding.etFecha.setText(FechaAgenda.fechaDeSelector(it))
            binding.tilFecha.error = null
        }
        picker.show(childFragmentManager, "fecha_agenda")
    }

    private fun elegirHora() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(if (hora >= 0) hora else 9)
            .setMinute(if (minuto >= 0) minuto else 0)
            .setTitleText(R.string.hint_hora)
            .build()
        picker.addOnPositiveButtonClickListener {
            hora = picker.hour
            minuto = picker.minute
            binding.etHora.setText(FechaAgenda.hora(hora, minuto))
            binding.tilHora.error = null
        }
        picker.show(childFragmentManager, "hora_agenda")
    }

    private fun guardar() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val detalle = binding.etDetalle.text?.toString()?.trim().orEmpty()
        val duracion = binding.etDuracion.text?.toString()?.trim()?.toIntOrNull()
        val requerido = getString(R.string.error_campo_requerido)
        var valido = true

        val cliente = clientes.find { it.id == clienteId }
        val vehiculo = vehiculos.find { it.id == vehiculoId }
        val mecanico = mecanicos.find { it.id == mecanicoId }

        if (cliente == null) { binding.tilCliente.error = requerido; valido = false } else binding.tilCliente.error = null
        if (vehiculo == null) { binding.tilVehiculo.error = requerido; valido = false } else binding.tilVehiculo.error = null
        if (email.isEmpty()) {
            binding.tilEmail.error = requerido; valido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalido); valido = false
        } else binding.tilEmail.error = null
        if (detalle.isEmpty()) { binding.tilDetalle.error = requerido; valido = false } else binding.tilDetalle.error = null
        if (esIntervencion) {
            if (mecanico == null) { binding.tilMecanico.error = requerido; valido = false } else binding.tilMecanico.error = null
        }
        if (duracion == null || duracion < 15 || duracion > 720) {
            binding.tilDuracion.error = getString(R.string.error_duracion_invalida); valido = false
        } else binding.tilDuracion.error = null

        val fecha = fechaUtc
        var inicio = 0L
        if (fecha == null) {
            binding.tilFecha.error = getString(R.string.error_fecha_requerida); valido = false
        } else binding.tilFecha.error = null
        if (hora < 0) {
            binding.tilHora.error = getString(R.string.error_hora_requerida); valido = false
        } else binding.tilHora.error = null
        if (fecha != null && hora >= 0) {
            inicio = FechaAgenda.combinar(fecha, hora, minuto)
            if (inicio <= System.currentTimeMillis()) {
                binding.tilHora.error = getString(R.string.error_fecha_pasada); valido = false
            }
        }
        if (!valido || cliente == null || vehiculo == null || duracion == null) return

        val evento = EventoAgenda(
            id = AgendaRepository.siguienteId(tipo),
            tipo = tipo,
            clienteNombre = "${cliente.nombre} ${cliente.apellido}",
            clienteEmail = email,
            vehiculoPatente = vehiculo.patente,
            vehiculoDescripcion = "${vehiculo.marca} ${vehiculo.modelo}",
            detalle = detalle,
            inicio = inicio,
            duracionMin = duracion,
            mecanicoNombre = mecanico?.let { "${it.nombre} ${it.apellido}" }.orEmpty(),
            mecanicoEmail = mecanico?.email.orEmpty(),
            ordenTrabajoId = if (esIntervencion) vehiculo.id else 0
        )

        // Se guarda primero en Firestore (funciona sin conexión) y luego se intenta crear el evento
        AgendaRepository.guardar(evento)
        val ctx = requireContext().applicationContext
        SincronizadorAgenda.sincronizar(ctx, evento) { resultado ->
            val mensaje = if (resultado.syncError.isBlank()) {
                ctx.getString(R.string.cal_msg_sincronizada)
            } else {
                ctx.getString(R.string.cal_msg_guardada_sin_sync, resultado.syncError)
            }
            Toast.makeText(ctx, mensaje, Toast.LENGTH_LONG).show()
        }
        binding.progress.visibility = View.VISIBLE
        binding.btnGuardar.isEnabled = false
        findNavController().popBackStack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("clienteId", clienteId)
        outState.putInt("vehiculoId", vehiculoId)
        outState.putInt("mecanicoId", mecanicoId)
        fechaUtc?.let { outState.putLong("fechaUtc", it) }
        outState.putInt("hora", hora)
        outState.putInt("minuto", minuto)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
