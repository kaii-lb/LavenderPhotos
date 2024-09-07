package com.kaii.photos.helpers.single_image_functions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.kaii.photos.MainActivity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.TrashedItemEntity
import com.kaii.photos.helpers.getAppTrashBinDirectory
import com.kaii.photos.helpers.GetDateTakenForMedia
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.setAttribute

const val TAG = "IMAGE_FUNCTIONS"

/** Order is very important here */
enum class ImageFunctions {
    ShareImage,
    EditImage,
    TrashImage,
    MoveToLockedFolder,
    UnTrashImage,
    LoadNormalImage,
    LoadTrashedImage,
    PermaDeleteImage,
}

fun operateOnImage(absolutePath: String, id: Long, operation: ImageFunctions, context: Context) {
    when (operation) {
        ImageFunctions.TrashImage -> {
            trashPhoto(absolutePath, id, context)
        }
        ImageFunctions.UnTrashImage -> {
            untrashPhoto(absolutePath, id)
        }
        ImageFunctions.PermaDeleteImage -> {
            permanentlyDeletePhoto(absolutePath)
        }

        else -> {
            Log.d(TAG, "chosen function was ${operation.name}")
        }
    }
}

fun trashPhoto(path: String, id: Long, context: Context) {
    val fileToBeTrashed = File(path)
    val absolutePath = fileToBeTrashed.absolutePath
    val trashDir = getAppTrashBinDirectory(context)
    val copyToPath = trashDir + "trashed." + fileToBeTrashed.name

	Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.ATOMIC_MOVE)
	
	val database = MainActivity.applicationDatabase

	CoroutineScope(EmptyCoroutineContext + CoroutineName("delete_file_context")).launch {
	  	val dateTaken = database.mediaEntityDao().getDateTaken(id)
	   	val mimeType = database.mediaEntityDao().getMimeType(id)
		val lastModified = System.currentTimeMillis()

		Path(copyToPath).setAttribute(BasicFileAttributes::lastModifiedTime.name, FileTime.fromMillis(lastModified))
		
	    database.trashedItemEntityDao().insertEntity(
	        TrashedItemEntity(
	            absolutePath,
	            copyToPath,
	            dateTaken,
	            mimeType
	        )
	    )

		database.mediaEntityDao().deleteEntityById(id)	
	}
	File(copyToPath).lastModified()

	fileToBeTrashed.delete()
}

private fun untrashPhoto(path: String, id: Long) {
    val fileToBeRevived = File(path)
	val absolutePath = fileToBeRevived.absolutePath

	val database = MainActivity.applicationDatabase

	CoroutineScope(EmptyCoroutineContext + CoroutineName("undelete_file_context")).launch {
		val item = database.trashedItemEntityDao().getFromTrashedPath(absolutePath)

		database.mediaEntityDao().insertEntity(
			MediaEntity(
				id = id,
				mimeType = item.mimeType,
				dateTaken = item.dateTaken
			)
		)

		val reverseCemetery = item.originalPath

		database.trashedItemEntityDao().deleteEntityByPath(absolutePath)

		Files.move(Path(absolutePath), Path(reverseCemetery), StandardCopyOption.ATOMIC_MOVE)

		val lastModified = System.currentTimeMillis()
		Path(reverseCemetery).setAttribute(BasicFileAttributes::lastModifiedTime.name, FileTime.fromMillis(lastModified))
		
		fileToBeRevived.delete()
	}
}

private fun permanentlyDeletePhoto(absolutePath: String) {
    val fileToBeShredded = File(absolutePath)
    fileToBeShredded.delete()

	CoroutineScope(EmptyCoroutineContext + CoroutineName("permanently_delete_file_context")).launch {
		MainActivity.applicationDatabase.trashedItemEntityDao().deleteEntityByPath(absolutePath)
	}
}
