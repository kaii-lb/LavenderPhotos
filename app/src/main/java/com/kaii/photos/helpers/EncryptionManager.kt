package com.kaii.photos.helpers

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val TAG = "com.kaii.photos.helpers.EncryptionManager"

object EncryptionManager {
    private const val KEY_SIZE = 256
    private const val KEYSTORE = "AndroidKeyStore"

    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val KEY_NAME = "LavenderPhotosSecureFolderKey"

    private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

    // the keystore key never changes once made, so cache it instead of a keystore round-trip per decrypt
    @Volatile
    private var cachedKey: SecretKey? = null

    private fun getOrCreateSecretKey(): SecretKey {
        cachedKey?.let { return it }

        return synchronized(this) {
            // double-checked: another thread may have populated the cache while we waited on the lock
            cachedKey?.let { return@synchronized it }

            val keystore = KeyStore.getInstance(KEYSTORE)
            keystore.load(null)

            // if key already exists just reuse it, otherwise create it once
            val key = (keystore.getKey(KEY_NAME, null) as? SecretKey) ?: run {
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

                keyGenerator.generateKey()
            }

            cachedKey = key
            key
        }
    }

    private fun writeToOutputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        fileSize: Long,
        cipher: Cipher,
        progress: (progress: Float) -> Unit
    ) {
        val inputStream = inputStream.buffered()

        val bufferSize = 1024 * 32
        val inputBuffer = ByteArray(bufferSize)
        val outputBuffer = ByteArray(cipher.getOutputSize(bufferSize))

        var totalBytesRead = 0L

        inputStream.use { input ->
            outputStream.use { output ->
                var read = -1
                while (input.read(inputBuffer).also { read = it } != -1) {
                    val processedByteCount = cipher.update(inputBuffer, 0, read, outputBuffer, 0)

                    if (processedByteCount > 0) {
                        output.write(outputBuffer, 0, processedByteCount)
                    }

                    totalBytesRead += read
                    if (fileSize > 0) {
                        val progress = totalBytesRead.toFloat() / fileSize
                        progress(progress)
                    }
                }

                try {
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        output.write(finalBytes)
                    }

                    progress(1f)
                } catch (e: Exception) {
                    Log.e(TAG, "Cipher finalization failed", e)
                    e.printStackTrace()
                }
            }
        }
    }

    fun encryptInputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        fileSize: Long
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        writeToOutputStream(
            inputStream = inputStream,
            outputStream = outputStream,
            fileSize = fileSize,
            cipher = cipher
        ) {}

        return cipher.iv
    }

    fun decryptInputStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        fileSize: Long,
        iv: ByteArray
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        writeToOutputStream(
            inputStream = inputStream,
            outputStream = outputStream,
            fileSize = fileSize,
            cipher = cipher
        ) {}
    }

    private fun writeToByteArray(
        bytes: ByteArray,
        cipher: Cipher
    ): ByteArray {
        val inputStream = bytes.inputStream()
        val outputStream = ByteArrayOutputStream()

        val buffer = ByteArray(cipher.blockSize * 1024 * 32)

        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            val processedBytes = cipher.update(buffer, 0, read)
            processedBytes?.let {
                outputStream.write(it)
            }
        }

        try {
            val finalBytes = cipher.doFinal()
            finalBytes?.let {
                outputStream.write(it)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            e.printStackTrace()
        } finally {
            inputStream.close()
            outputStream.close()
        }

        return outputStream.toByteArray()
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

    /**
     * Decrypt only the first 16-byte plaintext block of a CBC stream, without running doFinal
     * (so no padding handling / full-file read). Used by IV recovery to cheaply sample the
     * first plaintext block of a known-good sibling file.
     *
     * @return the first 16 plaintext bytes, or null if there isn't enough ciphertext.
     */
    fun decryptFirstBlock(cipherBytes: ByteArray, iv: ByteArray): ByteArray? {
        if (cipherBytes.size < 32 || iv.size != 16) return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        // feed two blocks: CBC.update buffers a trailing block for padding, so 32 bytes in
        // yields at least the first decrypted block out
        val out = cipher.update(cipherBytes.copyOfRange(0, 32))
        return if (out != null && out.size >= 16) out.copyOfRange(0, 16) else null
    }

    /**
     * Known first 16 plaintext bytes of every PNG file: signature + IHDR chunk header.
     * Used by [SecureIvRecovery] as a candidate first-block when recovering a lost IV.
     *
     *     89 50 4E 47 0D 0A 1A 0A  00 00 00 0D 49 48 44 52
     * (8-byte signature + 4-byte IHDR length=13 + "IHDR")
     */
    val KNOWN_PNG_FIRST_BLOCK = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
        0x00, 0x00, 0x00, 0x0D,                                     // IHDR chunk length (13)
        0x49, 0x48, 0x44, 0x52                                      // "IHDR"
    )

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

        // refuse invalid IVs early: the Android Keystore rejects short / all-zero IVs with
        // InvalidAlgorithmParameterException on Cipher.init(), which is a hard crash.
        // this is the last defence line — callers should validate before reaching here.
        if (iv.size != 16 || iv.all { it.toInt() == 0 }) {
            throw IllegalArgumentException("IV for $absolutePath is invalid (size=${iv.size}, allZero=${iv.all { it == 0.toByte() }})")
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        val inputStream = original.inputStream()
        val outputStream = destination.outputStream()

        writeToOutputStream(
            inputStream = inputStream,
            outputStream = outputStream,
            fileSize = original.length(),
            cipher = cipher
        ) {
            progress(it)
        }

        progress(1f)

        return destination
    }
}
