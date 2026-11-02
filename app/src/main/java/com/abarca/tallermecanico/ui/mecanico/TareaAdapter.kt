package com.abarca.tallermecanico.ui.mecanico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Tarea
import com.abarca.tallermecanico.util.mostrarTexto
import com.abarca.tallermecanico.databinding.ItemTareaBinding

data class TareaItem(val tarea: Tarea, val bloqueada: Boolean)

class TareaAdapter(
    private val editable: Boolean,
    private val onToggle: (Tarea, Boolean) -> Unit
) : ListAdapter<TareaItem, TareaAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TareaItem) {
            val tarea = item.tarea
            binding.checkboxTarea.setOnCheckedChangeListener(null)
            binding.checkboxTarea.mostrarTexto(tarea.descripcion) { descripcion ->
                if (item.bloqueada) {
                    binding.root.context.getString(R.string.label_tarea_adicional_pendiente, descripcion)
                } else descripcion
            }
            binding.checkboxTarea.isChecked = tarea.completada
            binding.checkboxTarea.isEnabled = editable && !item.bloqueada
            binding.checkboxTarea.setOnCheckedChangeListener { _, checked -> onToggle(tarea, checked) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTareaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<TareaItem>() {
        override fun areItemsTheSame(old: TareaItem, new: TareaItem) = old.tarea.id == new.tarea.id
        override fun areContentsTheSame(old: TareaItem, new: TareaItem) = old == new
    }
}
