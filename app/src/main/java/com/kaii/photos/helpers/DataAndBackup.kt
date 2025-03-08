package com.kaii.photos.helpers

import android.content.Context
import android.util.Log
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Files

private const val TAG = "DATA_AND_BACKUP"

class DataAndBackupHelper {
    companion object {
        private const val EXPORT_DIR = "Exports"
        private const val UNENCRYPTED_DIR = "Lavender_Photos_Export"
        private const val RAW_DIR = "Lavender_Photos_Export_Raw"
    }

    private fun getCurrentDate() = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format {
                dayOfMonth()
                char('_')
                monthNumber()
                char('_')
                year()
            })

    fun getUnencryptedExportDir(context: Context) =
        File(
            File(context.appStorageDir, EXPORT_DIR),
            UNENCRYPTED_DIR + "_taken_" + getCurrentDate()
        )

    fun getRawExportDir(context: Context) =
    	File(
    	    File(context.appStorageDir, EXPORT_DIR),
    	    RAW_DIR + "_taken_" + getCurrentDate()
    	)

    fun exportUnencryptedSecureFolderItems(context: Context) : Boolean {
        val secureFolder = File(context.appSecureFolderDir)
        val exportDir = getUnencryptedExportDir(context)

        if (!exportDir.exists()) exportDir.mkdirs()

        val securedItems = secureFolder.listFiles { dir, name ->
            val file = File(dir, name)
            val mimeType = Files.probeContentType(file.toPath())

            mimeType.startsWith("image") || mimeType.startsWith("video")
        }

        if (securedItems == null) return false

        val database = applicationDatabase.securedItemEntityDao()

        securedItems.forEach { secureFile ->
            Log.d(TAG, "Trying to decrypt ${secureFile.name}...")

            try {
                val decryptedFile = File(exportDir, secureFile.name)
                decryptedFile.createNewFile()
                val iv = database.getIvFromSecuredPath(secureFile.absolutePath)
                if (iv == null) throw Exception("IV for ${secureFile.name} was null, cannot decrypt.")

                EncryptionManager.decryptInputStream(
                    inputStream = secureFile.inputStream(),
                    outputStream = decryptedFile.outputStream(),
                    iv = iv
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Couldn't decrypt ${secureFile.name}")
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }
        }

        return true
    }

    fun exportRawSecureFolderItems(context: Context) {
    	val secureFolder = File(context.appSecureFolderDir)
    	val exportDir = getRawExportDir(context)

    	if (!exportDir.exists()) exportDir.mkdirs()

    	val securedItems = secureFolder.listFiles { dir, name ->
    	    val file = File(dir, name)
    	    val mimeType = Files.probeContentType(file.toPath())

    	    mimeType.startsWith("image") || mimeType.startsWith("video")
    	}

    	if (securedItems == null) return

    	securedItems.forEach { secureFile ->
			val decryptedFile = File(exportDir, secureFile.name)
			if (!decryptedFile.exists())  secureFile.copyTo(decryptedFile)
    	}
    }
}
