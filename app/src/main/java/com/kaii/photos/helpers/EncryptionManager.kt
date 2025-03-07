package com.kaii.photos.helpers

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val TAG = "ENCRYPTION_MANAGER"

object EncryptionManager {
    private const val KEY_SIZE = 256
    private const val KEYSTORE = "AndroidKeyStore"

    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val KEY_NAME = "LavenderPhotosSecureFolderKey"

    private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

    private fun getOrCreateSecretKey(): SecretKey {
        val keystore = KeyStore.getInstance(KEYSTORE)
        keystore.load(null)

        // if key already exists just return it
        keystore.getKey(KEY_NAME, null)?.let { possiblePreviousKey ->
            return possiblePreviousKey as SecretKey
        }

        val keyGenParams =
            KeyGenParameterSpec.Builder(
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

    private fun writeToOutputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        cipher: Cipher,
        progress: (progress: Float) -> Unit
    ) {
        val totalLength = if (inputStream.available() != 0) inputStream.available().toFloat() else 1f
        var currentProgress = 0f

        val buffer = ByteArray(cipher.blockSize * 1024 * 32)

        var read = 0
        while (read > -1) {
            read = inputStream.read(buffer)

            cipher.update(buffer)?.let {
                currentProgress += it.size / totalLength
                progress(currentProgress)

                outputStream.write(it)
                Log.d(TAG, "Progress: $currentProgress, writing data of size $read bytes >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            }
        }

        val remaining = inputStream.readBytes()
        if (remaining.isNotEmpty()) {
            val lastChunk = cipher.doFinal(remaining)
            outputStream.write(lastChunk)
        }

        outputStream.close()
        inputStream.close()
    }

    fun encryptInputStream(
        inputStream: InputStream,
        outputStream: OutputStream
    ): ByteArray {
    	val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        writeToOutputStream(
            inputStream = inputStream,
            outputStream = outputStream,
            cipher = cipher
        ) {}

        return cipher.iv
    }

    fun decryptInputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        iv: ByteArray
    ) {
    	val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        writeToOutputStream(
            inputStream = inputStream,
            outputStream = outputStream,
            cipher = cipher
        ) {}
    }

    private fun writeToByteArray(
        bytes: ByteArray,
        cipher: Cipher
    ): ByteArray {
        val buffer = ByteArray(cipher.blockSize * 1024 * 32)

        var read = 0
        val inputStream = bytes.inputStream()
        val output = mutableListOf<Byte>()

        while (read > -1) {
            read = inputStream.read(buffer)

            cipher.update(buffer)?.let {
                output.addAll(it.toList())
            }
        }

        val remaining = inputStream.readBytes()
        if (remaining.isNotEmpty()) {
            val lastChunk = cipher.doFinal(remaining)
            output.addAll(lastChunk.toList())
        }
        inputStream.close()

        return output.toTypedArray().toByteArray()
    }

    fun decryptBytes(bytes: ByteArray, iv: ByteArray): ByteArray {
    	val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        val decrypted = writeToByteArray(
        	bytes = bytes,
        	cipher = cipher
       	)

        return decrypted
    }

    /** return the encrypted byte array and an iv as the second param */
    fun encryptBytes(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    	val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encrypted = writeToByteArray(
        	bytes = bytes,
        	cipher = cipher
       	)

        return Pair(encrypted, cipher.iv)
    }

    fun decryptVideo(
        absolutePath: String,
        iv: ByteArray,
        context: Context,
        progress: (progress: Float) -> Unit
    ): File {
    	Log.d(TAG, "trying to decrypt video $absolutePath")

        val original = File(absolutePath)
        val destination = getSecureDecryptedVideoFile(
        	name = original.name,
        	context = context
       	)

		val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        val inputStream = original.inputStream()
        val outputStream = destination.outputStream()

        writeToOutputStream(
            inputStream = inputStream,
            outputStream = outputStream,
            cipher = cipher
        ) {
            progress(it)
        }

        return destination
    }
}
