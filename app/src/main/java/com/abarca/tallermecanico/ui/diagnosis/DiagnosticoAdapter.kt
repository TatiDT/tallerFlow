package com.abarca.tallermecanico.ui.diagnosis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.data.entity.Diagnostico
import com.abarca.tallermecanico.util.mostrarTexto
import com.abarca.tallermecanico.databinding.ItemDiagnosticoBinding

class DiagnosticoAdapter : ListAdapter<Diagnostico, DiagnosticoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemDiagnosticoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(diagnostico: Diagnostico) {
            binding.tvDescripcionDiagnostico.mostrarTexto(diagnostico.descripcion)
            binding.tvFechaDiagnostico.text = diagnostico.fecha
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemDiagnosticoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Diagnostico>() {
        override fun areItemsTheSame(old: Diagnostico, new: Diagnostico) = old.id == new.id
        override fun areContentsTheSame(old: Diagnostico, new: Diagnostico) = old == new
    }
}
