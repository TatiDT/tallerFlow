package com.abarca.tallermecanico.util

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.abarca.tallermecanico.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AuthGoogle {

    val disponible: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun iniciar(context: Context): String {
        val opcion = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val peticion = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        val credencial = CredentialManager.create(context).getCredential(context, peticion).credential
        check(credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credencial de Google no reconocida"
        }
        val token = GoogleIdTokenCredential.createFrom(credencial.data).idToken
        val resultado = suspendCancellableCoroutine { continuacion ->
            FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                .addOnSuccessListener { continuacion.resume(it) }
                .addOnFailureListener { continuacion.resumeWithException(it) }
        }
        return checkNotNull(resultado.user?.email) { "La cuenta de Google no tiene correo" }
    }

    suspend fun limpiar(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
