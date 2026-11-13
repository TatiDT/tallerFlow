package com.abarca.tallermecanico

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doOnTextChanged
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.Usuario
import com.abarca.tallermecanico.data.repository.TallerRepository
import com.abarca.tallermecanico.data.repository.UsuarioRepository
import com.abarca.tallermecanico.databinding.ActivityMainBinding
import com.abarca.tallermecanico.ui.home.HomeActivity
import com.abarca.tallermecanico.ui.jefe.JefeTallerActivity
import com.abarca.tallermecanico.ui.operativo.OperativoActivity
import com.abarca.tallermecanico.ui.registro.RegistroActivity
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.AuthGoogle
import com.abarca.tallermecanico.util.Notificaciones
import com.abarca.tallermecanico.util.RecordarSesion
import com.abarca.tallermecanico.util.Sesion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
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
        binding.tvRegistro.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }
        binding.etUsername.setOnFocusChangeListener { _, _ -> binding.tilUsername.error = null }
        binding.etPassword.setOnFocusChangeListener { _, _ -> binding.tilPassword.error = null }
        binding.etTaller.setOnFocusChangeListener { _, _ -> binding.tilTaller.error = null }
        binding.etTaller.setText(RecordarSesion.ultimoCodigo)
        binding.etUsername.doOnTextChanged { texto, _, _, _ ->
            val valor = texto?.toString().orEmpty()
            binding.tilTaller.visibility = if (valor.isNotBlank() && !valor.contains("@")) View.VISIBLE else View.GONE
        }
        binding.tvForgotPassword.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_password)
                .setMessage(R.string.msg_olvido_clave)
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
        }

        if (savedInstanceState == null && RecordarSesion.hayGuardada) intentarRestaurarSesion()
    }

    private fun intentarRestaurarSesion() {
        binding.loginCard.visibility = View.INVISIBLE
        var terminado = false

        fun mostrarFormulario() {
            if (terminado) return
            terminado = true
            binding.loginCard.visibility = View.VISIBLE
        }

        UsuarioRepository.usuarios.observe(this) { lista ->
            if (terminado) return@observe
            val usuario = RecordarSesion.restaurar()
            when {
                usuario != null -> {
                    terminado = true
                    entrar(usuario, recordar = true)
                }
                lista.isNotEmpty() -> mostrarFormulario()
            }
        }
        UsuarioRepository.error.observe(this) { if (it != null) mostrarFormulario() }
        binding.root.postDelayed({ mostrarFormulario() }, 6000)
    }

    private fun enCurso(activo: Boolean) {
        binding.btnLogin.isEnabled = !activo
        binding.btnLoginGoogle.isEnabled = !activo
        binding.tvRegistro.isEnabled = !activo
    }

    private fun mostrarError(mensaje: Int) {
        binding.tilPassword.error = getString(mensaje)
        enCurso(false)
    }

    private fun validateAndLogin() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val codigo = binding.etTaller.text?.toString()?.trim()?.uppercase() ?: ""
        val esCorreo = username.contains("@")
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.error_username_required)
            isValid = false
        } else binding.tilUsername.error = null

        if (!esCorreo && username.isNotEmpty() && codigo.isEmpty()) {
            binding.tilTaller.error = getString(R.string.error_codigo_requerido)
            isValid = false
        } else binding.tilTaller.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            isValid = false
        } else binding.tilPassword.error = null

        if (!isValid) return

        if (esCorreo) iniciarComoPropietaria(username, password) else iniciarComoPersonal(username, password, codigo)
    }

    private fun iniciarComoPropietaria(correo: String, password: String) {
        enCurso(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, password)
            .addOnSuccessListener { resultado ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                val uid = resultado.user?.uid
                if (uid == null) {
                    mostrarError(R.string.error_login_invalido)
                    return@addOnSuccessListener
                }
                abrirTallerYEntrar(
                    idTaller = uid,
                    mensajeFallo = R.string.error_cuenta_sin_taller,
                    cerrarCuenta = true
                ) { UsuarioRepository.autenticarPorEmail(correo)?.takeIf { it.usaCuentaCorreo } }
            }
            .addOnFailureListener {
                if (isFinishing || isDestroyed) return@addOnFailureListener
                if (it is FirebaseNetworkException) {
                    mostrarError(R.string.error_taller_sin_conexion)
                } else {
                    AnalyticsLogger.logLoginFallido(this)
                    mostrarError(R.string.error_login_invalido)
                }
            }
    }

    private fun iniciarComoPersonal(username: String, password: String, codigo: String) {
        enCurso(true)
        TallerRepository.idPorCodigo(codigo) { idTaller, error ->
            if (isFinishing || isDestroyed) return@idPorCodigo
            when {
                error != null -> mostrarError(R.string.error_taller_sin_conexion)
                idTaller == null -> {
                    binding.tilTaller.error = getString(R.string.error_codigo_invalido)
                    enCurso(false)
                }
                else -> abrirTallerYEntrar(
                    idTaller = idTaller,
                    mensajeFallo = R.string.error_login_invalido,
                    cerrarCuenta = false,
                    alEntrar = { RecordarSesion.ultimoCodigo = codigo }
                ) { UsuarioRepository.autenticar(username, password) }
            }
        }
    }

    private fun iniciarConGoogle() {
        binding.tilPassword.error = null
        enCurso(true)
        lifecycleScope.launch {
            try {
                val email = AuthGoogle.iniciar(this@MainActivity)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    mostrarError(R.string.error_google_cancelado)
                    return@launch
                }
                abrirTallerYEntrar(
                    idTaller = uid,
                    mensajeFallo = R.string.error_google_sin_usuario,
                    cerrarCuenta = true,
                    alFallar = { lifecycleScope.launch { AuthGoogle.limpiar(this@MainActivity) } }
                ) { UsuarioRepository.autenticarPorEmail(email)?.takeIf { it.usaCuentaCorreo } }
            } catch (e: GetCredentialCancellationException) {
                Log.d("Google", "Inicio cancelado")
                enCurso(false)
            } catch (e: NoCredentialException) {
                Log.e("Google", "Sin cuentas disponibles", e)
                mostrarError(R.string.error_google_sin_cuentas)
            } catch (e: Exception) {
                Log.e("Google", "Fallo inicio con Google", e)
                mostrarError(R.string.error_google_cancelado)
            }
        }
    }

    private fun abrirTallerYEntrar(
        idTaller: String,
        mensajeFallo: Int,
        cerrarCuenta: Boolean,
        alEntrar: () -> Unit = {},
        alFallar: () -> Unit = {},
        buscarUsuario: () -> Usuario?
    ) {
        TallerApp.instancia.abrirTaller(idTaller)
        EsperaUsuarios(this) { exito ->
            if (isFinishing || isDestroyed) return@EsperaUsuarios
            val usuario = if (exito) buscarUsuario() else null
            if (usuario == null) {
                if (exito) AnalyticsLogger.logLoginFallido(this)
                if (cerrarCuenta) TallerApp.instancia.cerrarSesionFirebase() else TallerApp.instancia.descartarTaller()
                alFallar()
                mostrarError(if (exito) mensajeFallo else R.string.error_taller_sin_conexion)
            } else {
                alEntrar()
                enCurso(false)
                entrar(usuario)
            }
        }
    }

    private fun entrar(usuario: Usuario, recordar: Boolean = binding.cbRecordarme.isChecked) {
        if (recordar) RecordarSesion.guardar(usuario) else RecordarSesion.borrar()
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

    private class EsperaUsuarios(owner: LifecycleOwner, private val alTerminar: (Boolean) -> Unit) {
        private val handler = Handler(Looper.getMainLooper())
        private var terminado = false
        private val observadorListo = Observer<Boolean> { if (it) terminar(true) }
        private val observadorError = Observer<Throwable?> { if (it != null) terminar(false) }
        private val limite = Runnable { terminar(false) }

        init {
            UsuarioRepository.listo.observe(owner, observadorListo)
            UsuarioRepository.error.observe(owner, observadorError)
            handler.postDelayed(limite, 10_000)
        }

        private fun terminar(exito: Boolean) {
            if (terminado) return
            terminado = true
            UsuarioRepository.listo.removeObserver(observadorListo)
            UsuarioRepository.error.removeObserver(observadorError)
            handler.removeCallbacks(limite)
            alTerminar(exito)
        }
    }
}
