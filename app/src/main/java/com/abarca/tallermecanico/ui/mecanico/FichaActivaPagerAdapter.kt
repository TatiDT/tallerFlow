package com.abarca.tallermecanico.ui.mecanico

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FichaActivaPagerAdapter(
    fragment: Fragment,
    private val vehiculoId: Int
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> TareasFragment.newInstance(vehiculoId)
        1 -> CostosFragment.newInstance(vehiculoId)
        else -> InspeccionFragment.newInstance(vehiculoId)
    }
}
