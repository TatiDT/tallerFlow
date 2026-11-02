package com.abarca.tallermecanico.ui.presupuesto

import android.graphics.Paint
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.data.entity.Adicional
import com.abarca.tallermecanico.data.entity.EstadoAdicional
import com.abarca.tallermecanico.data.entity.Falla
import com.abarca.tallermecanico.data.entity.Presupuesto
import com.abarca.tallermecanico.data.entity.Tratamiento
import com.abarca.tallermecanico.data.repository.TratamientoRepository
import com.abarca.tallermecanico.databinding.DialogCostoBinding
import com.abarca.tallermecanico.databinding.DialogPresupuestoBinding
import com.abarca.tallermecanico.ui.viewmodel.PresupuestoViewModel
import com.abarca.tallermecanico.util.AnalyticsLogger
import com.abarca.tallermecanico.util.Formato
import com.abarca.tallermecanico.util.Texto
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class PresupuestoDialog(
    private val fragment: Fragment,
    private val viewModel: PresupuestoViewModel,
    private val vehiculoId: Int,
    private val modoSolicitado: Modo
) {

    enum class Modo { EDITAR, AGREGAR, VER }

    private val presupuesto: Presupuesto? = viewModel.getByVehiculoId(vehiculoId)
    private val modo = if (presupuesto?.aprobado == true) Modo.VER else modoSolicitado
    private val existentes = presupuesto?.tratamientos.orEmpty()
    private val existentesIds = existentes.map { it.id }.toSet()
    private val seleccionados = existentesIds.toMutableSet()


    private val opciones: List<Tratamiento> = when (modo) {
        Modo.VER -> existentes
        Modo.AGREGAR -> viewModel.tratamientos + existentes.filter { !TratamientoRepository.esDelCatalogo(it.id) }
        Modo.EDITAR -> viewModel.tratamientos +
            existentes.filter { !TratamientoRepository.esDelCatalogo(it.id) && it.fallaId == null }
    }

    private val fallas: List<Falla> = if (modo == Modo.EDITAR) viewModel.fallas(vehiculoId) else emptyList()
    private val itemsFallaExistentes = existentes.filter { it.fallaId != null }.associateBy { it.fallaId!! }
    private val preciosFalla = itemsFallaExistentes.mapValues { it.value.precio }.toMutableMap()
    private val fallasIncluidas = itemsFallaExistentes.keys.toMutableSet()

    private val puedeResponderAdicionales = modoSolicitado == Modo.EDITAR

    private lateinit var binding: DialogPresupuestoBinding

    fun show() {
        val ctx = fragment.requireContext()
        binding = DialogPresupuestoBinding.inflate(fragment.layoutInflater)

        presupuesto?.let {
            val estado = if (it.aprobado) ctx.getString(R.string.label_presupuesto_aprobado_el, it.fechaAprobacion)
            else ctx.getString(R.string.label_presupuesto_pendiente)
            binding.tvFechaPresupuesto.text = ctx.getString(R.string.label_fecha_presupuesto, it.fechaCreacion) + " · " + estado
            binding.tvFechaPresupuesto.visibility = View.VISIBLE
        }

        opciones.forEach { tratamiento ->
            val checkBox = MaterialCheckBox(ctx).apply {
                text = ctx.getString(R.string.label_tratamiento_precio, tratamiento.nombre, Formato.pesos(tratamiento.precio))
                isChecked = tratamiento.id in seleccionados
                isEnabled = when (modo) {
                    Modo.EDITAR -> true
                    Modo.AGREGAR -> tratamiento.id !in existentesIds
                    Modo.VER -> false
                }
                buttonTintList = ContextCompat.getColorStateList(ctx, R.color.tf_accent)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) seleccionados.add(tratamiento.id) else seleccionados.remove(tratamiento.id)
                    binding.tvErrorTratamientos.visibility = View.GONE
                    actualizarTotales()
                }
            }
            binding.llTratamientos.addView(checkBox)
        }

        if (fallas.isNotEmpty()) {
            binding.tvTituloHallazgos.visibility = View.VISIBLE
            binding.llHallazgos.visibility = View.VISIBLE
            fallas.forEachIndexed { index, falla -> agregarFilaFalla(falla, index + 1) }
        }

        if (modo == Modo.VER) {
            binding.cbOtro.visibility = View.GONE
        } else {
            binding.cbOtro.setOnCheckedChangeListener { _, checked ->
                binding.llOtro.visibility = if (checked) View.VISIBLE else View.GONE
                binding.tvErrorTratamientos.visibility = View.GONE
                actualizarTotales()
            }
            binding.etOtroMonto.doAfterTextChanged {
                binding.tilOtroMonto.error = null
                actualizarTotales()
            }
            binding.etOtroDescripcion.doAfterTextChanged { binding.tilOtroDescripcion.error = null }
        }
        actualizarTotales()
        mostrarAdicionales()

        val titulo = when {
            modo == Modo.VER -> R.string.title_presupuesto
            modo == Modo.AGREGAR -> R.string.title_agregar_presupuesto
            presupuesto == null -> R.string.title_nuevo_presupuesto
            else -> R.string.title_editar_presupuesto
        }
        val builder = MaterialAlertDialogBuilder(ctx)
            .setTitle(titulo)
            .setView(binding.root)

        if (modo == Modo.VER) {
            builder.setPositiveButton(R.string.btn_cerrar, null).show()
            return
        }

        builder.setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
        if (modo == Modo.EDITAR) builder.setNeutralButton(R.string.btn_aprobar, null)
        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val elegidos = recolectar() ?: return@setOnClickListener
                if (modo == Modo.AGREGAR && elegidos.size == existentes.size) {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                viewModel.guardar(vehiculoId, elegidos)
                val accion = when {
                    modo == Modo.AGREGAR -> "agregar"
                    presupuesto == null -> "crear"
                    else -> "editar"
                }
                AnalyticsLogger.logCrud(ctx, "presupuesto", accion)
                dialog.dismiss()
            }
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                val elegidos = recolectar() ?: return@setOnClickListener
                val sinPrecio = fallas.count { it.id !in fallasIncluidas }
                var mensaje = ctx.getString(R.string.confirm_aprobar_presupuesto_message)
                if (sinPrecio > 0) mensaje += ctx.getString(R.string.msg_hallazgos_fuera, sinPrecio)
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.confirm_aprobar_presupuesto_title)
                    .setMessage(mensaje)
                    .setPositiveButton(R.string.btn_aprobar) { _, _ ->
                        viewModel.guardar(vehiculoId, elegidos)
                        viewModel.aprobar(vehiculoId)
                        AnalyticsLogger.logCrud(ctx, "presupuesto", "aprobar")
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.btn_cancelar, null)
                    .show()
            }
        }
        dialog.show()
    }

    private fun agregarFilaFalla(falla: Falla, numero: Int) {
        val ctx = fragment.requireContext()
        val checkBox = MaterialCheckBox(ctx).apply {
            buttonTintList = ContextCompat.getColorStateList(ctx, R.color.tf_accent)
        }
        fun refrescar() {
            val incluida = falla.id in fallasIncluidas
            checkBox.isChecked = incluida
            checkBox.text = if (incluida) {
                ctx.getString(R.string.label_hallazgo_precio, numero, falla.descripcion, Formato.pesos(preciosFalla.getValue(falla.id)))
            } else {
                ctx.getString(R.string.label_hallazgo_sin_precio, numero, falla.descripcion)
            }
            binding.tvErrorTratamientos.visibility = View.GONE
            actualizarTotales()
        }
        checkBox.setOnClickListener {
            if (falla.id in fallasIncluidas) {
                fallasIncluidas.remove(falla.id)
                refrescar()
            } else {
                refrescar()
                pedirPrecioFalla(falla, numero) { precio ->
                    preciosFalla[falla.id] = precio
                    fallasIncluidas.add(falla.id)
                    refrescar()
                }
            }
        }
        refrescar()
        binding.llHallazgos.addView(checkBox)
    }

    private fun pedirPrecioFalla(falla: Falla, numero: Int, onPrecio: (Double) -> Unit) {
        val ctx = fragment.requireContext()
        val precioBinding = DialogCostoBinding.inflate(fragment.layoutInflater)
        precioBinding.tilDescripcion.visibility = View.GONE
        preciosFalla[falla.id]?.let { precioBinding.etMonto.setText(it.toLong().toString()) }
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(ctx.getString(R.string.title_precio_falla, numero))
            .setMessage(falla.descripcion)
            .setView(precioBinding.root)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val precio = precioBinding.etMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
                if (precio == null || precio <= 0) {
                    precioBinding.tilMonto.error = ctx.getString(R.string.error_monto_invalido)
                    return@setOnClickListener
                }
                onPrecio(precio)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun mostrarAdicionales() {
        val ctx = fragment.requireContext()
        val actual = viewModel.getByVehiculoId(vehiculoId)
        val adicionales = actual?.adicionales.orEmpty()
        val visibilidad = if (adicionales.isEmpty()) View.GONE else View.VISIBLE
        binding.tvTituloAdicionales.visibility = visibilidad
        binding.llAdicionales.visibility = visibilidad
        binding.tvAyudaAdicionales.visibility =
            if (puedeResponderAdicionales && actual?.adicionalesPendientes.orEmpty().isNotEmpty()) View.VISIBLE else View.GONE

        binding.llAdicionales.removeAllViews()
        val padding = (8 * ctx.resources.displayMetrics.density).toInt()
        adicionales.forEach { adicional ->
            val fila = TextView(ctx).apply {
                text = ctx.getString(
                    R.string.label_adicional_fila,
                    adicional.descripcion,
                    adicional.precio?.let { Formato.pesos(it) } ?: ctx.getString(R.string.label_sin_precio),
                    adicional.estado.display
                )
                textSize = 14f
                setPadding(0, padding, 0, padding)
                setTextColor(ContextCompat.getColor(ctx, when (adicional.estado) {
                    EstadoAdicional.PENDIENTE -> R.color.tf_text_primary
                    EstadoAdicional.APROBADO -> R.color.tf_accent
                    EstadoAdicional.RECHAZADO -> R.color.color_vencido
                }))
                if (adicional.estado == EstadoAdicional.RECHAZADO) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                if (puedeResponderAdicionales && adicional.estado == EstadoAdicional.PENDIENTE) {
                    setOnClickListener { responderAdicional(adicional) }
                }
            }
            binding.llAdicionales.addView(fila)
        }
        actualizarTotales()
    }

    private fun responderAdicional(adicional: Adicional) {
        val ctx = fragment.requireContext()
        val precioBinding = DialogCostoBinding.inflate(fragment.layoutInflater)
        precioBinding.tilDescripcion.visibility = View.GONE
        adicional.precio?.let { precioBinding.etMonto.setText(it.toLong().toString()) }
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.title_adicional)
            .setMessage(adicional.descripcion)
            .setView(precioBinding.root)
            .setPositiveButton(R.string.btn_cliente_aprueba, null)
            .setNegativeButton(R.string.btn_cliente_rechaza) { _, _ ->
                val ofrecido = precioBinding.etMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
                viewModel.responderAdicional(vehiculoId, adicional.id, ofrecido, aprobado = false)
                AnalyticsLogger.logCrud(ctx, "adicional", "rechazar")
                mostrarAdicionales()
            }
            .setNeutralButton(R.string.btn_cancelar, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val precio = precioBinding.etMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
                if (precio == null || precio <= 0) {
                    precioBinding.tilMonto.error = ctx.getString(R.string.error_monto_invalido)
                    return@setOnClickListener
                }
                viewModel.responderAdicional(vehiculoId, adicional.id, precio, aprobado = true)
                AnalyticsLogger.logCrud(ctx, "adicional", "aprobar")
                dialog.dismiss()
                mostrarAdicionales()
            }
        }
        dialog.show()
    }

    private fun montoAdicionales(): Double =
        viewModel.getByVehiculoId(vehiculoId)?.adicionalesAprobados.orEmpty().sumOf { it.precio ?: 0.0 }

    private fun montoFallas(): Double = fallasIncluidas.sumOf { preciosFalla.getValue(it) }

    private fun montoOtro(): Double =
        if (binding.cbOtro.isChecked) binding.etOtroMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble() ?: 0.0
        else 0.0

    private fun actualizarTotales() {
        val neto = opciones.filter { it.id in seleccionados }.sumOf { it.precio } + montoFallas() + montoOtro() + montoAdicionales()
        val iva = Presupuesto.calcularIva(neto)
        binding.tvNeto.text = Formato.pesos(neto)
        binding.tvIva.text = Formato.pesos(iva)
        binding.tvTotal.text = Formato.pesos(neto + iva)
    }


    private fun recolectar(): List<Tratamiento>? {
        val ctx = fragment.requireContext()
        val incluyeOtro = binding.cbOtro.isChecked
        if (opciones.none { it.id in seleccionados } && fallasIncluidas.isEmpty() && !incluyeOtro) {
            binding.tvErrorTratamientos.visibility = View.VISIBLE
            return null
        }

        val elegidos = opciones.filter { it.id in seleccionados }.toMutableList()
        fallas.filter { it.id in fallasIncluidas }.forEach { falla ->
            val id = itemsFallaExistentes[falla.id]?.id ?: TratamientoRepository.nuevoIdOtro()
            elegidos.add(Tratamiento(id, falla.descripcion, preciosFalla.getValue(falla.id), fallaId = falla.id))
        }
        if (incluyeOtro) {
            val descripcion = binding.etOtroDescripcion.text?.toString()?.trim() ?: ""
            val monto = binding.etOtroMonto.text?.toString()?.trim()?.toLongOrNull()?.toDouble()
            var isValid = true
            val repetido = opciones.any { Texto.normalizar(it.nombre) == Texto.normalizar(descripcion) }
            if (descripcion.isEmpty()) { binding.tilOtroDescripcion.error = ctx.getString(R.string.error_campo_requerido); isValid = false }
            else if (repetido) { binding.tilOtroDescripcion.error = ctx.getString(R.string.error_tratamiento_duplicado); isValid = false }
            if (monto == null || monto <= 0) { binding.tilOtroMonto.error = ctx.getString(R.string.error_monto_invalido); isValid = false }
            if (!isValid) return null
            elegidos.add(Tratamiento(TratamientoRepository.nuevoIdOtro(), descripcion, monto!!))
        }
        return elegidos
    }
}
