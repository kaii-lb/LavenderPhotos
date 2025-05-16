package com.aks_labs.tulsi.helpers

import android.content.Context
import android.util.Log
import com.aks_labs.tulsi.MainActivity.Companion.applicationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Files
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val TAG = "DATA_AND_BACKUP"

class DataAndBackupHelper {
    companion object {
        private const val EXPORT_DIR = "Exports"
        private const val UNENCRYPTED_DIR = "Lavender_Photos_Secure_Folder_Export"
        private const val RAW_DIR = "Lavender_Photos_Secure_Folder_Export_Raw"
        private const val ZIP_NAME = "Lavender_Photos_Secure_Folder_Backup"
        private const val FAV_DIR = "Lavender_Photos_Favourites_Export"
        private const val FAV_ZIP = "Lavender_Photos_Favourites_Backup"
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

    fun getFavExportDir(context: Context) =
        File(
            File(context.appStorageDir, EXPORT_DIR),
            FAV_DIR + "_taken_" + getCurrentDate() + ".zip"
        )

    fun getFavZipFile(context: Context) =
        File(
            File(context.appStorageDir, EXPORT_DIR),
            FAV_ZIP + "_taken_" + getCurrentDate() + ".zip"
        )

    /** takes items in [AppDirectories.SecureFolder], decrypts them, and copies them to [getUnencryptedExportDir] */
    fun exportUnencryptedSecureFolderItems(context: Context): Boolean {
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
    ): Boolean {
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
    ): Boolean {
        val secureFolder = File(context.appSecureFolderDir)

        val securedItems = secureFolder.listFiles()

        if (securedItems == null) {
            Log.d(TAG, "Secured items was null, nothing to export.")
            return false
        }

        val fileOutputStream = getZipFile(context = context).outputStream()
        val zipOutputStream = ZipOutputStream(fileOutputStream)

        zipOutputStream.setLevel(Deflater.BEST_COMPRESSION)

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

    suspend fun exportFavourites(context: Context) {
        val database = applicationDatabase.favouritedItemEntityDao()
        val exportDir = getFavExportDir(context = context)

        val items = database.getAll().first()

        items.forEach { favItem ->
            val favFile = File(favItem.absolutePath)

            val destination = File(exportDir, favFile.name)
            if (!destination.exists()) favFile.copyTo(destination)
        }
    }

    suspend fun exportFavouritesToZipFile(
        context: Context,
        progress: (percentage: Float) -> Unit
    ) {
        val database = applicationDatabase.favouritedItemEntityDao()

        val fileOutputStream = getZipFile(context = context).outputStream()
        val zipOutputStream = ZipOutputStream(fileOutputStream)

        zipOutputStream.setLevel(Deflater.BEST_COMPRESSION)

        val items = database.getAll().first().map { File(it.absolutePath) }

        val totalSize = items.sumOf { it.length() }.toFloat()
        var currentProgress = 0f

        items.forEach { favFile ->
            try {
                val entry = ZipEntry(favFile.name)
                zipOutputStream.putNextEntry(entry)

                val buffer = ByteArray(1024 * 32)
                var read = 0
                val inputStream = favFile.inputStream()

                while (read > -1) {
                    read = inputStream.read(buffer)

                    zipOutputStream.write(buffer)
                    currentProgress += buffer.size / totalSize
                    progress(currentProgress)
                }

                zipOutputStream.closeEntry()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed exporting ${favFile.name} to favourites zip file")
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }
        }

        withContext(Dispatchers.IO) {
            zipOutputStream.close()
            fileOutputStream.close()
        }
    }
}

private fun File.listMediaFiles() = listFiles { dir, name ->
    val file = File(dir, name)
    val mimeType = Files.probeContentType(file.toPath())

    mimeType.startsWith("image") || mimeType.startsWith("video")
}


