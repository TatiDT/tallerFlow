package com.abarca.tallermecanico.util

import android.util.Log
import com.abarca.tallermecanico.data.entity.Cliente
import com.abarca.tallermecanico.data.entity.EstadoVehiculo
import com.abarca.tallermecanico.data.entity.Presupuesto
import com.abarca.tallermecanico.data.entity.Vehiculo
import com.abarca.tallermecanico.data.repository.ClienteRepository
import com.abarca.tallermecanico.data.repository.VehiculoRepository

object NotificadorCorreo {

    fun vehiculoIngresado(vehiculo: Vehiculo) {
        enviar(vehiculo, "Recibimos tu vehículo ${vehiculo.patente}") { cliente ->
            "Hola ${cliente.nombre},\n\n" +
                "Confirmamos el ingreso de tu ${descripcion(vehiculo)} al taller" +
                (if (vehiculo.fechaIngreso.isNotBlank()) " el ${vehiculo.fechaIngreso}" else "") + ".\n" +
                "Te avisaremos cuando haya novedades."
        }
    }

    fun estadoCambiado(vehiculo: Vehiculo) {
        when (vehiculo.estado) {
            EstadoVehiculo.LISTO -> enviar(vehiculo, "Tu vehículo ${vehiculo.patente} está listo") { cliente ->
                "Hola ${cliente.nombre},\n\n" +
                    "Tu ${descripcion(vehiculo)} está listo para ser retirado en el taller."
            }
            EstadoVehiculo.ENTREGADO -> enviar(vehiculo, "Entrega del vehículo ${vehiculo.patente}") { cliente ->
                "Hola ${cliente.nombre},\n\n" +
                    "Registramos la entrega de tu ${descripcion(vehiculo)}. Gracias por confiar en nuestro taller."
            }
            else -> Unit
        }
    }

    fun presupuestoGenerado(vehiculoId: Int, presupuesto: Presupuesto) {
        val vehiculo = VehiculoRepository.getById(vehiculoId) ?: return
        if (presupuesto.tratamientos.isEmpty()) return
        enviar(vehiculo, "Presupuesto de tu vehículo ${vehiculo.patente}") { cliente ->
            val detalle = presupuesto.tratamientos.joinToString("\n") { "- ${it.nombre}: ${Formato.pesos(it.precio)}" }
            "Hola ${cliente.nombre},\n\n" +
                "Este es el presupuesto para tu ${descripcion(vehiculo)}:\n\n" +
                detalle + "\n\n" +
                "Neto: ${Formato.pesos(presupuesto.neto)}\n" +
                "IVA: ${Formato.pesos(presupuesto.iva)}\n" +
                "Total: ${Formato.pesos(presupuesto.total)}\n\n" +
                "Contáctanos para aprobarlo."
        }
    }

    private fun enviar(vehiculo: Vehiculo, asunto: String, cuerpo: (Cliente) -> String) {
        val cliente = ClienteRepository.getById(vehiculo.clienteId)
        if (cliente == null) {
            Log.d("Correo", "Omitido: cliente ${vehiculo.clienteId} no encontrado")
            return
        }
        if (cliente.email.isBlank()) {
            Log.d("Correo", "Omitido: cliente ${cliente.id} sin email")
            return
        }
        Log.d("Correo", "Enviando '$asunto' a ${cliente.email}")
        Correo.enviar(cliente.email, asunto, cuerpo(cliente))
    }

    private fun descripcion(vehiculo: Vehiculo) =
        "${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.patente})"
}
