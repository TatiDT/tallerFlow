package com.abarca.tallermecanico.ui.repuesto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.FormatoBusqueda
import com.abarca.tallermecanico.data.entity.TiendaOnline
import com.abarca.tallermecanico.databinding.ItemTiendaOnlineBinding

// Lista de administración de tiendas en línea (editar, activar/desactivar y eliminar).
class TiendaOnlineAdapter(
    private val alEditar: (TiendaOnline) -> Unit,
    private val alCambiarActiva: (TiendaOnline, Boolean) -> Unit,
    private val alEliminar: (TiendaOnline) -> Unit
) : ListAdapter<TiendaOnline, TiendaOnlineAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemTiendaOnlineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tienda: TiendaOnline) {
            val contexto = binding.root.context
            binding.tvNombreTienda.text = tienda.nombre
            binding.tvUrlTienda.text = tienda.urlBusqueda
            val formato = contexto.getString(
                if (tienda.formato == FormatoBusqueda.GUIONES) R.string.formato_guiones else R.string.formato_espacios
            )
            binding.tvFormatoTienda.text = contexto.getString(R.string.label_tienda_formato, formato, tienda.orden)

            // Se quita el listener antes de fijar el estado para no disparar un cambio al reciclar la fila.
            binding.switchActiva.setOnCheckedChangeListener(null)
            binding.switchActiva.isChecked = tienda.activa
            binding.switchActiva.setOnCheckedChangeListener { _, activa -> alCambiarActiva(tienda, activa) }

            binding.root.setOnClickListener { alEditar(tienda) }
            binding.btnEditarTienda.setOnClickListener { alEditar(tienda) }
            binding.btnEliminarTienda.setOnClickListener { alEliminar(tienda) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTiendaOnlineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<TiendaOnline>() {
        override fun areItemsTheSame(old: TiendaOnline, new: TiendaOnline) = old.id == new.id
        override fun areContentsTheSame(old: TiendaOnline, new: TiendaOnline) = old == new
    }
}
