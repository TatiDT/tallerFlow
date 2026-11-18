package com.abarca.tallermecanico.ui.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abarca.tallermecanico.util.Aviso
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EstadoAgenda
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.data.entity.TipoAgenda
import com.abarca.tallermecanico.data.repository.AgendaRepository
import com.abarca.tallermecanico.databinding.FragmentAgendaDetalleBinding
import com.abarca.tallermecanico.util.FechaAgenda
import com.abarca.tallermecanico.util.SincronizadorAgenda
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class AgendaDetalleFragment : Fragment() {

    private var _binding: FragmentAgendaDetalleBinding? = null
    private val binding get() = _binding!!
    private lateinit var tipo: TipoAgenda
    private var eventoId = -1
    private var evento: EventoAgenda? = null
    private var ocupado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tipo = runCatching { TipoAgenda.valueOf(arguments?.getString("tipo").orEmpty()) }.getOrDefault(TipoAgenda.CITA)
        eventoId = arguments?.getInt("eventoId", -1) ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgendaDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AgendaRepository.eventos.observe(viewLifecycleOwner) {
            evento = AgendaRepository.getById(tipo, eventoId)
            mostrar()
        }
        binding.btnReprogramar.setOnClickListener { reprogramar() }
        binding.btnRealizada.setOnClickListener { marcarRealizada() }
        binding.btnCancelarCita.setOnClickListener { confirmarCancelacion() }
        binding.btnReintentar.setOnClickListener { evento?.let { sincronizar(it) } }
    }

    private fun mostrar() {
        val e = evento
        if (e == null) {
            findNavController().popBackStack()
            return
        }
        val ctx = requireContext()
        binding.tvTitulo.text = e.titulo
        binding.tvEstado.text = e.estado.display.uppercase()
        binding.tvFecha.text = getString(
            R.string.label_agenda_duracion, FechaAgenda.fechaHora(e.inicio), e.duracionMin
        )
        binding.tvCliente.text = getString(R.string.label_agenda_cliente, e.clienteNombre, e.clienteEmail)
        binding.tvVehiculo.text = getString(R.string.label_agenda_vehiculo, e.vehiculoPatente, e.vehiculoDescripcion)
        binding.tvMecanico.visibility = if (e.mecanicoNombre.isBlank()) View.GONE else View.VISIBLE
        binding.tvMecanico.text = getString(R.string.label_agenda_mecanico, e.mecanicoNombre)

        val sincronizada = !e.requiereSincronizar
        binding.tvSync.text = when {
            e.syncError.isNotBlank() -> getString(R.string.cal_sync_error, e.syncError)
            sincronizada -> getString(R.string.cal_sync_ok)
            else -> getString(R.string.cal_sync_pendiente)
        }
        binding.tvSync.setTextColor(
            ContextCompat.getColor(ctx, if (sincronizada) R.color.agenda_ok else R.color.agenda_alerta)
        )

        val activa = e.activa
        binding.btnReprogramar.visibility = if (activa) View.VISIBLE else View.GONE
        binding.btnRealizada.visibility = if (activa) View.VISIBLE else View.GONE
        binding.btnCancelarCita.visibility = if (activa) View.VISIBLE else View.GONE
        binding.btnReintentar.visibility = if (e.requiereSincronizar) View.VISIBLE else View.GONE
        actualizarOcupado()
    }

    private fun actualizarOcupado() {
        binding.progress.visibility = if (ocupado) View.VISIBLE else View.GONE
        listOf(binding.btnReprogramar, binding.btnRealizada, binding.btnCancelarCita, binding.btnReintentar)
            .forEach { it.isEnabled = !ocupado }
    }

    private fun reprogramar() {
        val e = evento ?: return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.btn_reprogramar)
            .setSelection(FechaAgenda.aSelectorUtc(e.inicio))
            .setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
            .build()
        picker.addOnPositiveButtonClickListener { fechaUtc ->
            val hora = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(FechaAgenda.horaDe(e.inicio))
                .setMinute(FechaAgenda.minutoDe(e.inicio))
                .setTitleText(R.string.hint_hora)
                .build()
            hora.addOnPositiveButtonClickListener {
                val nuevoInicio = FechaAgenda.combinar(fechaUtc, hora.hour, hora.minute)
                if (nuevoInicio <= System.currentTimeMillis()) {
                    Aviso.mostrar(requireContext(), R.string.error_fecha_pasada)
                    return@addOnPositiveButtonClickListener
                }
                if (e.mecanicoNombre.isNotBlank()) {
                    AgendaRepository.conflictoMecanico(
                        e.mecanicoNombre, e.mecanicoEmail, nuevoInicio, nuevoInicio + e.duracionMin * 60_000L, e
                    )?.let {
                        Aviso.mostrar(
                            requireContext(),
                            getString(
                                R.string.error_mecanico_ocupado,
                                e.mecanicoNombre,
                                FechaAgenda.fechaHora(it.inicio),
                                FechaAgenda.fechaHora(it.fin),
                                it.vehiculoPatente
                            )
                        )
                        return@addOnPositiveButtonClickListener
                    }
                }
                sincronizar(
                    e.copy(
                        inicio = nuevoInicio,
                        estado = EstadoAgenda.REPROGRAMADA,
                        actualizadoEn = System.currentTimeMillis()
                    ),
                    guardarAntes = true
                )
            }
            hora.show(childFragmentManager, "hora_reprogramar")
        }
        picker.show(childFragmentManager, "fecha_reprogramar")
    }

    private fun marcarRealizada() {
        val e = evento ?: return
        AgendaRepository.guardar(e.copy(estado = EstadoAgenda.REALIZADA, actualizadoEn = System.currentTimeMillis()))
    }

    private fun confirmarCancelacion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_cancelar_cita_title)
            .setMessage(R.string.confirm_cancelar_cita_message)
            .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                val e = evento ?: return@setPositiveButton
                sincronizar(
                    e.copy(estado = EstadoAgenda.CANCELADA, actualizadoEn = System.currentTimeMillis()),
                    guardarAntes = true
                )
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun sincronizar(nuevo: EventoAgenda, guardarAntes: Boolean = false) {
        val ctx = requireContext().applicationContext
        if (guardarAntes) AgendaRepository.guardar(nuevo)
        ocupado = true
        actualizarOcupado()
        SincronizadorAgenda.sincronizar(ctx, nuevo) { resultado ->
            val mensaje = if (resultado.syncError.isBlank()) {
                ctx.getString(R.string.cal_msg_sincronizada)
            } else {
                ctx.getString(R.string.cal_msg_guardada_sin_sync, resultado.syncError)
            }
            Aviso.mostrar(ctx, mensaje)
            ocupado = false
            if (_binding != null) actualizarOcupado()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
