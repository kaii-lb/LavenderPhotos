package com.kaii.photos.helpers

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

private const val TAG = "ENCRYPTION_MANAGER"

class EncryptionManager {
    companion object {
        private const val KEY_SIZE = 256
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
		inputStream: FileInputStream,
		outputStream: FileOutputStream,
		cipher: Cipher
	) {
        val buffer = ByteArray(cipher.blockSize * 1024 * 32)

		var read = 0
        while (read > -1) {
            read = inputStream.read(buffer)

            cipher.update(buffer)?.let {
            	outputStream.write(it)
            	Log.d(TAG, "WRITING DATA $read >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            }
        }

        val remaining = inputStream.readBytes()
        if (remaining.size > 0) {
	        val lastChunk = cipher.doFinal(remaining)
	        outputStream.write(lastChunk)
        }

        outputStream.close()
        inputStream.close()
	}

    fun encryptInputStream(
    	inputStream: FileInputStream,
    	outputStream: FileOutputStream
    ) : ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
       	cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

		writeToOutputStream(
			inputStream = inputStream,
			outputStream = outputStream,
			cipher = cipher
		)

        return cipher.iv
    }

    fun decryptInputStream(
    	inputStream: FileInputStream,
    	outputStream: FileOutputStream,
    	iv: ByteArray
   	) {
    	val cipher = Cipher.getInstance(TRANSFORMATION)
    	cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

		writeToOutputStream(
			inputStream = inputStream,
			outputStream = outputStream,
			cipher = cipher
		)
    }

    fun decryptBytes(inputStream: FileInputStream, iv: ByteArray) : ByteArray {
    	val cipher = Cipher.getInstance(TRANSFORMATION)
    	cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

    	return cipher.doFinal(inputStream.readBytes())
    }

    fun decryptVideo(absolutePath: String, iv: ByteArray, context: Context) : File {
        val original = File(absolutePath)
        val destination = File(context.appSecureVideoCacheDir, original.name)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))

        val inputStream = original.inputStream()
        val outputStream = destination.outputStream()

		writeToOutputStream(
			inputStream = inputStream,
			outputStream = outputStream,
			cipher = cipher
		)

        return destination
    }
}

@UnstableApi
class EncryptedDataSourceFactory(
    private val absolutePath: String,
    private val iv: ByteArray
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        val decryptCipher = Cipher.getInstance(EncryptionManager.TRANSFORMATION)
        decryptCipher.init(Cipher.DECRYPT_MODE, EncryptionManager().getOrCreateSecretKey(), IvParameterSpec(iv))

        return EncryptedDataSource(absolutePath, decryptCipher)
    }
}

@UnstableApi
class EncryptedDataSource(
    private val absolutePath: String,
    private val decryptCipher: Cipher
) : DataSource {
    private var inputStream: InputStream? = null
    private var uri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        val file = File(absolutePath)
        inputStream = file.inputStream()
        uri = dataSpec.uri

        val length =
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                file.length() - dataSpec.position
            } else dataSpec.length

        inputStream?.skip(dataSpec.position)

        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val encryptedBytes = ByteArray(length)

        val bytesRead = inputStream?.read(encryptedBytes, 0, length) ?: return C.RESULT_END_OF_INPUT

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        val decryptedBytes = decryptCipher.update(encryptedBytes, 0, bytesRead) ?: return C.RESULT_END_OF_INPUT

        System.arraycopy(decryptedBytes, 0, buffer, offset, decryptedBytes.size)
        return decryptedBytes.size
    }

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun close() {
        inputStream?.close()
        inputStream = null
    }

    override fun getUri(): Uri? = uri
}
