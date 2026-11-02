package com.abarca.tallermecanico.util

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckProveedor {
    fun fabrica(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
