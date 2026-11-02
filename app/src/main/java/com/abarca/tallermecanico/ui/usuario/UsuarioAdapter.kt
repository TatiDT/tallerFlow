package com.abarca.tallermecanico.ui.usuario

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.databinding.ItemUsuarioBinding

class UsuarioAdapter(
    private val onItemClick: (Usuario) -> Unit
) : ListAdapter<Usuario, UsuarioAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemUsuarioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: Usuario) {
            binding.tvNombreCompleto.text = "${usuario.nombre} ${usuario.apellido}"
            binding.tvUsername.text = "@${usuario.username}"
            binding.tvRolBadge.text = usuario.rol.display

            val rolColor = when (usuario.rol) {
                RolUsuario.ADMIN -> R.color.rol_admin
                RolUsuario.JEFE_TALLER -> R.color.rol_jefe_taller
                RolUsuario.MECANICO -> R.color.rol_mecanico
                RolUsuario.REPUESTO -> R.color.rol_repuesto
                RolUsuario.DIAGNOSIS -> R.color.rol_diagnosis
            }
            val colorInt = ContextCompat.getColor(binding.root.context, rolColor)
            binding.tvRolBadge.backgroundTintList = ColorStateList.valueOf(colorInt)
            binding.viewRolBar.setBackgroundColor(colorInt)

            val ctx = binding.root.context
            binding.tvEstadoActivo.text = if (usuario.activo)
                ctx.getString(R.string.label_activo)
            else
                ctx.getString(R.string.label_inactivo)
            binding.tvEstadoActivo.setTextColor(
                ContextCompat.getColor(ctx, if (usuario.activo) R.color.estado_listo else R.color.estado_entregado)
            )

            binding.root.setOnClickListener { onItemClick(usuario) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Usuario>() {
        override fun areItemsTheSame(old: Usuario, new: Usuario) = old.id == new.id
        override fun areContentsTheSame(old: Usuario, new: Usuario) = old == new
    }
}
