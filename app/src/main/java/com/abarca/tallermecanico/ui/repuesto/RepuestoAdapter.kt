package com.abarca.tallermecanico.ui.repuesto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Repuesto
import com.abarca.tallermecanico.util.mostrarTexto
import com.abarca.tallermecanico.databinding.ItemRepuestoBinding
import com.abarca.tallermecanico.util.Formato

class RepuestoAdapter(
    private val onItemClick: (Repuesto) -> Unit
) : ListAdapter<Repuesto, RepuestoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemRepuestoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(repuesto: Repuesto) {
            binding.tvNombreRepuesto.mostrarTexto(repuesto.nombre)
            binding.tvCodigoRepuesto.text = "Código: ${repuesto.codigo}"
            binding.tvDetalleRepuesto.mostrarTexto(repuesto.categoria) { categoria ->
                "$categoria · ${Formato.pesos(repuesto.precio.toDouble())}"
            }
            binding.tvStockBadge.text = binding.root.context.getString(R.string.label_stock, repuesto.stock)
            binding.root.setOnClickListener { onItemClick(repuesto) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRepuestoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Repuesto>() {
        override fun areItemsTheSame(old: Repuesto, new: Repuesto) = old.id == new.id
        override fun areContentsTheSame(old: Repuesto, new: Repuesto) = old == new
    }
}
