package com.abarca.tallermecanico.ui.mecanico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.databinding.FragmentFichaActivaBinding
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.ui.viewmodel.TareaViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.FirestoreOps
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator

class FichaActivaFragment : Fragment() {

    private var _binding: FragmentFichaActivaBinding? = null
    private val binding get() = _binding!!
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val tareaViewModel: TareaViewModel by activityViewModels()
    private val presupuestoViewModel: PresupuestoViewModel by activityViewModels()

    private var vehiculoId = -1
    private var currentVehiculo: Vehiculo? = null
    private var ocupado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        vehiculoId = arguments?.getInt("entityId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFichaActivaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentVehiculo = vehiculoViewModel.getById(vehiculoId)
        requireActivity().title = currentVehiculo?.let { "${it.patente} · ${it.marca} ${it.modelo}" }
            ?: getString(R.string.title_ficha_activa)

        binding.viewPager.adapter = FichaActivaPagerAdapter(this, vehiculoId)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_tareas)
                1 -> getString(R.string.tab_costos)
                else -> getString(R.string.tab_inspeccion)
            }
        }.attach()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_ficha_activa, menu)
        // Solo se puede terminar lo que está en reparación; Terminado espera la revisión del Jefe
        menu.findItem(R.id.action_dar_alta)?.isVisible =
            vehiculoViewModel.getById(vehiculoId)?.estado == EstadoVehiculo.EN_REPARACION
        menu.findItem(R.id.action_dar_alta)?.isEnabled = !ocupado
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_dar_alta) {
            if (!ocupado) confirmarDarAlta()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmarDarAlta() {
        val tareas = tareaViewModel.getByVehiculoId(vehiculoId)
        if (tareas.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.error_sin_tareas)
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
            return
        }
        if (presupuestoViewModel.tareasBloqueadas(vehiculoId).isNotEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_tareas_pendientes)
                .setMessage(R.string.msg_adicionales_pendientes)
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
            return
        }
        if (tareas.any { !it.completada }) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_tareas_pendientes)
                .setMessage(R.string.msg_tareas_pendientes)
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_dar_alta_title)
            .setMessage(R.string.confirm_dar_alta_message)
            .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                val v = currentVehiculo ?: return@setPositiveButton
                val ctx = requireContext().applicationContext
                mostrarCarga(true)
                vehiculoViewModel.update(v.copy(estado = EstadoVehiculo.TERMINADO)) { error ->
                    if (error == null) {
                        AnalyticsLogger.logCrud(ctx, "vehiculo", "terminar")
                        Toast.makeText(ctx, R.string.msg_guardado_ok, Toast.LENGTH_SHORT).show()
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

    private fun mostrarCarga(cargando: Boolean) {
        ocupado = cargando
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        activity?.invalidateOptionsMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
