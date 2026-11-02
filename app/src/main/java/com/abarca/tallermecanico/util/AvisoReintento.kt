package com.abarca.tallermecanico.util

import androidx.fragment.app.Fragment
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.TallerApp
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class AvisoReintento(private val fragment: Fragment) {

    private var snackbar: Snackbar? = null

    fun mostrar(mensaje: String) {
        if (snackbar != null) return
        val vista = fragment.view ?: return
        snackbar = Snackbar.make(vista, mensaje, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.reintentar) {
                (fragment.requireContext().applicationContext as TallerApp).reintentar()
            }
            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    snackbar = null
                }
            })
        snackbar?.show()
    }

    fun ocultar() {
        snackbar?.dismiss()
        snackbar = null
    }
}
