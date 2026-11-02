package com.abarca.tallermecanico.ui.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Diagnostico
import com.abarca.tallermecanico.databinding.FragmentDiagnosticosBinding
import com.abarca.tallermecanico.ui.presupuesto.PresupuestoDialog
import com.abarca.tallermecanico.ui.viewmodel.DiagnosticoViewModel
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.util.Formato
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticosFragment : Fragment() {

    private var _binding: FragmentDiagnosticosBinding? = null
    private val binding get() = _binding!!
    private val diagnosticoViewModel: DiagnosticoViewModel by activityViewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val presupuestoViewModel: PresupuestoViewModel by activityViewModels()
    private var vehiculoId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehiculoId = arguments?.getInt("vehiculoId") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiagnosticosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehiculoViewModel.getById(vehiculoId)?.let { v ->
            requireActivity().title = "${v.patente} · ${v.marca} ${v.modelo}"
        }

        presupuestoViewModel.presupuestos.observe(viewLifecycleOwner) {
            val presupuesto = presupuestoViewModel.getByVehiculoId(vehiculoId)
            binding.btnPresupuesto.text = when {
                presupuesto == null -> getString(R.string.btn_presupuesto_vacio)
                presupuesto.aprobado -> getString(R.string.btn_presupuesto_aprobado, Formato.pesos(presupuesto.total))
                else -> getString(R.string.btn_presupuesto_pendiente, Formato.pesos(presupuesto.total))
            }
        }
        binding.btnPresupuesto.setOnClickListener {
            PresupuestoDialog(this, presupuestoViewModel, vehiculoId, PresupuestoDialog.Modo.AGREGAR).show()
        }

        val adapter = DiagnosticoAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        diagnosticoViewModel.diagnosticos.observe(viewLifecycleOwner) { todos ->
            val propios = todos.filter { it.vehiculoId == vehiculoId }
            adapter.submitList(propios)
            binding.tvEmpty.visibility = if (propios.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnAgregarDiagnostico.setOnClickListener {
            val descripcion = binding.etDiagnostico.text?.toString()?.trim() ?: ""
            if (descripcion.isNotEmpty()) {
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                if (diagnosticoViewModel.insert(Diagnostico(vehiculoId = vehiculoId, descripcion = descripcion, fecha = fecha))) {
                    binding.tilDiagnostico.error = null
                    binding.etDiagnostico.text?.clear()
                } else {
                    binding.tilDiagnostico.error = getString(R.string.error_hallazgo_duplicado)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
