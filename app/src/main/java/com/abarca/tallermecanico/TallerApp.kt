package com.abarca.tallermecanico

import android.app.Application
import android.util.Log
import com.abarca.tallermecanico.data.repository.AgendaRepository
import com.abarca.tallermecanico.data.repository.ClienteRepository
import com.abarca.tallermecanico.data.repository.CostoRepository
import com.abarca.tallermecanico.data.repository.DiagnosticoRepository
import com.abarca.tallermecanico.data.repository.FallaRepository
import com.abarca.tallermecanico.data.repository.PresupuestoRepository
import com.abarca.tallermecanico.data.repository.RepuestoRepository
import com.abarca.tallermecanico.data.repository.TareaRepository
import com.abarca.tallermecanico.data.repository.UsuarioRepository
import com.abarca.tallermecanico.data.repository.VehiculoRepository
import com.abarca.tallermecanico.util.AppCheckProveedor
import com.abarca.tallermecanico.util.Aviso
import com.abarca.tallermecanico.util.Notificaciones
import com.abarca.tallermecanico.util.RecordarSesion
import com.abarca.tallermecanico.util.TallerActual
import com.abarca.tallermecanico.util.Traductor
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import org.osmdroid.config.Configuration
import java.io.File

class TallerApp : Application() {

    companion object {
        lateinit var instancia: TallerApp
            private set

        fun userAgentTaller(): String = "TallerFlow/" + BuildConfig.VERSION_NAME + " (com.abarca.tallermecanico)"
    }

    override fun onCreate() {
        super.onCreate()
        instancia = this
        Aviso.iniciar(this)
        configurarMapa()
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckProveedor.fabrica())
        Traductor.iniciar(this)
        RecordarSesion.iniciar(this)
        RecordarSesion.tallerId?.let { TallerActual.fijar(it) }
        Notificaciones.crearCanal(this)
        iniciarSesionFirebase()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { Log.d("FCM", "Token: $it") }
    }

    private fun configurarMapa() {
        Configuration.getInstance().apply {
            userAgentValue = userAgentTaller()
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }

    private fun iniciarSesionFirebase() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            iniciarRepositorios()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { iniciarRepositorios() }
            .addOnFailureListener {
                FirebaseCrashlytics.getInstance().recordException(it)
                ClienteRepository.publicarError(it)
                RepuestoRepository.publicarError(it)
                VehiculoRepository.publicarError(it)
                UsuarioRepository.publicarError(it)
                FallaRepository.publicarError(it)
                DiagnosticoRepository.publicarError(it)
                TareaRepository.publicarError(it)
                CostoRepository.publicarError(it)
                PresupuestoRepository.publicarError(it)
                AgendaRepository.publicarError(it)
            }
    }

    fun reintentar() {
        limpiarRepositorios()
        iniciarSesionFirebase()
    }

    fun abrirTaller(id: String, nombre: String = "", codigo: String = "") {
        if (TallerActual.id != id) {
            limpiarRepositorios()
            TallerActual.fijar(id, nombre, codigo)
        } else if (TallerActual.codigo.isEmpty()) {
            TallerActual.fijar(id, nombre, codigo)
        }
        iniciarRepositorios()
    }

    fun descartarTaller() {
        limpiarRepositorios()
        TallerActual.limpiar()
    }

    fun cerrarSesionFirebase() {
        descartarTaller()
        FirebaseAuth.getInstance().signOut()
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnFailureListener { FirebaseCrashlytics.getInstance().recordException(it) }
    }

    private fun limpiarRepositorios() {
        ClienteRepository.reiniciar()
        RepuestoRepository.reiniciar()
        VehiculoRepository.reiniciar()
        UsuarioRepository.reiniciar()
        FallaRepository.reiniciar()
        DiagnosticoRepository.reiniciar()
        TareaRepository.reiniciar()
        CostoRepository.reiniciar()
        PresupuestoRepository.reiniciar()
        AgendaRepository.reiniciar()
    }

    private fun iniciarRepositorios() {
        if (TallerActual.id == null) return
        ClienteRepository.iniciar()
        RepuestoRepository.iniciar()
        VehiculoRepository.iniciar()
        UsuarioRepository.iniciar()
        FallaRepository.iniciar()
        DiagnosticoRepository.iniciar()
        TareaRepository.iniciar()
        CostoRepository.iniciar()
        PresupuestoRepository.iniciar()
        AgendaRepository.iniciar()
    }
}
