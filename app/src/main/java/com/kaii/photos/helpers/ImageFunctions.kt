package com.kaii.photos.helpers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.database.entities.SecuredItemEntity
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

fun shareImage(uri: Uri, context: Context) {
    val contentResolver = context.contentResolver

	CoroutineScope(Dispatchers.IO).launch {
	    val mimeType = contentResolver.getType(uri)

	    val shareIntent = Intent().apply {
	        action = Intent.ACTION_SEND
	        type = mimeType
	    }

	    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)

	    val chooserIntent = Intent.createChooser(shareIntent, null)
	    context.startActivity(chooserIntent)
	}
}

fun moveImageToLockedFolder(absolutePath: String, id: Long, context: Context) {
    val fileToBeHidden = File(absolutePath)
    val lockedFolderDir = context.getAppLockedFolderDirectory()
    val copyToPath = lockedFolderDir + fileToBeHidden.name
    Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.REPLACE_EXISTING)

    val lastModified = System.currentTimeMillis()
    CoroutineScope(EmptyCoroutineContext + CoroutineName("hide_file_context")).launch {
        Path(copyToPath).setAttribute(
            BasicFileAttributes::lastModifiedTime.name,
            FileTime.fromMillis(lastModified)
        )

        applicationDatabase.securedItemEntityDao().insertEntity(
            SecuredItemEntity(
                originalPath = absolutePath,
                securedPath = copyToPath
            )
        )
        applicationDatabase.mediaEntityDao().deleteEntityById(id)
    }
    File(copyToPath).lastModified()
}

/** @param path is the secured folder path (/data/ path) to this item */
fun moveImageOutOfLockedFolder(path: String) {
    val fileToBeRevived = File(path)
    val absolutePath = fileToBeRevived.absolutePath

	CoroutineScope(Dispatchers.IO).launch {
	    val reverseCemetery =
	        applicationDatabase.securedItemEntityDao().getOriginalPathFromSecuredPath(path)
	            ?: (getAppRestoredFromLockedFolderDirectory() + fileToBeRevived.name)

	    Files.move(Path(absolutePath), Path(reverseCemetery), StandardCopyOption.REPLACE_EXISTING)

	    val lastModified = System.currentTimeMillis()
	    Path(reverseCemetery).setAttribute(
	        BasicFileAttributes::lastModifiedTime.name,
	        FileTime.fromMillis(lastModified)
	    )
	    File(reverseCemetery).lastModified()
	    applicationDatabase.securedItemEntityDao().deleteEntityBySecuredPath(path)
	}
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
                val path = destination.removeSuffix("/") + "/"
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
                val inputStream = contentResolver.openInputStream(it.uri)
                val outputStream = File(path).outputStream()

                inputStream?.copyTo(outputStream)

                outputStream.close()
                inputStream?.close()
            }
        }
    }
}
