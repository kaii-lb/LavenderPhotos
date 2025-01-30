package com.kaii.photos.helpers

import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toFile
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val TAG = "ENCRYPTION_MANAGER"

class EncryptionManager {
    companion object {
        private const val KEY_SIZE = 128
        private const val KEYSTORE = "AndroidKeyStore"

        const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        const val KEY_NAME = "LavenderPhotosSecureFolderKey"

        const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
    }

    fun getOrCreateSecretKey() : SecretKey {
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

    fun encryptBytes(bytes: ByteArray) : Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
       	cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedBytes = cipher.doFinal(bytes)
//         val iv = cipher.iv
//
//         val encryptedDataWithIv = ByteArray(iv.size + encryptedBytes.size)
//         System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.size)
//         System.arraycopy(encryptedBytes, 0, encryptedDataWithIv, iv.size, encryptedBytes.size)

        return Pair(encryptedBytes, cipher.iv)
    }

    fun decryptBytes(encryptedBytes: ByteArray, iv: ByteArray): ByteArray? {
    	val cipher = Cipher.getInstance(TRANSFORMATION)

    	cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

		try {
			return cipher.doFinal(encryptedBytes)
		} catch (e: Throwable) {
			Log.d(TAG, e.toString())
			e.printStackTrace()

			return null
		}
    }
}

@OptIn(UnstableApi::class)
class EncryptedDataSourceFactory(private val iv: ByteArray) : DataSource.Factory {
    override fun createDataSource(): EncryptedDataSource = EncryptedDataSource(iv)
}

@OptIn(UnstableApi::class)
class EncryptedDataSource(private val iv: ByteArray) : DataSource {
    private var inputStream: EncryptedInputStream? = null
    private lateinit var uri: Uri

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri

        try {
            val file = File(uri.path!!)
            val cipher = Cipher.getInstance(EncryptionManager.TRANSFORMATION)

            val encryptionManager = EncryptionManager()
            val secretKey = encryptionManager.getOrCreateSecretKey()

            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            // inputStream = CipherInputStream(file.inputStream(), cipher)
            inputStream = EncryptedInputStream(
                inputStream = file.inputStream(),
                cipher = cipher,
                secretKey = secretKey,
                ivParameterSpec = IvParameterSpec(iv)
            )

            inputStream?.forceSkip(dataSpec.position)
        } catch (e: Throwable) {
            Log.d(TAG, e.toString())
            e.printStackTrace()
        }

        return dataSpec.length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        if (length == 0 || inputStream == null) {
            0
        } else {
            inputStream!!.read(buffer, offset, length)
        }

    override fun getUri(): Uri = uri

    override fun close() {
        inputStream?.close()
    }
}

class EncryptedInputStream(
    private val inputStream: FileInputStream,
    private val cipher: Cipher,
    private val secretKey: SecretKey,
    private val ivParameterSpec: IvParameterSpec
) : CipherInputStream(inputStream, cipher) {
    // override fun skip(n: Long): Long {
    //     return forceSkip(n)
    // }

    fun forceSkip(bytesToSkip: Long) : Long {
        val skipped = inputStream.skip(bytesToSkip)

        try {
            val skipOverflow = (bytesToSkip % cipher.blockSize).toInt()
            val skipBlockPosition = bytesToSkip - skipOverflow

            val blockNumber = skipBlockPosition / cipher.blockSize
            val ivOffset = BigInteger(1, ivParameterSpec.iv).add(
                BigInteger.valueOf(blockNumber)
            )

            val ivOffsetBytes = ivOffset.toByteArray()

            val skippedIvSpec = if (ivOffsetBytes.size < cipher.blockSize) {
                val resizeIvOffsetBytes = ByteArray(cipher.blockSize)

                System.arraycopy(
                    ivOffsetBytes,
                    0,
                    resizeIvOffsetBytes,
                    cipher.blockSize - ivOffsetBytes.size,
                    ivOffsetBytes.size
                )

                IvParameterSpec(resizeIvOffsetBytes)
            } else {
                IvParameterSpec(
                    ivOffsetBytes,
                    ivOffsetBytes.size - cipher.blockSize,
                    cipher.blockSize
                )
            }

            cipher.init(Cipher.DECRYPT_MODE, secretKey, skippedIvSpec)
            val skipBuffer = ByteArray(skipOverflow)
            cipher.update(skipBuffer, 0, skipOverflow, skipBuffer)
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()
        }

        return skipped
    }

    override fun available(): Int = inputStream.available()
}
