package com.abarca.tallermecanico.ui.mecanico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.data.entity.Falla
import com.abarca.tallermecanico.util.mostrarTexto
import com.abarca.tallermecanico.databinding.ItemFallaBinding

class FallaAdapter(
    private val onItemClick: (Falla) -> Unit
) : ListAdapter<Falla, FallaAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemFallaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(falla: Falla, numero: Int) {
            binding.tvNumeroFalla.text = numero.toString()
            binding.tvDescripcionFalla.mostrarTexto(falla.descripcion)
            binding.tvFechaFalla.text = falla.fecha
            binding.root.setOnClickListener { onItemClick(falla) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemFallaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position), position + 1)

    class DiffCallback : DiffUtil.ItemCallback<Falla>() {
        override fun areItemsTheSame(old: Falla, new: Falla) = old.id == new.id
        override fun areContentsTheSame(old: Falla, new: Falla) = old == new
    }
}
