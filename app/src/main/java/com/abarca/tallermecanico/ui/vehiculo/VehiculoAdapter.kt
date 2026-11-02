package com.abarca.tallermecanico.ui.vehiculo

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abarca.tallermecanico.R
import com.abarca.tallermecanico.util.colorRes
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.RolUsuario
import com.abarca.tallermecanico.data.entity.VehiculoConCliente
import com.abarca.tallermecanico.databinding.ItemVehiculoBinding
import com.abarca.tallermecanico.util.Sesion
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VehiculoAdapter(
    private val onItemClick: (VehiculoConCliente) -> Unit,
    private val onPresupuestoClick: (VehiculoConCliente) -> Unit
) : ListAdapter<VehiculoConCliente, VehiculoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemVehiculoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VehiculoConCliente) {
            val v = item.vehiculo
            val c = item.cliente
            val m = item.mecanico
            binding.tvPatente.text = v.patente
            binding.tvMarcaModelo.text = "${v.marca} ${v.modelo} · ${v.año}"
            binding.tvClienteNombre.text = "Cliente: ${c.nombre} ${c.apellido}"
            binding.tvEstadoBadge.text = v.estado.display

            if (m != null) {
                binding.tvMecanicoNombre.text = "Mecánico: ${m.nombre} ${m.apellido}"
                binding.tvMecanicoNombre.visibility = android.view.View.VISIBLE
            } else {
                binding.tvMecanicoNombre.visibility = android.view.View.GONE
            }

            val colorInt = ContextCompat.getColor(binding.root.context, v.estado.colorRes())
            binding.tvEstadoBadge.backgroundTintList = ColorStateList.valueOf(colorInt)
            binding.viewEstadoBar.setBackgroundColor(colorInt)

            // Admin arma y aprueba el presupuesto; Jefe de Taller lo consulta antes de asignar mecánico
            val rol = Sesion.usuarioActual?.rol
            if (rol == RolUsuario.ADMIN || rol == RolUsuario.JEFE_TALLER) {
                val ctx = binding.root.context
                binding.tvPresupuesto.visibility = android.view.View.VISIBLE
                val presupuesto = item.presupuesto
                val resumen = when {
                    presupuesto != null -> {
                        val total = NumberFormat.getNumberInstance(Locale("es", "CL")).format(presupuesto.total)
                        val estado = ctx.getString(
                            if (presupuesto.aprobado) R.string.label_presupuesto_aprobado else R.string.label_presupuesto_pendiente
                        )
                        ctx.getString(R.string.label_presupuesto, "$$total", presupuesto.tratamientos.size, estado)
                    }
                    rol == RolUsuario.ADMIN -> ctx.getString(R.string.label_sin_presupuesto)
                    else -> ctx.getString(R.string.label_sin_presupuesto_jefe)
                }
                val avisos = if (rol == RolUsuario.ADMIN) listOfNotNull(
                    ctx.getString(R.string.label_hallazgos_por_cotizar, item.fallasPorCotizar).takeIf { item.fallasPorCotizar > 0 },
                    ctx.getString(R.string.label_adicionales_por_cotizar, item.adicionalesPendientes).takeIf { item.adicionalesPendientes > 0 }
                ) else emptyList()
                binding.tvPresupuesto.text = (listOf(resumen) + avisos).joinToString("\n")
                binding.tvPresupuesto.setOnClickListener { onPresupuestoClick(item) }
            } else {
                binding.tvPresupuesto.visibility = android.view.View.GONE
            }

            if (Sesion.usuarioActual?.rol == RolUsuario.JEFE_TALLER && v.fechaLimite.isNotEmpty()) {
                binding.tvFechaLimite.visibility = android.view.View.VISIBLE
                binding.tvFechaLimite.text = binding.root.context.getString(R.string.label_fecha_limite, v.fechaLimite)
                val terminado = v.estado == EstadoVehiculo.LISTO || v.estado == EstadoVehiculo.ENTREGADO
                val vencida = !terminado && esVencida(v.fechaLimite)
                binding.tvFechaLimite.setTextColor(
                    ContextCompat.getColor(binding.root.context, if (vencida) R.color.color_vencido else R.color.tf_text_secondary)
                )
            } else {
                binding.tvFechaLimite.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun esVencida(fechaLimite: String): Boolean = try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val limite = sdf.parse(fechaLimite)
            val hoy = sdf.parse(sdf.format(Date()))
            limite != null && hoy != null && limite.before(hoy)
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<VehiculoConCliente>() {
        override fun areItemsTheSame(old: VehiculoConCliente, new: VehiculoConCliente) =
            old.vehiculo.id == new.vehiculo.id
        override fun areContentsTheSame(old: VehiculoConCliente, new: VehiculoConCliente) =
            old == new
    }
}
