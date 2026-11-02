package com.abarca.tallermecanico.ui.mecanico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Tarea
import com.abarca.tallermecanico.databinding.FragmentTareasBinding
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.ui.viewmodel.TareaViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.Sesion

class TareasFragment : Fragment() {

    private var _binding: FragmentTareasBinding? = null
    private val binding get() = _binding!!
    private val tareaViewModel: TareaViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val presupuestoViewModel: PresupuestoViewModel by activityViewModels()
    private var vehiculoId = -1
    private val soloLectura get() = Sesion.tieneRol(RolUsuario.MECANICO) &&
        vehiculoViewModel.getById(vehiculoId)?.estado != EstadoVehiculo.EN_REPARACION

    companion object {
        fun newInstance(vehiculoId: Int) = TareasFragment().apply {
            arguments = Bundle().apply { putInt("vehiculoId", vehiculoId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehiculoId = arguments?.getInt("vehiculoId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTareasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editable = !soloLectura
        (binding.tilTarea.parent as View).visibility = if (editable) View.VISIBLE else View.GONE
        val adapter = TareaAdapter(editable) { tarea, completada ->
            tareaViewModel.update(tarea.copy(completada = completada))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        fun refrescar() {
            val bloqueadas = presupuestoViewModel.tareasBloqueadas(vehiculoId)
            val propias = tareaViewModel.getByVehiculoId(vehiculoId).map { TareaItem(it, it.id in bloqueadas) }
            adapter.submitList(propias)
            binding.tvEmpty.visibility = if (propias.isEmpty()) View.VISIBLE else View.GONE
        }
        tareaViewModel.tareas.observe(viewLifecycleOwner) { refrescar() }
        presupuestoViewModel.presupuestos.observe(viewLifecycleOwner) { refrescar() }

        binding.btnAgregarTarea.setOnClickListener {
            val descripcion = binding.etTarea.text?.toString()?.trim() ?: ""
            if (descripcion.isEmpty()) return@setOnClickListener
            if (tareaViewModel.existe(vehiculoId, descripcion)) {
                binding.tilTarea.error = getString(R.string.error_tarea_duplicada)
                return@setOnClickListener
            }
            if (presupuestoViewModel.agregarAdicional(vehiculoId, descripcion)) {
                Toast.makeText(requireContext(), R.string.msg_adicional_enviado, Toast.LENGTH_LONG).show()
            } else {
                tareaViewModel.insert(Tarea(vehiculoId = vehiculoId, descripcion = descripcion))
            }
            binding.tilTarea.error = null
            binding.etTarea.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
