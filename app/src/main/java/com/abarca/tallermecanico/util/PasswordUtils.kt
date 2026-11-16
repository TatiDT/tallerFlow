package com.abarca.tallermecanico.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtils {
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"
    private const val ITERACIONES = 120_000
    private const val BYTES_SAL = 16
    private const val BITS_CLAVE = 256
    private const val VERSION = "2"

    data class ClaveNueva(val almacenado: String, val verificador: String)

    fun nueva(password: String): ClaveNueva {
        val salHex = aHex(ByteArray(BYTES_SAL).also { SecureRandom().nextBytes(it) })
        val verificador = checkNotNull(hashConSal(password, salHex))
        return ClaveNueva("$VERSION:$salHex:${sha256Hex(parteSecreta(verificador))}", verificador)
    }

    fun verify(plain: String, almacenado: String): Boolean {
        val sal = salDe(almacenado) ?: return false
        val verificador = hashConSal(plain, sal) ?: return false
        val esperado = almacenado.split(":")[2]
        return MessageDigest.isEqual(
            sha256Hex(parteSecreta(verificador)).toByteArray(Charsets.UTF_8),
            esperado.toByteArray(Charsets.UTF_8)
        )
    }

    fun esFormatoActual(almacenado: String): Boolean = salDe(almacenado) != null

    fun salDe(almacenado: String): String? =
        almacenado.split(":")
            .takeIf { it.size == 3 && it[0] == VERSION && deHex(it[1]) != null && it[2].length == 64 }
            ?.get(1)

    fun hashConSal(password: String, salHex: String): String? {
        val sal = deHex(salHex) ?: return null
        return "$salHex:${aHex(derivar(password, sal))}"
    }

    fun claveAcceso(username: String): String = sha256Hex(username.lowercase())

    private fun parteSecreta(verificador: String) = verificador.substringAfter(":")

    private fun sha256Hex(texto: String): String =
        aHex(MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8)))

    private fun derivar(password: String, sal: ByteArray): ByteArray =
        SecretKeyFactory.getInstance(ALGORITMO)
            .generateSecret(PBEKeySpec(password.toCharArray(), sal, ITERACIONES, BITS_CLAVE))
            .encoded

    private fun aHex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private fun deHex(texto: String): ByteArray? {
        if (texto.isEmpty() || texto.length % 2 != 0) return null
        return runCatching { texto.chunked(2).map { it.toInt(16).toByte() }.toByteArray() }.getOrNull()
    }
}
