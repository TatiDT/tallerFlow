package com.abarca.tallermecanico.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.abarca.tallermecanico.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Activity.mostrarCodigoTaller() {
    val codigo = TallerActual.codigo
    val mensaje = getString(R.string.msg_codigo_taller) + "\n\n" +
        if (codigo.isEmpty()) getString(R.string.msg_codigo_cargando) else codigo

    val dialogo = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.title_codigo_taller)
        .setMessage(mensaje)
        .setNegativeButton(R.string.btn_cerrar, null)

    if (codigo.isNotEmpty()) {
        dialogo.setPositiveButton(R.string.btn_copiar) { _, _ ->
            val portapapeles = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            portapapeles.setPrimaryClip(ClipData.newPlainText("codigo_taller", codigo))
            Aviso.mostrar(this, R.string.msg_codigo_copiado)
        }
    }
    dialogo.show()
}
