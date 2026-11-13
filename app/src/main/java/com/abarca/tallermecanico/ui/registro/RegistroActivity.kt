package com.abarca.tallermecanico.ui.registro

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.TallerApp
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.repository.TallerRepository
import com.abarca.tallermecanico.databinding.ActivityRegistroBinding
import com.abarca.tallermecanico.ui.home.HomeActivity
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.RecordarSesion
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.content) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, barras.top, v.paddingRight, barras.bottom)
            insets
        }

        binding.btnCrear.setOnClickListener { registrar() }
        binding.tvVolver.setOnClickListener { finish() }
    }

    private fun limpiarErrores() {
        listOf(
            binding.tilTaller, binding.tilNombre, binding.tilApellido,
            binding.tilCorreo, binding.tilPassword, binding.tilConfirmar
        ).forEach { it.error = null }
    }

    private fun marcar(campo: TextInputLayout, mensaje: Int): Boolean {
        campo.error = getString(mensaje)
        return false
    }

    private fun registrar() {
        limpiarErrores()
        val taller = binding.etTaller.text?.toString()?.trim().orEmpty()
        val nombre = binding.etNombre.text?.toString()?.trim().orEmpty()
        val apellido = binding.etApellido.text?.toString()?.trim().orEmpty()
        val correo = binding.etCorreo.text?.toString()?.trim()?.lowercase().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmar = binding.etConfirmar.text?.toString().orEmpty()

        var valido = true
        if (taller.isEmpty()) valido = marcar(binding.tilTaller, R.string.error_taller_requerido)
        if (nombre.isEmpty()) valido = marcar(binding.tilNombre, R.string.error_nombre_requerido)
        if (apellido.isEmpty()) valido = marcar(binding.tilApellido, R.string.error_apellido_requerido)
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) valido = marcar(binding.tilCorreo, R.string.error_correo_invalido)
        if (password.length < 6) valido = marcar(binding.tilPassword, R.string.error_password_short)
        else if (password != confirmar) valido = marcar(binding.tilConfirmar, R.string.error_clave_no_coincide)
        if (!valido) return

        enCurso(true)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(correo, password)
            .addOnSuccessListener { resultado ->
                val cuenta = resultado.user
                if (cuenta == null) {
                    fallo(binding.tilCorreo, R.string.error_registro)
                    return@addOnSuccessListener
                }
                val admin = Usuario(
                    id = 1,
                    nombre = nombre,
                    apellido = apellido,
                    username = correo,
                    email = correo,
                    password = "",
                    rol = RolUsuario.ADMIN
                )
                TallerRepository.crear(cuenta.uid, taller, admin) { codigo, error ->
                    if (codigo == null || error != null) {
                        cuenta.delete()
                        fallo(binding.tilCorreo, R.string.error_registro)
                    } else {
                        entrar(cuenta.uid, taller, codigo, admin)
                    }
                }
            }
            .addOnFailureListener {
                when (it) {
                    is FirebaseAuthUserCollisionException -> fallo(binding.tilCorreo, R.string.error_correo_en_uso)
                    is FirebaseNetworkException -> fallo(binding.tilCorreo, R.string.error_taller_sin_conexion)
                    else -> fallo(binding.tilCorreo, R.string.error_registro)
                }
            }
    }

    private fun fallo(campo: TextInputLayout, mensaje: Int) {
        if (isFinishing || isDestroyed) return
        campo.error = getString(mensaje)
        enCurso(false)
    }

    private fun enCurso(activo: Boolean) {
        binding.btnCrear.isEnabled = !activo
        binding.tvVolver.isEnabled = !activo
    }

    private fun entrar(idTaller: String, nombreTaller: String, codigo: String, admin: Usuario) {
        if (isFinishing || isDestroyed) return
        TallerApp.instancia.abrirTaller(idTaller, nombreTaller, codigo)
        Sesion.usuarioActual = admin
        RecordarSesion.guardar(admin)
        AnalyticsLogger.logLoginExitoso(this, admin.rol)
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
