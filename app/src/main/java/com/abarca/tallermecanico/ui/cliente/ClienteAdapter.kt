package com.abarca.tallermecanico.ui.cliente

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.databinding.ItemClienteBinding

class ClienteAdapter(
    private val onItemClick: (Cliente) -> Unit
) : ListAdapter<Cliente, ClienteAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemClienteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cliente: Cliente) {
            binding.tvNombreCompleto.text = "${cliente.nombre} ${cliente.apellido}"
            binding.tvRut.text = "RUT: ${cliente.rut}"
            binding.tvTelefono.text = cliente.telefono
            binding.root.setOnClickListener { onItemClick(cliente) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Cliente>() {
        override fun areItemsTheSame(old: Cliente, new: Cliente) = old.id == new.id
        override fun areContentsTheSame(old: Cliente, new: Cliente) = old == new
    }
}
