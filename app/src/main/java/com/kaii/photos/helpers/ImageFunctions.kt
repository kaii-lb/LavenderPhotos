package com.kaii.photos.helpers

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.kaii.photos.MainActivity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.TrashedItemEntity
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.Collections
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.pathString
import kotlin.io.path.setAttribute

private const val TAG = "IMAGE_FUNCTIONS"

/** Order is very important here
 * bad decisions where made, that's why this exists
 * should really be refactored and fixed but we in too deep */
enum class ImageFunctions {
    ShareImage,
    EditImage,
    TrashImage,
    MoveToLockedFolder,
    UnTrashImage,
    LoadNormalImage,
    LoadTrashedImage,
    LoadSecuredImage,
    PermaDeleteImage,
    SearchImage,
    MoveOutOfLockedFolder,
    RenameImage,
    CopyImage,
    MoveImage
}

fun operateOnImage(
    absolutePath: String,
    id: Long,
    operation: ImageFunctions,
    context: Context,
    extraData: Map<String, Any>? = null
) {
    when (operation) {
        ImageFunctions.TrashImage -> {
            trashPhoto(absolutePath, id)
        }

        ImageFunctions.UnTrashImage -> {
            untrashPhoto(absolutePath, id, context)
        }

        ImageFunctions.PermaDeleteImage -> {
            permanentlyDeletePhoto(absolutePath)
        }

        ImageFunctions.ShareImage -> {
            shareImage(absolutePath, id, context)
        }

        ImageFunctions.MoveToLockedFolder -> {
            moveImageToLockedFolder(absolutePath, id, context)
        }

        ImageFunctions.MoveOutOfLockedFolder -> {
            moveOutOfLockedFolder(absolutePath)
        }

        ImageFunctions.RenameImage -> {
            if (extraData == null) throw Exception("extra data should not be null on renaming an image.")

            renameImage(
                imagePath = absolutePath,
                imageName = extraData["old_name"].toString(),
                newName = extraData["new_name"].toString(),
                context
            )
        }

        ImageFunctions.CopyImage -> {
            extraData?.get("albumPath")?.let { copyToPath(absolutePath, it.toString(), false) }
        }

        ImageFunctions.MoveImage -> {
            extraData?.get("albumPath")?.let { copyToPath(absolutePath, it.toString(), true) }
        }

        else -> {
            Log.d(TAG, "chosen function was ${operation.name}")
        }
    }
}

fun trashPhoto(path: String, id: Long) {
    val fileToBeTrashed = File(path)
    val absolutePath = fileToBeTrashed.absolutePath
    val trashDir = getAppTrashBinDirectory()

    val lastModified = System.currentTimeMillis()
    var copyToPath = trashDir + "trashed-$lastModified-" + fileToBeTrashed.name

    Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.ATOMIC_MOVE)

    val database = MainActivity.applicationDatabase

    CoroutineScope(EmptyCoroutineContext + CoroutineName("delete_file_context")).launch {
        val entity = database.mediaEntityDao().getFromId(id)

        Path(copyToPath).setAttribute(
            BasicFileAttributes::lastModifiedTime.name,
            FileTime.fromMillis(lastModified)
        )

        database.trashedItemEntityDao().deleteEntityByPath(absolutePath)

        database.trashedItemEntityDao().insertEntity(
            TrashedItemEntity(
                absolutePath,
                copyToPath,
                entity.dateTaken,
                entity.mimeType,
                entity.displayName
            )
        )

        Log.d(TAG, "path of trash photo $absolutePath")

        database.favouritedItemEntityDao().deleteEntityById(id)

        database.mediaEntityDao().deleteEntityById(id)
    }
    File(copyToPath).lastModified()

    fileToBeTrashed.delete()
}

private fun untrashPhoto(path: String, id: Long, context: Context) {
    val fileToBeRevived = File(path)
    val absolutePath = fileToBeRevived.absolutePath
    val lockedFolderPath = context.getAppLockedFolderDirectory()

    val database = MainActivity.applicationDatabase
    val lastModified = System.currentTimeMillis()

    try {
        CoroutineScope(EmptyCoroutineContext + CoroutineName("undelete_file_context")).launch {
            val item = database.trashedItemEntityDao().getFromTrashedPath(absolutePath)

            database.mediaEntityDao().insertEntity(
                MediaEntity(
                    id = id,
                    mimeType = item.mimeType,
                    dateTaken = item.dateTaken,
                    displayName = item.displayName
                )
            )

            val reverseCemetery = item.originalPath

            database.trashedItemEntityDao().deleteEntityByPath(absolutePath)

            val moveOp = if (absolutePath.contains(lockedFolderPath)) {
                StandardCopyOption.REPLACE_EXISTING
            } else {
                StandardCopyOption.ATOMIC_MOVE
            }

            Files.move(Path(absolutePath), Path(reverseCemetery), moveOp)

            Path(reverseCemetery).setAttribute(
                BasicFileAttributes::lastModifiedTime.name,
                FileTime.fromMillis(lastModified)
            )

            fileToBeRevived.delete()
        }
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

private fun permanentlyDeletePhoto(absolutePath: String) {
    val fileToBeShredded = File(absolutePath)
    fileToBeShredded.delete()

    CoroutineScope(EmptyCoroutineContext + CoroutineName("permanently_delete_file_context")).launch {
        MainActivity.applicationDatabase.trashedItemEntityDao().deleteEntityByPath(absolutePath)
    }
}

@Composable
fun PermanentlyDeletePhotoList(list: List<Uri>) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

	val coroutineScope = rememberCoroutineScope()
	coroutineScope.launch {
		withContext(Dispatchers.IO) {
		    list.forEach {
		        contentResolver.delete(it, null)
		    }
		}	
	}
}

@Composable
fun SetTrashedOnPhotoList(list: List<Uri>, trashed: Boolean) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val trashedValues = ContentValues().apply {
        put(MediaColumns.IS_TRASHED, trashed)
    }

	val coroutineScope = rememberCoroutineScope()
	coroutineScope.launch {
		withContext(Dispatchers.IO) {
		    list.forEach {
		        contentResolver.update(it, trashedValues, null)
		    }
		}	
	}
}

private fun shareImage(absolutePath: String, id: Long, context: Context) {
    val database = MainActivity.applicationDatabase

    val mimeType = database.mediaEntityDao().getMimeType(id)

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = mimeType
    }

    shareIntent.putExtra(Intent.EXTRA_STREAM, absolutePath.toUri())

    val chooserIntent = Intent.createChooser(shareIntent, null)
    context.startActivity(chooserIntent)
}

private fun moveImageToLockedFolder(absolutePath: String, id: Long, context: Context) {
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

        // TODO: replace with locked folder thing
        // database.trashedItemEntityDao().insertEntity(
        // 	TrashedItemEntity(
        // 		absolutePath,
        // 		copyToPath,
        // 		entity.dateTaken,
        // 		entity.mimeType,
        // 		entity.displayName
        // 	)
        // )

        database.mediaEntityDao().deleteEntityById(id)
    }
    File(copyToPath).lastModified()

    //fileToBeHidden.delete()
}

private fun moveOutOfLockedFolder(path: String) {
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

private fun renameImage(imagePath: String, imageName: String, newName: String, context: Context) {
    Log.d(TAG, "$imagePath, $imageName, $newName")
    val dir = imagePath.replace(imageName, "")
    val original = File(dir, imageName)
    val new = File(dir, newName)

    // if somehow the directory that the file is in stopped existing while renaming
    if (!File(dir).exists()) {
        File(dir).mkdirs()
    }

    try {
        val success = original.toPath().moveTo(new.toPath())
        if (!success.exists()) {
            Toast.makeText(context, "Failed to rename file", Toast.LENGTH_LONG).show()
        }
    } catch (e: Throwable) {
        Toast.makeText(context, "Failed to rename file", Toast.LENGTH_LONG).show()
    }
}

private fun copyToPath(mediaPath: String, albumPath: String, deleteOriginal: Boolean = false) {
    try {
        val fileToBeCopied = File(mediaPath)
        val absoluteAlbumPath = Path("/storage/emulated/0/", albumPath)
        val copyToPath = Path(absoluteAlbumPath.pathString, fileToBeCopied.name)
        Files.copy(
            Path(mediaPath),
            copyToPath,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        )

        if (deleteOriginal) {
            fileToBeCopied.delete()
        }

        File(copyToPath.absolutePathString()).lastModified()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}
