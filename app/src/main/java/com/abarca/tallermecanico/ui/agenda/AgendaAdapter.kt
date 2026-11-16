package com.abarca.tallermecanico.ui.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EventoAgenda
import com.abarca.tallermecanico.databinding.ItemAgendaBinding
import com.abarca.tallermecanico.util.FechaAgenda

class AgendaAdapter(
    private val onItemClick: (EventoAgenda) -> Unit
) : ListAdapter<EventoAgenda, AgendaAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemAgendaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(evento: EventoAgenda) {
            val ctx = binding.root.context
            binding.tvFecha.text = FechaAgenda.fechaHora(evento.inicio)
            binding.tvEstado.text = evento.estado.display.uppercase()
            binding.tvTitulo.text = "${evento.tipo.etiqueta} · ${evento.detalle}"
            binding.tvDetalle.text = "${evento.vehiculoPatente} · ${evento.clienteNombre}"
            val sincronizada = !evento.requiereSincronizar
            binding.tvSync.text = when {
                evento.syncError.isNotBlank() -> ctx.getString(R.string.cal_sync_error, evento.syncError)
                sincronizada -> ctx.getString(R.string.cal_sync_ok)
                else -> ctx.getString(R.string.cal_sync_pendiente)
            }
            binding.tvSync.setTextColor(
                ContextCompat.getColor(ctx, if (sincronizada) R.color.agenda_ok else R.color.agenda_alerta)
            )
            binding.root.setOnClickListener { onItemClick(evento) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAgendaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<EventoAgenda>() {
        override fun areItemsTheSame(old: EventoAgenda, new: EventoAgenda) = old.clave == new.clave
        override fun areContentsTheSame(old: EventoAgenda, new: EventoAgenda) = old == new
    }
}
