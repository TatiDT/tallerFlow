package com.abarca.tallermecanico.data.entity

enum class RolUsuario(val display: String) {
    ADMIN("Administración"),
    JEFE_TALLER("Jefe de Taller"),
    MECANICO("Mecánico"),
    REPUESTO("Encargado Repuesto"),
    DIAGNOSIS("Encargado Diagnosis")
}

data class Usuario(
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val username: String,
    val email: String = "",
    val password: String = "",
    val rol: RolUsuario,
    val activo: Boolean = true
)
