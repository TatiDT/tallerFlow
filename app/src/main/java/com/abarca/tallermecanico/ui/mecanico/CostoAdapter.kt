package com.abarca.tallermecanico.ui.mecanico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.data.entity.Costo
import com.abarca.tallermecanico.databinding.ItemCostoBinding
import java.text.NumberFormat
import java.util.Locale

class CostoAdapter(
    private val onItemClick: ((Costo) -> Unit)? = null
) : ListAdapter<Costo, CostoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemCostoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(costo: Costo) {
            binding.tvDescripcionCosto.text = costo.descripcion
            val monto = NumberFormat.getNumberInstance(Locale("es", "CL")).format(costo.monto)
            binding.tvMontoCosto.text = "$$monto"
            if (onItemClick != null) {
                binding.root.setOnClickListener { onItemClick.invoke(costo) }
            } else {
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemCostoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Costo>() {
        override fun areItemsTheSame(old: Costo, new: Costo) = old.id == new.id
        override fun areContentsTheSame(old: Costo, new: Costo) = old == new
    }
}
