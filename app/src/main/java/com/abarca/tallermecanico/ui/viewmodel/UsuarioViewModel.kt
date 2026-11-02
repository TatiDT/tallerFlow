package com.abarca.tallermecanico.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.repository.UsuarioRepository

class UsuarioViewModel : ViewModel() {

    val usuarios: LiveData<List<Usuario>> = UsuarioRepository.usuarios

    fun insert(usuario: Usuario) = UsuarioRepository.insert(usuario)

    fun update(usuario: Usuario) = UsuarioRepository.update(usuario)

    fun delete(usuario: Usuario) = UsuarioRepository.delete(usuario)

    fun getById(id: Int): Usuario? = UsuarioRepository.getById(id)

    fun isEmailDuplicate(email: String, excludeId: Int = 0): Boolean =
        UsuarioRepository.isEmailDuplicate(email, excludeId)

    fun isUsernameDuplicate(username: String, excludeId: Int = 0): Boolean =
        UsuarioRepository.isUsernameDuplicate(username, excludeId)
}
