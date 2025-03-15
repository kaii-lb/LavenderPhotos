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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val TAG = "DATA_AND_BACKUP"

class DataAndBackupHelper {
    companion object {
        private const val EXPORT_DIR = "Exports"
        private const val UNENCRYPTED_DIR = "Lavender_Photos_Export"
        private const val RAW_DIR = "Lavender_Photos_Export_Raw"
        private const val ZIP_NAME = "Lavender_Photos_Backup"
    }

    private fun getCurrentDate() = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format {
            	minute()
            	char('_')
            	hour()
            	char('_')
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

    fun getZipFile(context: Context) =
        File(
            File(context.appStorageDir, EXPORT_DIR),
            ZIP_NAME + "_taken_" + getCurrentDate() + ".zip"
        )

    /** takes items in [AppDirectories.SecureFolder], decrypts them, and copies them to [getUnencryptedExportDir] */
    fun exportUnencryptedSecureFolderItems(context: Context) : Boolean {
        val secureFolder = File(context.appSecureFolderDir)
        val exportDir = getUnencryptedExportDir(context)

        if (!exportDir.exists()) exportDir.mkdirs()

        val securedItems = secureFolder.listMediaFiles()

        if (securedItems == null) {
            Log.d(TAG, "Secured items was null, nothing to export.")
            return false
        }

        val database = applicationDatabase.securedItemEntityDao()

        securedItems.forEach { secureFile ->
            Log.d(TAG, "Trying to decrypt ${secureFile.name}...")

            try {
                val decryptedFile = File(exportDir, secureFile.name)
                decryptedFile.createNewFile()
                val iv = database.getIvFromSecuredPath(secureFile.absolutePath) ?: throw Exception("IV for ${secureFile.name} was null, cannot decrypt.")

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

    /** Takes item in [secureFolder] and copies them as is to [getRawExportDir]
     * return true if it succeeded, false otherwise */
    fun exportRawSecureFolderItems(
        context: Context,
        secureFolder: File = File(context.appSecureFolderDir)
    ) : Boolean {
    	val exportDir = getRawExportDir(context)

    	if (!exportDir.exists()) exportDir.mkdirs()

    	val securedItems = secureFolder.listMediaFiles()

    	if (securedItems == null) {
            Log.d(TAG, "Secured items was null, nothing to export.")
            return false
        }

    	securedItems.forEach { secureFile ->
			val decryptedFile = File(exportDir, secureFile.name)
			if (!decryptedFile.exists()) secureFile.copyTo(decryptedFile)
    	}

        return true
    }

    fun exportSecureFolderToZipFile(
        context: Context,
    ) : Boolean {
        val secureFolder = File(context.appSecureFolderDir)

        val securedItems = secureFolder.listFiles()

        if (securedItems == null) {
            Log.d(TAG, "Secured items was null, nothing to export.")
            return false
        }

        val fileOutputStream = getZipFile(context = context)
        val zipOutputStream = ZipOutputStream(fileOutputStream.outputStream())

        val database = applicationDatabase.securedItemEntityDao()

        try {
            securedItems.forEach { secureFile ->
                Log.d(TAG, "Trying to decrypt ${secureFile.name}...")

                val iv = database.getIvFromSecuredPath(secureFile.absolutePath)

                val bytes =
                    if (iv != null) {
                        EncryptionManager.decryptBytes(
                            bytes = secureFile.readBytes(),
                            iv = iv
                        )
                    } else {
                        secureFile.readBytes()
                    }

                val entry = ZipEntry(secureFile.name)
                zipOutputStream.putNextEntry(entry)
                zipOutputStream.write(bytes)
                zipOutputStream.closeEntry()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Couldn't decrypt files")
            Log.e(TAG, e.toString())
            e.printStackTrace()
        } finally {
            zipOutputStream.close()
        }

        return true
    }
}

private fun File.listMediaFiles() = listFiles { dir, name ->
    val file = File(dir, name)
    val mimeType = Files.probeContentType(file.toPath())

    mimeType.startsWith("image") || mimeType.startsWith("video")
}
