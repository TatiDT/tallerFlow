package com.abarca.tallermecanico.ui.jefe

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.databinding.ActivityJefeTallerBinding
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.Sesion
import com.abarca.tallermecanico.util.irAlLogin
import com.abarca.tallermecanico.util.mostrarSelectorIdioma

class JefeTallerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJefeTallerBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!Sesion.tieneRol(RolUsuario.JEFE_TALLER)) {
            super.onCreate(null)
            irAlLogin()
            return
        }
        super.onCreate(savedInstanceState)
        binding = ActivityJefeTallerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.asistente.vincular(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
        // La barra va arriba: evitar el padding inferior que BottomNavigationView aplica por defecto
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { _, insets -> insets }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val topDestinations = setOf(
            R.id.clienteFragment,
            R.id.vehiculoFragment
        )
        val appBarConfig = AppBarConfiguration(topDestinations)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
        NavigationUI.setupWithNavController(binding.bottomNav, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sesion, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_idioma) {
            mostrarSelectorIdioma()
            return true
        }
        if (item.itemId == R.id.action_logout) {
            cerrarSesion()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun cerrarSesion() {
        AnalyticsLogger.logLogout(this, Sesion.usuarioActual?.rol)
        Sesion.cerrar()
        irAlLogin()
    }
}
