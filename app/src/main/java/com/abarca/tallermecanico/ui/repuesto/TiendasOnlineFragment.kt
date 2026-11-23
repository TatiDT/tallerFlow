package com.abarca.tallermecanico.ui.repuesto

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
import com.abarca.tallermecanico.data.entity.TiendaOnline
import com.abarca.tallermecanico.databinding.FragmentTiendasOnlineBinding
import com.abarca.tallermecanico.ui.viewmodel.TiendaOnlineViewModel
import com.abarca.tallermecanico.util.Aviso
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Administración de las tiendas en línea. Solo el jefe de taller llega aquí desde el menú de Repuestos.
class TiendasOnlineFragment : Fragment() {

    private var _binding: FragmentTiendasOnlineBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TiendaOnlineViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTiendasOnlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.title_tiendas_online)

        // Defensa extra: si por navegación llega alguien que no es jefe de taller, se vuelve atrás.
        if (!Sesion.tieneRol(RolUsuario.JEFE_TALLER)) {
            findNavController().popBackStack()
            return
        }

        val adapter = TiendaOnlineAdapter(
            alEditar = { editar(it) },
            alCambiarActiva = { tienda, activa -> cambiarActiva(tienda, activa) },
            alEliminar = { confirmarEliminar(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.tiendas.observe(viewLifecycleOwner) { tiendas ->
            adapter.submitList(tiendas)
            // Si el taller aún no guardó tiendas, se avisa que se muestran las predeterminadas.
            val usandoPredeterminadas = tiendas == TiendaOnline.PREDETERMINADAS
            binding.tvAvisoPredeterminadas.visibility = if (usandoPredeterminadas) View.VISIBLE else View.GONE
        }
    }

    private fun editar(tienda: TiendaOnline) {
        val bundle = Bundle().apply { putInt("entityId", tienda.id) }
        findNavController().navigate(R.id.action_tiendas_to_form, bundle)
    }

    private fun cambiarActiva(tienda: TiendaOnline, activa: Boolean) {
        if (tienda.activa == activa) return
        val contexto = requireContext().applicationContext
        viewModel.guardar(tienda.copy(activa = activa)) { error ->
            if (error != null) Aviso.mostrar(contexto, FirestoreOps.mensajeError(contexto, error))
        }
    }

    private fun confirmarEliminar(tienda: TiendaOnline) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_eliminar_tienda_title)
            .setMessage(getString(R.string.confirm_eliminar_tienda_message, tienda.nombre))
            .setPositiveButton(R.string.btn_confirmar) { _, _ ->
                val contexto = requireContext().applicationContext
                viewModel.eliminar(tienda) { error ->
                    if (error == null) Aviso.mostrar(contexto, R.string.msg_eliminado_ok)
                    else Aviso.mostrar(contexto, FirestoreOps.mensajeError(contexto, error))
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_tiendas_online, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_agregar_tienda) {
            findNavController().navigate(R.id.action_tiendas_to_form)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
