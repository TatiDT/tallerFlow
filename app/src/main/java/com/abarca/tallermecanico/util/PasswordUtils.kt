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

    fun hash(password: String): String {
        val sal = ByteArray(BYTES_SAL).also { SecureRandom().nextBytes(it) }
        return "${aHex(sal)}:${aHex(derivar(password, sal))}"
    }

    fun verify(plain: String, almacenado: String): Boolean {
        val partes = almacenado.split(":")
        if (partes.size != 2) return false
        val sal = deHex(partes[0]) ?: return false
        val esperado = deHex(partes[1]) ?: return false
        return MessageDigest.isEqual(derivar(plain, sal), esperado)
    }

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
