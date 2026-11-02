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
import com.abarca.tallermecanico.TallerApp
import com.abarca.tallermecanico.databinding.FragmentRepuestoBinding
import com.abarca.tallermecanico.ui.viewmodel.RepuestoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.FirestoreOps
import com.google.android.material.snackbar.Snackbar

class RepuestoFragment : Fragment() {

    private var _binding: FragmentRepuestoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RepuestoViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRepuestoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RepuestoAdapter { repuesto ->
            val bundle = Bundle().apply { putInt("entityId", repuesto.id) }
            findNavController().navigate(R.id.action_repuesto_to_form, bundle)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        var listadoRegistrado = false
        var avisoError: Snackbar? = null
        val actualizar = {
            val cargando = viewModel.cargando.value != false
            val repuestos = viewModel.repuestos.value.orEmpty()
            val error = viewModel.error.value
            adapter.submitList(repuestos)
            binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
            if (error != null && !cargando) {
                val mensaje = FirestoreOps.mensajeError(requireContext(), error)
                binding.tvEmpty.text = mensaje
                binding.tvEmpty.visibility = if (repuestos.isEmpty()) View.VISIBLE else View.GONE
                if (avisoError == null) {
                    avisoError = Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.reintentar) {
                            avisoError = null
                            (requireContext().applicationContext as TallerApp).reintentar()
                        }
                    avisoError?.show()
                }
            } else {
                avisoError?.dismiss()
                avisoError = null
                binding.tvEmpty.setText(R.string.empty_repuestos)
                binding.tvEmpty.visibility = if (!cargando && repuestos.isEmpty()) View.VISIBLE else View.GONE
            }
            if (!cargando && error == null && !listadoRegistrado) {
                listadoRegistrado = true
                AnalyticsLogger.logListadoConsultado(requireContext(), "repuesto")
            }
        }
        viewModel.repuestos.observe(viewLifecycleOwner) { actualizar() }
        viewModel.cargando.observe(viewLifecycleOwner) { actualizar() }
        viewModel.error.observe(viewLifecycleOwner) { actualizar() }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_agregar, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_agregar) {
            findNavController().navigate(R.id.action_repuesto_to_form)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
