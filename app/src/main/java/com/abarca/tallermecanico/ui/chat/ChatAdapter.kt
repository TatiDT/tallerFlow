package com.abarca.tallermecanico.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.databinding.ItemChatMensajeBinding
import com.abarca.tallermecanico.util.mostrarTexto

class ChatAdapter : ListAdapter<ChatMensaje, ChatAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemChatMensajeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mensaje: ChatMensaje) {
            val contexto = binding.root.context
            if (mensaje.esUsuario) binding.tvMensaje.text = mensaje.texto else binding.tvMensaje.mostrarTexto(mensaje.texto)
            binding.tvMensaje.setBackgroundResource(
                if (mensaje.esUsuario) R.drawable.bg_chat_usuario else R.drawable.bg_chat_asistente
            )
            binding.tvMensaje.setTextColor(
                ContextCompat.getColor(contexto, if (mensaje.esUsuario) R.color.tf_bg_dark else R.color.tf_text_primary)
            )
            val params = binding.tvMensaje.layoutParams as FrameLayout.LayoutParams
            params.gravity = if (mensaje.esUsuario) Gravity.END else Gravity.START
            binding.tvMensaje.layoutParams = params
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemChatMensajeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<ChatMensaje>() {
        override fun areItemsTheSame(old: ChatMensaje, new: ChatMensaje) = old.id == new.id
        override fun areContentsTheSame(old: ChatMensaje, new: ChatMensaje) = old == new
    }
}
