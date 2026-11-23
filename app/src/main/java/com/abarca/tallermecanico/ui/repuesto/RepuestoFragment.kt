package com.abarca.tallermecanico.ui.repuesto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.util.Sesion
import com.abarca.tallermecanico.databinding.FragmentRepuestoBinding
import com.google.android.material.tabs.TabLayoutMediator

class RepuestoFragment : Fragment() {

    private var _binding: FragmentRepuestoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRepuestoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = RepuestoPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(if (position == 0) R.string.tab_inventario else R.string.title_donde_comprar)
        }.attach()
    }

    fun mostrarDondeComprar() {
        _binding?.viewPager?.currentItem = 1
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_repuesto, menu)
        // "Tiendas en línea" solo lo ve el jefe de taller.
        menu.findItem(R.id.action_tiendas_online)?.isVisible = Sesion.tieneRol(RolUsuario.JEFE_TALLER)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_agregar) {
            findNavController().navigate(R.id.action_repuesto_to_form)
            return true
        }
        if (item.itemId == R.id.action_tiendas_online) {
            findNavController().navigate(R.id.action_repuesto_to_tiendas)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
