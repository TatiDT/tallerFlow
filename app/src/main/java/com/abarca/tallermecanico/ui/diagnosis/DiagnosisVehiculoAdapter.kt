package com.abarca.tallermecanico.ui.diagnosis

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.util.colorRes
import com.abarca.tallermecanico.data.entity.VehiculoConCliente
import com.abarca.tallermecanico.databinding.ItemDiagnosisVehiculoBinding

class DiagnosisVehiculoAdapter(
    private val onItemClick: (VehiculoConCliente) -> Unit
) : ListAdapter<VehiculoConCliente, DiagnosisVehiculoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemDiagnosisVehiculoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VehiculoConCliente) {
            val v = item.vehiculo
            val c = item.cliente
            binding.tvPatente.text = v.patente
            binding.tvMarcaModelo.text = "${v.marca} ${v.modelo} · ${v.año}"
            binding.tvClienteNombre.text = "Cliente: ${c.nombre} ${c.apellido}"
            binding.tvEstadoBadge.text = v.estado.display

            val colorInt = ContextCompat.getColor(binding.root.context, v.estado.colorRes())
            binding.tvEstadoBadge.backgroundTintList = ColorStateList.valueOf(colorInt)
            binding.viewEstadoBar.setBackgroundColor(colorInt)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemDiagnosisVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<VehiculoConCliente>() {
        override fun areItemsTheSame(old: VehiculoConCliente, new: VehiculoConCliente) =
            old.vehiculo.id == new.vehiculo.id
        override fun areContentsTheSame(old: VehiculoConCliente, new: VehiculoConCliente) =
            old == new
    }
}
