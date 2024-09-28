package com.kaii.photos.helpers.single_image_functions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.kaii.photos.MainActivity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.database.entities.TrashedItemEntity
import com.kaii.photos.helpers.getAppLockedFolderDirectory
import com.kaii.photos.helpers.getAppTrashBinDirectory
import com.kaii.photos.helpers.getAppRestoredFromLockedFolderDirectory
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.setAttribute
import kotlin.jvm.Throws

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
	SearchImage,
	MoveOutOfLockedFolder
}

fun operateOnImage(absolutePath: String, id: Long, operation: ImageFunctions, context: Context) {
    when (operation) {
        ImageFunctions.TrashImage -> {
            trashPhoto(absolutePath, id, context)
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
			moveOutOfLockedFolder(absolutePath, context)
		}

        else -> {
            Log.d(TAG, "chosen function was ${operation.name}")
        }
    }
}

fun trashPhoto(path: String, id: Long, context: Context) {
    val fileToBeTrashed = File(path)
    val absolutePath = fileToBeTrashed.absolutePath
    val trashDir = context.getAppTrashBinDirectory()
    val copyToPath = trashDir + "trashed-" + fileToBeTrashed.name

	Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.ATOMIC_MOVE)
	
	val database = MainActivity.applicationDatabase

	val lastModified = System.currentTimeMillis()
	CoroutineScope(EmptyCoroutineContext + CoroutineName("delete_file_context")).launch {
	  	val entity = database.mediaEntityDao().getFromId(id)

		Path(copyToPath).setAttribute(BasicFileAttributes::lastModifiedTime.name, FileTime.fromMillis(lastModified))
		
	    database.trashedItemEntityDao().insertEntity(
	        TrashedItemEntity(
	            absolutePath,
	            copyToPath,
	            entity.dateTaken,
	            entity.mimeType,
				entity.displayName
	        )
	    )

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

			Path(reverseCemetery).setAttribute(BasicFileAttributes::lastModifiedTime.name, FileTime.fromMillis(lastModified))

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

private fun shareImage(absolutePath: String, id: Long, context: Context) {
	val database = MainActivity.applicationDatabase
	CoroutineScope(EmptyCoroutineContext + CoroutineName("delete_file_context")).launch {
		val mimeType = database.mediaEntityDao().getMimeType(id)

		val shareIntent = Intent().apply {
			action = Intent.ACTION_SEND
			type = mimeType
		}

		shareIntent.putExtra(Intent.EXTRA_STREAM, absolutePath.toUri())

		val chooserIntent = Intent.createChooser(shareIntent, null)
		context.startActivity(chooserIntent)
	}
}

private fun moveImageToLockedFolder(absolutePath: String, id: Long, context: Context) {
	val fileToBeHidden = File(absolutePath)
	val lockedFolderDir = context.getAppLockedFolderDirectory()
	val copyToPath = lockedFolderDir + fileToBeHidden.name
	println(lockedFolderDir)
	Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.REPLACE_EXISTING)

	val database = MainActivity.applicationDatabase

	val lastModified = System.currentTimeMillis()
	CoroutineScope(EmptyCoroutineContext + CoroutineName("hide_file_context")).launch {
		// val entity = database.mediaEntityDao().getFromId(id)

		Path(copyToPath).setAttribute(BasicFileAttributes::lastModifiedTime.name, FileTime.fromMillis(lastModified))

		// TODO: replace this with a sidecar file
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

private fun moveOutOfLockedFolder(path: String, context: Context) {
    val fileToBeRevived = File(path)
	val absolutePath = fileToBeRevived.absolutePath

	val lastModified = System.currentTimeMillis()

	// TODO: use sidecar files(?) to track where it was from
	// or write metadata into the media itself
	val reverseCemetery = context.getAppRestoredFromLockedFolderDirectory() + fileToBeRevived.name

	Files.move(Path(absolutePath), Path(reverseCemetery), StandardCopyOption.REPLACE_EXISTING)

	Path(reverseCemetery).setAttribute(BasicFileAttributes::lastModifiedTime.name, FileTime.fromMillis(lastModified))

	fileToBeRevived.delete()
}

