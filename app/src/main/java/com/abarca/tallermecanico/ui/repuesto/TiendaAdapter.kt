package com.abarca.tallermecanico.ui.repuesto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.repository.TiendaCercana
import com.abarca.tallermecanico.databinding.ItemTiendaBinding

class TiendaAdapter(
    private val alTocar: (TiendaCercana) -> Unit,
    private val alComoLlegar: (TiendaCercana) -> Unit,
    private val alLlamar: (String) -> Unit,
    private val alAbrirSitio: (String) -> Unit
) : ListAdapter<TiendaCercana, TiendaAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemTiendaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tienda: TiendaCercana) {
            val contexto = binding.root.context
            binding.tvNombreTienda.text = tienda.nombre
            binding.tvDistanciaTienda.text = formatearDistancia(tienda.distanciaMetros, contexto)
            binding.tvDireccionTienda.text = tienda.direccion ?: contexto.getString(R.string.direccion_no_registrada)
            binding.tvHorarioTienda.visibility = if (tienda.horario != null) View.VISIBLE else View.GONE
            binding.tvHorarioTienda.text = tienda.horario?.let { contexto.getString(R.string.label_horario_tienda, it) }
            binding.btnLlamar.visibility = if (tienda.telefono != null) View.VISIBLE else View.GONE
            binding.btnSitioWeb.visibility = if (tienda.sitioWeb != null) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { alTocar(tienda) }
            binding.btnComoLlegar.setOnClickListener { alComoLlegar(tienda) }
            binding.btnLlamar.setOnClickListener { tienda.telefono?.let(alLlamar) }
            binding.btnSitioWeb.setOnClickListener { tienda.sitioWeb?.let(alAbrirSitio) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTiendaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<TiendaCercana>() {
        override fun areItemsTheSame(old: TiendaCercana, new: TiendaCercana) = old.id == new.id
        override fun areContentsTheSame(old: TiendaCercana, new: TiendaCercana) = old == new
    }

    companion object {
        fun formatearDistancia(metros: Double, contexto: android.content.Context): String =
            if (metros < 1000) contexto.getString(R.string.distancia_metros, metros.toInt())
            else contexto.getString(R.string.distancia_km, metros / 1000)
    }
}
