package com.abarca.tallermecanico.ui.repuesto

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abarca.tallermecanico.BuildConfig
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.data.repository.ErrorTiendas
import com.abarca.tallermecanico.data.repository.TiendaCercana
import com.abarca.tallermecanico.databinding.DialogCostoBinding
import com.abarca.tallermecanico.databinding.DialogTiendasOnlineBinding
import com.abarca.tallermecanico.ui.viewmodel.TiendaOnlineViewModel
import com.abarca.tallermecanico.util.ConsultaTienda
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.abarca.tallermecanico.databinding.FragmentDondeComprarBinding
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.ui.viewmodel.RepuestoViewModel
import com.abarca.tallermecanico.ui.viewmodel.EstadoTiendas
import com.abarca.tallermecanico.ui.viewmodel.TiendasViewModel
import com.abarca.tallermecanico.ui.viewmodel.VehiculoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class DondeComprarFragment : Fragment() {

    private var _binding: FragmentDondeComprarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TiendasViewModel by viewModels()
    private val vehiculoViewModel: VehiculoViewModel by activityViewModels()
    private val presupuestoViewModel: PresupuestoViewModel by activityViewModels()
    private val repuestoViewModel: RepuestoViewModel by activityViewModels()
    private val tiendaOnlineViewModel: TiendaOnlineViewModel by activityViewModels()
    private val marcadores = mutableMapOf<Long, Marker>()
    private var vehiculoSeleccionado: Vehiculo? = null
    private var vehiculoIdPendiente = -1

    private val centroTaller get() = GeoPoint(BuildConfig.TALLER_LAT, BuildConfig.TALLER_LNG)
    private val marca get() = vehiculoSeleccionado?.marca.orEmpty()
    private val modelo get() = vehiculoSeleccionado?.modelo.orEmpty()
    private val anio get() = vehiculoSeleccionado?.año ?: 0
    private val datosVehiculo get() = listOf(marca, modelo, if (anio > 0) anio.toString() else "").filter { it.isNotEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDondeComprarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarMapa()
        configurarBusqueda(savedInstanceState)
        vehiculoIdPendiente = savedInstanceState?.getInt("vehiculoSeleccionadoId", -1) ?: -1
        configurarVehiculos()
        actualizarVehiculo()

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("vehiculoSeleccionadoId", vehiculoSeleccionado?.id ?: -1)
    }

    private fun etiquetaVehiculo(v: Vehiculo) = "${v.marca} ${v.modelo} ${v.año} · ${v.patente}"

    private fun configurarVehiculos() {
        vehiculoViewModel.vehiculos.observe(viewLifecycleOwner) { todos ->
            val disponibles = todos.filter { it.estado != EstadoVehiculo.ENTREGADO }
            binding.actvVehiculo.setAdapter(
                android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    disponibles.map { etiquetaVehiculo(it) }
                )
            )
            if (vehiculoIdPendiente != -1) {
                val previo = disponibles.firstOrNull { it.id == vehiculoIdPendiente }
                vehiculoIdPendiente = -1
                if (previo != null) {
                    vehiculoSeleccionado = previo
                    binding.actvVehiculo.setText(etiquetaVehiculo(previo), false)
                    actualizarVehiculo()
                }
            }
            binding.actvVehiculo.setOnItemClickListener { _, _, posicion, _ ->
                vehiculoSeleccionado = disponibles[posicion]
                actualizarVehiculo()
            }
        }
        binding.btnAsignarCompra.setOnClickListener {
            val vehiculo = vehiculoSeleccionado
            if (vehiculo == null) {
                Toast.makeText(requireContext(), R.string.msg_elige_vehiculo_compra, Toast.LENGTH_SHORT).show()
            } else {
                pedirMontoYAsignar(vehiculo)
            }
        }
    }

    private fun actualizarVehiculo() {
        val datos = datosVehiculo
        binding.tvVehiculo.visibility = if (datos.isEmpty()) View.GONE else View.VISIBLE
        if (datos.isNotEmpty()) {
            binding.tvVehiculo.text = getString(R.string.label_para_vehiculo, datos.joinToString(" "))
        }
        actualizarBotonAsignar()
    }

    private fun actualizarBotonAsignar() {
        binding.btnAsignarCompra.isEnabled =
            vehiculoSeleccionado != null && !binding.etPieza.text.isNullOrBlank()
    }

    private fun pedirMontoYAsignar(vehiculo: Vehiculo) {
        val dialogBinding = DialogCostoBinding.inflate(layoutInflater)
        dialogBinding.etDescripcion.setText(binding.etPieza.text?.toString()?.trim().orEmpty())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_asignar_compra) + " · " + vehiculo.patente)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val descripcion = dialogBinding.etDescripcion.text?.toString()?.trim().orEmpty()
                val monto = dialogBinding.etMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
                var valido = true
                if (descripcion.isEmpty()) {
                    dialogBinding.tilDescripcion.error = getString(R.string.error_campo_requerido)
                    valido = false
                } else dialogBinding.tilDescripcion.error = null
                if (monto == null || monto <= 0) {
                    dialogBinding.tilMonto.error = getString(R.string.error_monto_invalido)
                    valido = false
                } else dialogBinding.tilMonto.error = null
                if (!valido) return@setOnClickListener

                // Se registra como "Repuesto adicional" del presupuesto, igual que los adicionales de diagnóstico.
                if (presupuestoViewModel.agregarRepuestoAdicional(vehiculo.id, descripcion, monto!!)) {
                    Toast.makeText(requireContext(), R.string.msg_compra_asignada, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), R.string.msg_compra_sin_presupuesto, Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
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
        repuestoViewModel.piezaBuscada.observe(viewLifecycleOwner, Observer { pieza ->
            if (pieza != null) {
                binding.etPieza.setText(pieza)
                repuestoViewModel.piezaBuscada.value = null
            }
        })
        // Se observa para mantener actualizada la lista de tiendas que muestra el BottomSheet.
        tiendaOnlineViewModel.tiendas.observe(viewLifecycleOwner) { }
        actualizarBotonComprarEnLinea()
        binding.etPieza.doAfterTextChanged { actualizarBotonComprarEnLinea() }
        binding.btnComprarEnLinea.setOnClickListener { mostrarTiendasEnLinea() }
    }

    // El botón solo se habilita cuando hay una pieza escrita.
    private fun actualizarBotonComprarEnLinea() {
        binding.btnComprarEnLinea.isEnabled = !binding.etPieza.text.isNullOrBlank()
        if (_binding != null) actualizarBotonAsignar()
    }

    // Muestra las tiendas activas en un BottomSheet; al elegir una se abre su búsqueda.
    private fun mostrarTiendasEnLinea() {
        val pieza = binding.etPieza.text?.toString().orEmpty().trim()
        if (pieza.isEmpty()) return
        val hoja = BottomSheetDialog(requireContext())
        val hojaBinding = DialogTiendasOnlineBinding.inflate(layoutInflater)
        val activas = tiendaOnlineViewModel.tiendas.value.orEmpty().filter { it.activa }
        hojaBinding.tvSinTiendas.visibility = if (activas.isEmpty()) View.VISIBLE else View.GONE
        activas.forEach { tienda ->
            val boton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            boton.text = tienda.nombre
            boton.setOnClickListener {
                abrir(Uri.parse(ConsultaTienda.url(tienda, pieza, datosVehiculo)))
                hoja.dismiss()
            }
            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            parametros.topMargin = (8 * resources.displayMetrics.density).toInt()
            hojaBinding.contenedorTiendas.addView(boton, parametros)
        }
        hoja.setContentView(hojaBinding.root)
        hoja.show()
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
        ErrorTiendas.Tipo.TIEMPO_AGOTADO -> R.string.error_tiendas_tiempo_agotado
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
