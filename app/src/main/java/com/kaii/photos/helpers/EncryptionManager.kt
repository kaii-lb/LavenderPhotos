package com.kaii.photos.helpers

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.OutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val TAG = "ENCRYPTION_MANAGER"

class EncryptionManager {
    companion object {
        private const val KEY_SIZE = 128
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_NAME = "LavenderPhotosSecureFolderKey"

        private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
    }

    private fun getOrCreateSecretKey() : SecretKey {
        val keystore = KeyStore.getInstance(KEYSTORE)
        keystore.load(null)

        // if key already exists just return it
        keystore.getKey(KEY_NAME, null)?.let { possiblePreviousKey ->
            return possiblePreviousKey as SecretKey
        }

        val keyGenParams = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
        )
        .setBlockModes(ENCRYPTION_BLOCK_MODE)
        .setEncryptionPaddings(ENCRYPTION_PADDING)
        .setUserAuthenticationRequired(false)
        .setKeySize(KEY_SIZE)
        .setInvalidatedByBiometricEnrollment(false)
        .setRandomizedEncryptionRequired(true)
        .build()

        val keyGenerator = KeyGenerator.getInstance(
            ENCRYPTION_ALGORITHM,
            KEYSTORE
        )
        keyGenerator.init(keyGenParams)

        return keyGenerator.generateKey()
    }

    fun encryptFile(bytes: ByteArray) : ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedBytes = cipher.doFinal(bytes)
        val iv = cipher.iv

        val encryptedDataWithIv = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, encryptedDataWithIv, iv.size, encryptedBytes.size)

        return encryptedDataWithIv
    }

    fun decryptBytes(encryptedDataWithIv: ByteArray): ByteArray? {
    	val cipher = Cipher.getInstance(TRANSFORMATION)

		val iv = encryptedDataWithIv.copyOfRange(0, cipher.blockSize)

    	cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

    	val encryptedBytes = encryptedDataWithIv.copyOfRange(cipher.blockSize, encryptedDataWithIv.size)

		try {
			return cipher.doFinal(encryptedBytes)
		} catch (e: Throwable) {
			Log.d(TAG, e.toString())
			e.printStackTrace()

			return null
		}
    }
}
