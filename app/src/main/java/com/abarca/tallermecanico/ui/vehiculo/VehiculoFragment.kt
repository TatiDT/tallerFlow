package com.abarca.tallermecanico.ui.vehiculo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.VehiculoConCliente
import com.abarca.tallermecanico.databinding.FragmentVehiculoBinding
import com.abarca.tallermecanico.ui.presupuesto.PresupuestoDialog
import com.abarca.tallermecanico.ui.viewmodel.ClienteViewModel
import com.abarca.tallermecanico.ui.viewmodel.FallaViewModel
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.ui.viewmodel.UsuarioViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.abarca.tallermecanico.util.AvisoReintento
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Sesion

class VehiculoFragment : Fragment() {

    private var _binding: FragmentVehiculoBinding? = null
    private val binding get() = _binding!!
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val clienteViewModel: ClienteViewModel by activityViewModels()
    private val usuarioViewModel: UsuarioViewModel by activityViewModels()
    private val presupuestoViewModel: PresupuestoViewModel by activityViewModels()
    private val fallaViewModel: FallaViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehiculoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = VehiculoAdapter(
            onItemClick = { item ->
                val bundle = Bundle().apply { putInt("entityId", item.vehiculo.id) }
                findNavController().navigate(R.id.action_vehiculo_to_form, bundle)
            },
            onPresupuestoClick = { item -> mostrarDialogoPresupuesto(item) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val aviso = AvisoReintento(this)

        fun combinar() {
            val vehiculos = vehiculoViewModel.vehiculos.value.orEmpty()
            val clientes = clienteViewModel.clientes.value.orEmpty()
            val usuarios = usuarioViewModel.usuarios.value.orEmpty()
            val presupuestos = presupuestoViewModel.presupuestos.value.orEmpty()
            val clienteMap = clientes.associateBy { it.id }
            val usuarioMap = usuarios.associateBy { it.id }
            val presupuestoMap = presupuestos.associateBy { it.vehiculoId }
            val fallasMap = fallaViewModel.fallas.value.orEmpty().groupBy { it.vehiculoId }
            val combinados = vehiculos.mapNotNull { v ->
                clienteMap[v.clienteId]?.let { c ->
                    val presupuesto = presupuestoMap[v.id]
                    val porCotizar = if (presupuesto?.aprobado == true) 0 else {
                        val cotizadas = presupuesto?.tratamientos.orEmpty().mapNotNull { it.fallaId }.toSet()
                        fallasMap[v.id].orEmpty().count { it.id !in cotizadas }
                    }
                    VehiculoConCliente(
                        v, c, v.mecanicoId?.let(usuarioMap::get), presupuesto, porCotizar,
                        presupuesto?.adicionalesPendientes?.size ?: 0
                    )
                }
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
                binding.tvEmpty.setText(R.string.empty_vehiculos)
                binding.tvEmpty.visibility = if (!cargando && combinados.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        vehiculoViewModel.vehiculos.observe(viewLifecycleOwner) { combinar() }
        vehiculoViewModel.cargando.observe(viewLifecycleOwner) { combinar() }
        vehiculoViewModel.error.observe(viewLifecycleOwner) { combinar() }
        clienteViewModel.cargando.observe(viewLifecycleOwner) { combinar() }
        clienteViewModel.error.observe(viewLifecycleOwner) { combinar() }
        clienteViewModel.clientes.observe(viewLifecycleOwner) { combinar() }
        usuarioViewModel.usuarios.observe(viewLifecycleOwner) { combinar() }
        presupuestoViewModel.presupuestos.observe(viewLifecycleOwner) { combinar() }
        fallaViewModel.fallas.observe(viewLifecycleOwner) { combinar() }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // La recepción de vehículos es tarea de Admin; el Jefe de Taller no ingresa vehículos
        if (Sesion.usuarioActual?.rol != RolUsuario.JEFE_TALLER) {
            inflater.inflate(R.menu.menu_agregar, menu)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_agregar) {
            findNavController().navigate(R.id.action_vehiculo_to_form)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mostrarDialogoPresupuesto(item: VehiculoConCliente) {
        val modo = if (Sesion.usuarioActual?.rol == RolUsuario.ADMIN) PresupuestoDialog.Modo.EDITAR
        else PresupuestoDialog.Modo.VER
        if (modo == PresupuestoDialog.Modo.VER && item.presupuesto == null) return
        PresupuestoDialog(this, presupuestoViewModel, item.vehiculo.id, modo).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
