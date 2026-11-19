package com.abarca.tallermecanico.ui.repuesto

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.BuildConfig
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.repository.ErrorTiendas
import com.abarca.tallermecanico.data.repository.TiendaCercana
import com.abarca.tallermecanico.databinding.FragmentDondeComprarBinding
import com.abarca.tallermecanico.ui.viewmodel.EstadoTiendas
import com.abarca.tallermecanico.ui.viewmodel.TiendasViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.net.URLEncoder

class DondeComprarFragment : Fragment() {

    private var _binding: FragmentDondeComprarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TiendasViewModel by viewModels()
    private val marcadores = mutableMapOf<Long, Marker>()

    private val centroTaller get() = GeoPoint(BuildConfig.TALLER_LAT, BuildConfig.TALLER_LNG)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDondeComprarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarMapa()
        configurarBusqueda(savedInstanceState)

        val adapter = TiendaAdapter(
            alTocar = { centrarEnTienda(it) },
            alComoLlegar = { abrir(Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${it.lat},${it.lng}")) },
            alLlamar = { abrir(Uri.parse("tel:" + Uri.encode(it)), Intent.ACTION_DIAL) },
            alAbrirSitio = { abrir(normalizarSitio(it)) }
        )
        binding.recyclerTiendas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTiendas.adapter = adapter
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progress.visibility = if (estado is EstadoTiendas.Cargando) View.VISIBLE else View.GONE
            when (estado) {
                is EstadoTiendas.Cargando -> {
                    binding.panelEstado.visibility = View.GONE
                    adapter.submitList(emptyList())
                    mostrarTiendasEnMapa(emptyList())
                }
                is EstadoTiendas.Listo -> {
                    adapter.submitList(estado.tiendas)
                    mostrarTiendasEnMapa(estado.tiendas)
                    binding.panelEstado.visibility = if (estado.tiendas.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvEstado.setText(R.string.empty_tiendas)
                    binding.btnReintentar.visibility = View.GONE
                }
                is EstadoTiendas.Error -> {
                    adapter.submitList(emptyList())
                    mostrarTiendasEnMapa(emptyList())
                    binding.panelEstado.visibility = View.VISIBLE
                    binding.tvEstado.setText(mensajeDeError(estado.tipo))
                    binding.btnReintentar.visibility =
                        if (estado.tipo == ErrorTiendas.Tipo.SIN_UBICACION) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun configurarMapa() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(centroTaller)
        }
        val marcadorTaller = Marker(binding.mapView).apply {
            position = centroTaller
            title = getString(R.string.marcador_taller)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_taller)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapView.overlays.add(marcadorTaller)
    }

    private fun configurarBusqueda(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            binding.etPieza.setText(arguments?.getString("nombrePieza").orEmpty())
        }
        actualizarBotonMercadoLibre()
        binding.etPieza.doAfterTextChanged { actualizarBotonMercadoLibre() }
        binding.btnMercadoLibre.setOnClickListener {
            val pieza = binding.etPieza.text?.toString().orEmpty().trim()
            if (pieza.isNotEmpty()) abrir(Uri.parse(urlMercadoLibre(pieza)))
        }
    }

    private fun actualizarBotonMercadoLibre() {
        binding.btnMercadoLibre.isEnabled = !binding.etPieza.text.isNullOrBlank()
    }

    private fun urlMercadoLibre(pieza: String): String {
        val ruta = pieza.trim().lowercase().replace(Regex("\\s+"), "-")
        return "https://listado.mercadolibre.cl/" + URLEncoder.encode(ruta, "UTF-8")
    }

    private fun mostrarTiendasEnMapa(tiendas: List<TiendaCercana>) {
        val mapa = binding.mapView
        marcadores.values.forEach { mapa.overlays.remove(it) }
        marcadores.clear()
        tiendas.forEach { tienda ->
            val detalle = listOfNotNull(
                tienda.direccion ?: getString(R.string.direccion_no_registrada),
                tienda.horario?.let { getString(R.string.label_horario_tienda, it) }
            ).joinToString("\n")
            val marcador = Marker(mapa).apply {
                position = GeoPoint(tienda.lat, tienda.lng)
                title = tienda.nombre
                snippet = detalle
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            marcadores[tienda.id] = marcador
            mapa.overlays.add(marcador)
        }
        mapa.invalidate()
    }

    private fun centrarEnTienda(tienda: TiendaCercana) {
        val mapa = binding.mapView
        mapa.controller.animateTo(GeoPoint(tienda.lat, tienda.lng))
        marcadores[tienda.id]?.let {
            InfoWindow.closeAllInfoWindowsOn(mapa)
            it.showInfoWindow()
        }
    }

    private fun mensajeDeError(tipo: ErrorTiendas.Tipo): Int = when (tipo) {
        ErrorTiendas.Tipo.SIN_INTERNET -> R.string.error_tiendas_sin_internet
        ErrorTiendas.Tipo.SERVICIO_SATURADO -> R.string.error_tiendas_saturado
        ErrorTiendas.Tipo.SERVICIO_CAIDO -> R.string.error_tiendas_servicio
        ErrorTiendas.Tipo.SIN_UBICACION -> R.string.error_tiendas_sin_ubicacion
    }

    private fun normalizarSitio(sitio: String): Uri {
        val limpio = sitio.trim()
        val conEsquema = if (limpio.startsWith("http://") || limpio.startsWith("https://")) limpio else "https://$limpio"
        return Uri.parse(conEsquema)
    }

    private fun abrir(uri: Uri, accion: String = Intent.ACTION_VIEW) {
        try {
            startActivity(Intent(accion, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.msg_sin_app_para_abrir, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDetach()
        marcadores.clear()
        super.onDestroyView()
        _binding = null
    }
}
