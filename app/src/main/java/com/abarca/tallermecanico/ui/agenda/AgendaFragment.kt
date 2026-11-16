package com.abarca.tallermecanico.ui.agenda

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.repository.AgendaRepository
import com.abarca.tallermecanico.databinding.FragmentAgendaBinding
import com.abarca.tallermecanico.util.CalendarioGoogle
import com.abarca.tallermecanico.util.Sesion
import com.abarca.tallermecanico.util.SincronizadorAgenda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!
    private val esAdmin get() = Sesion.tieneRol(RolUsuario.ADMIN)
    private var eventos: List<EventoAgenda> = emptyList()

    private val autorizacion = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { resultado ->
        val ctx = context?.applicationContext ?: return@registerForActivityResult
        if (resultado.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        runCatching { CalendarioGoogle.resultadoDeIntent(ctx, resultado.data) }
            .onSuccess {
                CalendarioGoogle.registrarConexion(ctx, it)
                Toast.makeText(ctx, R.string.cal_msg_conectada, Toast.LENGTH_SHORT).show()
                refrescarCabecera()
            }
            .onFailure { Toast.makeText(ctx, R.string.cal_error_autorizar, Toast.LENGTH_LONG).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AgendaAdapter { evento ->
            findNavController().navigate(
                R.id.action_agenda_to_detalle,
                bundleOf("tipo" to evento.tipo.name, "eventoId" to evento.id)
            )
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        AgendaRepository.eventos.observe(viewLifecycleOwner) {
            eventos = it
            adapter.submitList(it)
            actualizarVacio()
            refrescarCabecera()
        }
        AgendaRepository.cargando.observe(viewLifecycleOwner) { actualizarVacio() }

        binding.btnConectar.setOnClickListener { alPulsarConectar() }
        binding.btnSync.setOnClickListener { sincronizarPendientes() }
        refrescarCabecera()
    }

    private fun actualizarVacio() {
        val cargando = AgendaRepository.cargando.value != false
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility = if (!cargando && eventos.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refrescarCabecera() {
        if (_binding == null) return
        val ctx = requireContext()
        val conectado = CalendarioGoogle.conectado(ctx)
        val cuenta = CalendarioGoogle.cuenta(ctx)
        binding.tvCuenta.text = when {
            !conectado -> getString(R.string.cal_cuenta_desconectada)
            cuenta.isBlank() -> getString(R.string.cal_cuenta_conectada_sin_correo)
            else -> getString(R.string.cal_cuenta_conectada, cuenta)
        }
        binding.btnConectar.visibility = if (esAdmin) View.VISIBLE else View.GONE
        binding.btnConectar.setText(if (conectado) R.string.cal_btn_desconectar else R.string.cal_btn_conectar)

        val pendientes = eventos.count { it.requiereSincronizar }
        binding.btnSync.visibility = if (esAdmin && conectado && pendientes > 0) View.VISIBLE else View.GONE
        binding.btnSync.text = getString(R.string.cal_btn_sync_pendientes, pendientes)
    }

    private fun alPulsarConectar() {
        val ctx = requireContext().applicationContext
        if (CalendarioGoogle.conectado(ctx)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cal_btn_desconectar)
                .setMessage(R.string.cal_confirm_desconectar)
                .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        CalendarioGoogle.desconectar(ctx)
                        Toast.makeText(ctx, R.string.cal_msg_desconectada, Toast.LENGTH_SHORT).show()
                        refrescarCabecera()
                    }
                }
                .setNegativeButton(R.string.btn_cancelar, null)
                .show()
            return
        }
        binding.btnConectar.isEnabled = false
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val resultado = CalendarioGoogle.solicitarAutorizacion(ctx)
                val intencion = resultado.pendingIntent
                if (resultado.hasResolution() && intencion != null) {
                    if (_binding != null) {
                        autorizacion.launch(IntentSenderRequest.Builder(intencion.intentSender).build())
                    }
                } else {
                    CalendarioGoogle.registrarConexion(ctx, resultado)
                    Toast.makeText(ctx, R.string.cal_msg_conectada, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, R.string.cal_error_autorizar, Toast.LENGTH_LONG).show()
            }
            if (_binding != null) {
                binding.btnConectar.isEnabled = true
                refrescarCabecera()
            }
        }
    }

    private fun sincronizarPendientes() {
        val ctx = requireContext().applicationContext
        binding.btnSync.isEnabled = false
        SincronizadorAgenda.sincronizarPendientes(ctx) { exitosos, fallidos ->
            Toast.makeText(ctx, ctx.getString(R.string.cal_msg_sync_resultado, exitosos, fallidos), Toast.LENGTH_LONG).show()
            if (_binding != null) {
                binding.btnSync.isEnabled = true
                refrescarCabecera()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_agenda, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_nueva_cita) {
            findNavController().navigate(R.id.action_agenda_to_form, bundleOf("tipo" to "CITA"))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
