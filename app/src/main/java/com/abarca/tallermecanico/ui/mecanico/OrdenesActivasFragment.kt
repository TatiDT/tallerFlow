package com.abarca.tallermecanico.ui.mecanico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.databinding.FragmentOrdenesActivasBinding
import com.abarca.tallermecanico.ui.viewmodel.ClienteViewModel
import com.abarca.tallermecanico.ui.viewmodel.TareaViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AvisoReintento
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Sesion

class OrdenesActivasFragment : Fragment() {

    private var _binding: FragmentOrdenesActivasBinding? = null
    private val binding get() = _binding!!
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val clienteViewModel: ClienteViewModel by activityViewModels()
    private val tareaViewModel: TareaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrdenesActivasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OrdenAdapter { item ->
            val bundle = Bundle().apply { putInt("entityId", item.vehiculo.id) }
            findNavController().navigate(R.id.action_ordenes_to_ficha, bundle)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val aviso = AvisoReintento(this)

        fun combinar() {
            val mecanicoId = Sesion.usuarioActual?.id ?: -1
            val clienteMap = clienteViewModel.clientes.value.orEmpty().associateBy { it.id }
            val combinados = vehiculoViewModel.getActivosPorMecanico(mecanicoId).mapNotNull { v ->
                clienteMap[v.clienteId]?.let { c -> OrdenActiva(v, c, tareaViewModel.progreso(v.id)) }
            }
            adapter.submitList(combinados)
            val cargando = vehiculoViewModel.cargando.value != false || clienteViewModel.cargando.value != false
            val error = vehiculoViewModel.error.value ?: clienteViewModel.error.value
            binding.progress.visibility = if (cargando && error == null) View.VISIBLE else View.GONE
            if (error != null) {
                val mensaje = FirestoreOps.mensajeError(requireContext(), error)
                binding.tvEmpty.text = mensaje
                binding.tvEmpty.visibility = if (combinados.isEmpty()) View.VISIBLE else View.GONE
                aviso.mostrar(mensaje)
            } else {
                aviso.ocultar()
                binding.tvEmpty.setText(R.string.empty_ordenes)
                binding.tvEmpty.visibility = if (!cargando && combinados.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        vehiculoViewModel.vehiculos.observe(viewLifecycleOwner) { combinar() }
        vehiculoViewModel.cargando.observe(viewLifecycleOwner) { combinar() }
        vehiculoViewModel.error.observe(viewLifecycleOwner) { combinar() }
        clienteViewModel.cargando.observe(viewLifecycleOwner) { combinar() }
        clienteViewModel.error.observe(viewLifecycleOwner) { combinar() }
        clienteViewModel.clientes.observe(viewLifecycleOwner) { combinar() }
        tareaViewModel.tareas.observe(viewLifecycleOwner) { combinar() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
