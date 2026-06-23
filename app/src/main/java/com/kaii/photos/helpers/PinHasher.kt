package com.kaii.photos.helpers

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinHasher {
    companion object {
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH = 256
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    fun hashNewPin(pin: CharArray): Pair<ByteArray, ByteArray> {
        val salt = generateSalt()
        val hash = hashPinWithSalt(pin, salt)
        return Pair(hash, salt)
    }

    fun hashPinWithSalt(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)

        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }
}