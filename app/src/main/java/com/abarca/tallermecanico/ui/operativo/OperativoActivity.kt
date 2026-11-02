package com.abarca.tallermecanico.ui.operativo

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
import com.abarca.tallermecanico.databinding.ActivityOperativoBinding
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.Sesion
import com.abarca.tallermecanico.util.irAlLogin
import com.abarca.tallermecanico.util.mostrarSelectorIdioma

class OperativoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperativoBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!Sesion.tieneRol(RolUsuario.MECANICO, RolUsuario.REPUESTO, RolUsuario.DIAGNOSIS)) {
            super.onCreate(null)
            irAlLogin()
            return
        }
        super.onCreate(savedInstanceState)
        binding = ActivityOperativoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.asistente.vincular(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfig: AppBarConfiguration
        when (Sesion.usuarioActual!!.rol) {
            RolUsuario.MECANICO -> {
                navController.setGraph(R.navigation.nav_graph_mecanico)
                appBarConfig = AppBarConfiguration(setOf(R.id.ordenesActivasFragment))
            }
            RolUsuario.REPUESTO -> {
                navController.setGraph(R.navigation.nav_graph_repuesto)
                appBarConfig = AppBarConfiguration(setOf(R.id.repuestoFragment))
            }
            RolUsuario.DIAGNOSIS -> {
                navController.setGraph(R.navigation.nav_graph_diagnosis)
                appBarConfig = AppBarConfiguration(setOf(R.id.diagnosisVehiculosFragment))
            }
            else -> error("Rol sin pantalla operativa")
        }
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
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
