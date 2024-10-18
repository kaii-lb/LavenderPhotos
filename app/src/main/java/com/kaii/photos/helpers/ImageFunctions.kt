package com.kaii.photos.helpers

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.core.net.toUri
import com.kaii.photos.MainActivity
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.setAttribute

private const val TAG = "IMAGE_FUNCTIONS"

enum class ImageFunctions {
    LoadNormalImage,
    LoadTrashedImage,
    LoadSecuredImage
}

fun permanentlyDeletePhotoList(context: Context, list: List<Uri>) {
    val contentResolver = context.contentResolver

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                contentResolver.delete(it, null)
            }

            delay(3000)
        }
	}
}

fun setTrashedOnPhotoList(context: Context, list: List<Uri>, trashed: Boolean) {
    val contentResolver = context.contentResolver

    val trashedValues = ContentValues().apply {
        put(MediaColumns.IS_TRASHED, trashed)
    }

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                contentResolver.update(it, trashedValues, null)
            }

            delay(3000)
        }
    }
}

fun shareImage(absolutePath: String, id: Long, context: Context) {
    val database = MainActivity.applicationDatabase

	CoroutineScope(Dispatchers.IO).launch {
	    val mimeType = database.mediaEntityDao().getMimeType(id) // TODO: replace with contentResovler's method

	    val shareIntent = Intent().apply {
	        action = Intent.ACTION_SEND
	        type = mimeType
	    }

	    shareIntent.putExtra(Intent.EXTRA_STREAM, absolutePath.toUri())

	    val chooserIntent = Intent.createChooser(shareIntent, null)
	    context.startActivity(chooserIntent)
	}
}

fun moveImageToLockedFolder(absolutePath: String, id: Long, context: Context) {
    val fileToBeHidden = File(absolutePath)
    val lockedFolderDir = context.getAppLockedFolderDirectory()
    val copyToPath = lockedFolderDir + fileToBeHidden.name
    Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.REPLACE_EXISTING)

    val database = MainActivity.applicationDatabase

    val lastModified = System.currentTimeMillis()
    CoroutineScope(EmptyCoroutineContext + CoroutineName("hide_file_context")).launch {
        // val entity = database.mediaEntityDao().getFromId(id)

        Path(copyToPath).setAttribute(
            BasicFileAttributes::lastModifiedTime.name,
            FileTime.fromMillis(lastModified)
        )

        // TODO: use a database to store locked folder original paths

        database.mediaEntityDao().deleteEntityById(id)
    }
    File(copyToPath).lastModified()

    //fileToBeHidden.delete()
}

/** @param list is a list of the absolute path of every image to be deleted */
fun permanentlyDeleteSecureFolderImageList(list: List<String>) {
    try {
        list.forEach { path ->
            File(path).delete()
        }
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

fun moveImageOutOfLockedFolder(path: String) {
    val fileToBeRevived = File(path)
    val absolutePath = fileToBeRevived.absolutePath

    val lastModified = System.currentTimeMillis()

    // TODO: use database to track where it was from
    val reverseCemetery = getAppRestoredFromLockedFolderDirectory() + fileToBeRevived.name

    Files.move(Path(absolutePath), Path(reverseCemetery), StandardCopyOption.REPLACE_EXISTING)

    Path(reverseCemetery).setAttribute(
        BasicFileAttributes::lastModifiedTime.name,
        FileTime.fromMillis(lastModified)
    )
    File(reverseCemetery).lastModified()

    fileToBeRevived.delete()
}

fun renameImage(context: Context, uri: Uri, newName: String) {
    val contentResolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaColumns.DISPLAY_NAME, newName)
    }

    contentResolver.update(uri, contentValues, null)
    contentResolver.notifyChange(uri, null)
}

fun renameDirectory(path: String, newName: String) {
    val originalDir = File(path)
    val newDirAbsolutePath = path.replace(originalDir.name, newName)

    try {
        originalDir.renameTo(File(newDirAbsolutePath))
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

/** @param destination where to move said files to, should be relative*/
fun moveImageListToPath(context: Context, list: List<MediaStoreData>, destination: String) {
    val contentResolver = context.contentResolver

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                val fileName = File(it.absolutePath).name
                val path = if (destination.endsWith("/")) destination else destination + "/"
                println("PATH IS $path")
                val contentValues = ContentValues().apply {
                    put(MediaColumns.RELATIVE_PATH, path)
                }
                contentResolver.update(it.uri, contentValues, null)
            }

            delay(3000)
        }
    }
}

/** @param destination where to copy said files to, should be relative*/
fun copyImageListToPath(context: Context, list: List<MediaStoreData>, destination: String) {
    val contentResolver = context.contentResolver

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                val file = File(it.absolutePath)
                val path = "/storage/emulated/0/" + destination.removeSuffix("/") + "/${file.name}"
                println("PATH IS $path")
                val inputStream = contentResolver.openInputStream(it.uri)
                val outputStream = File(path).outputStream()

                inputStream?.copyTo(outputStream)

                outputStream.close()
                inputStream?.close()
            }
        }
    }
}
