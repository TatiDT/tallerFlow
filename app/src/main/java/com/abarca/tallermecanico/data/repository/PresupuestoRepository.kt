package com.abarca.tallermecanico.data.repository

import androidx.lifecycle.LiveData
import com.abarca.tallermecanico.data.entity.Adicional
import com.abarca.tallermecanico.data.entity.EstadoAdicional
import com.abarca.tallermecanico.data.entity.Presupuesto
import com.abarca.tallermecanico.data.entity.Tarea
import com.abarca.tallermecanico.data.entity.Tratamiento
import com.abarca.tallermecanico.util.NotificadorCorreo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PresupuestoRepository {

    private val coleccion = ColeccionFirestore(
        nombre = "presupuestos",
        idDe = Presupuesto::id,
        aMapa = { it.aMapa() },
        deDocumento = { it.aPresupuesto() }
    )

    val presupuestos: LiveData<List<Presupuesto>> = coleccion.items

    private var ultimoAdicionalId = 0

    fun iniciar() = coleccion.iniciar()

    fun reiniciar() = coleccion.reiniciar()

    fun publicarError(causa: Throwable) = coleccion.publicarError(causa)

    fun getByVehiculoId(vehiculoId: Int): Presupuesto? =
        coleccion.valor.find { it.vehiculoId == vehiculoId }

    /** Crea o reemplaza los tratamientos. No hace nada si el presupuesto ya está aprobado. */
    fun guardar(vehiculoId: Int, tratamientos: List<Tratamiento>) {
        val existente = getByVehiculoId(vehiculoId)
        if (existente?.aprobado == true) return
        val guardado = existente?.copy(tratamientos = tratamientos)
            ?: Presupuesto(
                id = coleccion.siguienteId(),
                vehiculoId = vehiculoId,
                tratamientos = tratamientos,
                fechaCreacion = hoy()
            )
        coleccion.guardar(guardado)
        NotificadorCorreo.presupuestoGenerado(vehiculoId, guardado)
    }

    /** Marca el presupuesto como aprobado y genera el checklist del mecánico con sus tratamientos. */
    fun aprobar(vehiculoId: Int) {
        val presupuesto = getByVehiculoId(vehiculoId) ?: return
        if (presupuesto.aprobado) return
        coleccion.guardar(presupuesto.copy(aprobado = true, fechaAprobacion = hoy()))
        TareaRepository.insertChecklist(presupuesto.tratamientos.map { Tarea(vehiculoId = vehiculoId, descripcion = it.nombre) })
    }

    fun fallasCotizadas(vehiculoId: Int): Set<Int> =
        getByVehiculoId(vehiculoId)?.tratamientos.orEmpty().mapNotNull { it.fallaId }.toSet()

    fun agregarAdicional(vehiculoId: Int, descripcion: String, fallaId: Int? = null): Boolean {
        val presupuesto = getByVehiculoId(vehiculoId)
        if (presupuesto == null || !presupuesto.aprobado) return false
        val tareaId = TareaRepository.insertSinValidar(Tarea(vehiculoId = vehiculoId, descripcion = descripcion))
        val adicional = Adicional(
            id = siguienteAdicionalId(), descripcion = descripcion, tareaId = tareaId, fallaId = fallaId, fecha = hoy()
        )
        coleccion.guardar(presupuesto.copy(adicionales = presupuesto.adicionales + adicional))
        return true
    }

    fun responderAdicional(vehiculoId: Int, adicionalId: Int, precio: Double?, aprobado: Boolean) {
        val presupuesto = getByVehiculoId(vehiculoId) ?: return
        val adicional = presupuesto.adicionales.find { it.id == adicionalId } ?: return
        if (adicional.estado != EstadoAdicional.PENDIENTE) return
        if (aprobado && (precio == null || precio <= 0)) return
        val nuevo = adicional.copy(
            precio = precio,
            estado = if (aprobado) EstadoAdicional.APROBADO else EstadoAdicional.RECHAZADO
        )
        coleccion.guardar(presupuesto.copy(adicionales = presupuesto.adicionales.map { if (it.id == adicionalId) nuevo else it }))
        if (!aprobado) TareaRepository.delete(adicional.tareaId)
    }

    fun tareasBloqueadas(vehiculoId: Int): Set<Int> =
        getByVehiculoId(vehiculoId)?.adicionalesPendientes.orEmpty().map { it.tareaId }.toSet()

    fun quitarFalla(vehiculoId: Int, fallaId: Int): Boolean {
        val presupuesto = getByVehiculoId(vehiculoId) ?: return true
        val adicional = presupuesto.adicionales.find { it.fallaId == fallaId }
        if (adicional != null) {
            if (adicional.estado == EstadoAdicional.APROBADO) return false
            if (adicional.estado == EstadoAdicional.PENDIENTE) TareaRepository.delete(adicional.tareaId)
            coleccion.guardar(presupuesto.copy(adicionales = presupuesto.adicionales.filter { it.id != adicional.id }))
            return true
        }
        if (presupuesto.tratamientos.none { it.fallaId == fallaId }) return true
        if (presupuesto.aprobado) return false
        val restantes = presupuesto.tratamientos.filter { it.fallaId != fallaId }
        if (restantes.isEmpty()) coleccion.eliminar(presupuesto.id)
        else coleccion.guardar(presupuesto.copy(tratamientos = restantes))
        return true
    }

    fun deleteByVehiculoId(vehiculoId: Int) = coleccion.eliminarDonde { it.vehiculoId == vehiculoId }

    private fun siguienteAdicionalId(): Int {
        val existente = coleccion.valor.flatMap { it.adicionales }.maxOfOrNull { it.id } ?: 0
        ultimoAdicionalId = maxOf(ultimoAdicionalId, existente) + 1
        return ultimoAdicionalId
    }

    private fun hoy() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    private fun Presupuesto.aMapa(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
        "vehiculoId" to vehiculoId,
        "tratamientos" to tratamientos.map {
            mapOf("id" to it.id, "nombre" to it.nombre, "precio" to it.precio, "fallaId" to it.fallaId)
        },
        "fechaCreacion" to fechaCreacion,
        "aprobado" to aprobado,
        "fechaAprobacion" to fechaAprobacion,
        "adicionales" to adicionales.map {
            mapOf(
                "id" to it.id,
                "descripcion" to it.descripcion,
                "tareaId" to it.tareaId,
                "fallaId" to it.fallaId,
                "precio" to it.precio,
                "estado" to it.estado.name,
                "fecha" to it.fecha
            )
        }
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.aPresupuesto(): Presupuesto? {
        val presupuestoId = id.toIntOrNull() ?: return null
        val tratamientos = (get("tratamientos") as? List<Map<String, Any?>>).orEmpty().mapNotNull {
            Tratamiento(
                id = (it["id"] as? Number)?.toInt() ?: return@mapNotNull null,
                nombre = it["nombre"] as? String ?: "",
                precio = (it["precio"] as? Number)?.toDouble() ?: 0.0,
                fallaId = (it["fallaId"] as? Number)?.toInt()
            )
        }
        val adicionales = (get("adicionales") as? List<Map<String, Any?>>).orEmpty().mapNotNull {
            Adicional(
                id = (it["id"] as? Number)?.toInt() ?: return@mapNotNull null,
                descripcion = it["descripcion"] as? String ?: "",
                tareaId = (it["tareaId"] as? Number)?.toInt() ?: return@mapNotNull null,
                fallaId = (it["fallaId"] as? Number)?.toInt(),
                precio = (it["precio"] as? Number)?.toDouble(),
                estado = runCatching { EstadoAdicional.valueOf(it["estado"] as? String ?: "") }
                    .getOrDefault(EstadoAdicional.PENDIENTE),
                fecha = it["fecha"] as? String ?: ""
            )
        }
        return Presupuesto(
            id = presupuestoId,
            vehiculoId = getLong("vehiculoId")?.toInt() ?: return null,
            tratamientos = tratamientos,
            fechaCreacion = getString("fechaCreacion") ?: "",
            aprobado = getBoolean("aprobado") ?: false,
            fechaAprobacion = getString("fechaAprobacion") ?: "",
            adicionales = adicionales
        )
    }
}
