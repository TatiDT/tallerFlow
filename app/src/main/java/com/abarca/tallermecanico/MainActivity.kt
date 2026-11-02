package com.abarca.tallermecanico

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.repository.UsuarioRepository
import com.abarca.tallermecanico.databinding.ActivityMainBinding
import com.abarca.tallermecanico.ui.home.HomeActivity
import com.abarca.tallermecanico.ui.jefe.JefeTallerActivity
import com.abarca.tallermecanico.ui.operativo.OperativoActivity
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.AuthGoogle
import com.abarca.tallermecanico.util.FirestoreOps
import com.abarca.tallermecanico.util.Notificaciones
import com.abarca.tallermecanico.util.Sesion
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.content) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, barras.top, v.paddingRight, barras.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificaciones.tienePermiso(this)) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        binding.btnLogin.setOnClickListener { validateAndLogin() }
        binding.btnLoginGoogle.visibility = if (AuthGoogle.disponible) View.VISIBLE else View.GONE
        binding.btnLoginGoogle.setOnClickListener { iniciarConGoogle() }
        UsuarioRepository.cargando.observe(this) {
            binding.btnLogin.isEnabled = !it
            binding.btnLoginGoogle.isEnabled = !it
        }
        binding.etUsername.setOnFocusChangeListener { _, _ -> binding.tilUsername.error = null }
        binding.etPassword.setOnFocusChangeListener { _, _ -> binding.tilPassword.error = null }
    }

    private fun validateAndLogin() {  //LOGICA DEL INICIO DE SESION
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.error_username_required)
            isValid = false
        } else binding.tilUsername.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            isValid = false
        } else binding.tilPassword.error = null

        if (!isValid) return

        UsuarioRepository.error.value?.let {
            binding.tilPassword.error = FirestoreOps.mensajeError(this, it)
            return
        }

        val usuario = UsuarioRepository.autenticar(username, password)
        if (usuario == null) {
            AnalyticsLogger.logLoginFallido(this)
            binding.tilPassword.error = getString(R.string.error_login_invalido)
            return
        }

        entrar(usuario)
    }

    private fun iniciarConGoogle() {
        binding.tilPassword.error = null
        binding.btnLoginGoogle.isEnabled = false
        lifecycleScope.launch {
            try {
                val email = AuthGoogle.iniciar(this@MainActivity)
                val usuario = UsuarioRepository.autenticarPorEmail(email)
                if (usuario == null) {
                    AnalyticsLogger.logLoginFallido(this@MainActivity)
                    binding.tilPassword.error = getString(R.string.error_google_sin_usuario)
                    AuthGoogle.limpiar(this@MainActivity)
                } else {
                    entrar(usuario)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d("Google", "Inicio cancelado")
            } catch (e: NoCredentialException) {
                Log.e("Google", "Sin cuentas disponibles", e)
                binding.tilPassword.error = getString(R.string.error_google_sin_cuentas)
            } catch (e: Exception) {
                Log.e("Google", "Fallo inicio con Google", e)
                binding.tilPassword.error = getString(R.string.error_google_cancelado)
            } finally {
                binding.btnLoginGoogle.isEnabled = true
            }
        }
    }

    private fun entrar(usuario: Usuario) {
        AnalyticsLogger.logLoginExitoso(this, usuario.rol)
        Sesion.usuarioActual = usuario
        val destino = when (usuario.rol) {
            RolUsuario.ADMIN -> HomeActivity::class.java
            RolUsuario.JEFE_TALLER -> JefeTallerActivity::class.java
            RolUsuario.MECANICO, RolUsuario.REPUESTO, RolUsuario.DIAGNOSIS -> OperativoActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }
}
