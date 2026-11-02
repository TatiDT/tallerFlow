package com.abarca.tallermecanico.ui.mecanico

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.util.colorRes
import com.abarca.tallermecanico.databinding.ItemOrdenBinding

class OrdenAdapter(
    private val onItemClick: (OrdenActiva) -> Unit
) : ListAdapter<OrdenActiva, OrdenAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemOrdenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrdenActiva) {
            val v = item.vehiculo
            val c = item.cliente
            binding.tvPatente.text = v.patente
            binding.tvMarcaModelo.text = "${v.marca} ${v.modelo} · ${v.año}"
            binding.tvClienteNombre.text = "Cliente: ${c.nombre} ${c.apellido}"
            binding.tvEstadoBadge.text = v.estado.display
            binding.progressTareas.progress = item.progreso
            binding.tvProgresoLabel.text = binding.root.context.getString(R.string.label_progreso, item.progreso)

            val colorInt = ContextCompat.getColor(binding.root.context, v.estado.colorRes())
            binding.tvEstadoBadge.backgroundTintList = ColorStateList.valueOf(colorInt)
            binding.viewEstadoBar.setBackgroundColor(colorInt)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemOrdenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<OrdenActiva>() {
        override fun areItemsTheSame(old: OrdenActiva, new: OrdenActiva) =
            old.vehiculo.id == new.vehiculo.id
        override fun areContentsTheSame(old: OrdenActiva, new: OrdenActiva) =
            old == new
    }
}
