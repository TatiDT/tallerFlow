package com.abarca.tallermecanico.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import com.abarca.tallermecanico.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference

object Aviso {

    private var actividad: WeakReference<Activity>? = null

    fun iniciar(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                actividad = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (actividad?.get() === activity) actividad = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun mostrar(ctx: Context, @StringRes mensaje: Int) = mostrar(ctx, ctx.getString(mensaje))

    fun mostrar(ctx: Context, mensaje: CharSequence) {
        val pantalla = actividad?.get()
        if (pantalla == null || pantalla.isFinishing || pantalla.isDestroyed) {
            Toast.makeText(ctx.applicationContext, mensaje, Toast.LENGTH_LONG).show()
            return
        }
        MaterialAlertDialogBuilder(pantalla)
            .setMessage(mensaje)
            .setPositiveButton(R.string.btn_cerrar, null)
            .show()
    }
}
