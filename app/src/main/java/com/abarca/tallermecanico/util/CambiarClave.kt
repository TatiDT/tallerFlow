package com.abarca.tallermecanico.util

import android.app.Activity
import android.content.DialogInterface
import android.widget.Toast
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.repository.UsuarioRepository
import com.abarca.tallermecanico.databinding.DialogCambiarClaveBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

fun Activity.mostrarCambiarClave() {
    val sesion = Sesion.usuarioActual ?: return
    val binding = DialogCambiarClaveBinding.inflate(layoutInflater)

    val dialogo = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.title_cambiar_clave)
        .setView(binding.root)
        .setPositiveButton(R.string.btn_guardar, null)
        .setNegativeButton(R.string.btn_cancelar, null)
        .create()

    dialogo.setOnShowListener {
        val botonGuardar = dialogo.getButton(DialogInterface.BUTTON_POSITIVE)
        botonGuardar.setOnClickListener {
            val usuario = UsuarioRepository.getById(sesion.id) ?: sesion
            val actual = binding.etClaveActual.text?.toString().orEmpty()
            val nueva = binding.etClaveNueva.text?.toString().orEmpty()
            val confirmar = binding.etClaveConfirmar.text?.toString().orEmpty()

            binding.tilClaveActual.error = null
            binding.tilClaveNueva.error = null
            binding.tilClaveConfirmar.error = null

            val error = when {
                actual.isEmpty() || (!usuario.usaCuentaCorreo && !PasswordUtils.verify(actual, usuario.password)) ->
                    binding.tilClaveActual to R.string.error_clave_actual
                nueva.length < 6 ->
                    binding.tilClaveNueva to R.string.error_password_short
                nueva == actual ->
                    binding.tilClaveNueva to R.string.error_clave_igual
                nueva != confirmar ->
                    binding.tilClaveConfirmar to R.string.error_clave_no_coincide
                else -> null
            }
            if (error != null) {
                error.first.error = getString(error.second)
                return@setOnClickListener
            }

            if (usuario.usaCuentaCorreo) {
                botonGuardar.isEnabled = false
                cambiarClaveCorreo(actual, nueva, binding, dialogo, botonGuardar)
            } else {
                val clave = PasswordUtils.nueva(nueva)
                val actualizado = usuario.copy(password = clave.almacenado)
                UsuarioRepository.update(actualizado, clave.verificador)
                terminarCambio(actualizado)
                dialogo.dismiss()
            }
        }
    }
    dialogo.show()
}

private fun Activity.cambiarClaveCorreo(
    actual: String,
    nueva: String,
    binding: DialogCambiarClaveBinding,
    dialogo: androidx.appcompat.app.AlertDialog,
    botonGuardar: android.widget.Button
) {
    val cuenta = FirebaseAuth.getInstance().currentUser
    val correo = cuenta?.email
    if (cuenta == null || correo == null || cuenta.isAnonymous) {
        binding.tilClaveActual.error = getString(R.string.error_clave_actual)
        botonGuardar.isEnabled = true
        return
    }
    cuenta.reauthenticate(EmailAuthProvider.getCredential(correo, actual))
        .addOnSuccessListener {
            cuenta.updatePassword(nueva)
                .addOnSuccessListener {
                    Sesion.usuarioActual?.let { terminarCambio(it) }
                    dialogo.dismiss()
                }
                .addOnFailureListener {
                    binding.tilClaveNueva.error = getString(R.string.error_cambio_clave)
                    botonGuardar.isEnabled = true
                }
        }
        .addOnFailureListener {
            if (it is FirebaseNetworkException) {
                binding.tilClaveActual.error = getString(R.string.error_taller_sin_conexion)
            } else {
                binding.tilClaveActual.error = getString(R.string.error_clave_actual)
            }
            botonGuardar.isEnabled = true
        }
}

private fun Activity.terminarCambio(usuario: Usuario) {
    Sesion.usuarioActual = usuario
    RecordarSesion.actualizarSiGuardada(usuario)
    Toast.makeText(this, R.string.msg_clave_cambiada, Toast.LENGTH_SHORT).show()
}
