package com.abarca.tallermecanico.util

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckProveedor {
    fun fabrica(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
}
