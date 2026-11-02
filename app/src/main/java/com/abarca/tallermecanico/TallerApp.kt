package com.abarca.tallermecanico

import android.app.Application
import android.util.Log
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
import com.abarca.tallermecanico.util.Notificaciones
import com.abarca.tallermecanico.util.Traductor
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging

class TallerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckProveedor.fabrica())
        Traductor.iniciar(this)
        Notificaciones.crearCanal(this)
        iniciarSesionFirebase()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { Log.d("FCM", "Token: $it") }
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
            }
    }

    fun reintentar() {
        ClienteRepository.reiniciar()
        RepuestoRepository.reiniciar()
        VehiculoRepository.reiniciar()
        UsuarioRepository.reiniciar()
        FallaRepository.reiniciar()
        DiagnosticoRepository.reiniciar()
        TareaRepository.reiniciar()
        CostoRepository.reiniciar()
        PresupuestoRepository.reiniciar()
        iniciarSesionFirebase()
    }

    private fun iniciarRepositorios() {
        ClienteRepository.iniciar()
        RepuestoRepository.iniciar()
        VehiculoRepository.iniciar()
        UsuarioRepository.iniciar()
        FallaRepository.iniciar()
        DiagnosticoRepository.iniciar()
        TareaRepository.iniciar()
        CostoRepository.iniciar()
        PresupuestoRepository.iniciar()
    }
}
