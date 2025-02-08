package com.kaii.photos.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.FileUtils
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.EXTERNAL_DOCUMENTS_AUTHORITY
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.setDateTakenForMedia
import com.kaii.photos.helpers.getDateTakenForMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

private const val TAG = "MEDIA_STORE_UTILS"

const val LAVENDER_FILE_PROVIDER_AUTHORITY = "com.kaii.photos.LavenderPhotos.fileprovider"

/** @param media the [MediaStoreData] to copy
 * @param destination the relative path to copy [media] to */
suspend fun ContentResolver.copyMedia(
    context: Context,
    media: MediaStoreData,
    destination: String,
    overrideDisplayName: String? = null,
    setExifDateBeforeCopy: Boolean = false
): Uri? = withContext(Dispatchers.IO) {
    if (setExifDateBeforeCopy && media.type == MediaType.Image) {
        try {
            setDateTakenForMedia(
                media.absolutePath,
                media.dateTaken
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Cannot set date taken for media $media")
            e.printStackTrace()
        }
    }

    val file = File(media.absolutePath)

    val storageContentUri = when {
        destination.startsWith(Environment.DIRECTORY_DCIM) || destination.startsWith(Environment.DIRECTORY_PICTURES) || destination.startsWith(Environment.DIRECTORY_MOVIES) -> {
        	if (media.type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
   		}
        destination.startsWith(Environment.DIRECTORY_DOCUMENTS) || destination.startsWith(Environment.DIRECTORY_DOWNLOADS) -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else -> null
    }

    if (storageContentUri != null) {
        val contentValues = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, file.name)
            put(MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
            put(MediaColumns.DATE_TAKEN, media.dateTaken)
            put(MediaColumns.RELATIVE_PATH, destination)
            put(MediaColumns.MIME_TYPE, media.mimeType)
        }

        val newUri = insert(
            storageContentUri,
            contentValues
        )

        newUri?.let { uri ->
            copyUriToUri(media.uri, uri)

            return@withContext uri
        }

        return@withContext null
    }

    val fileName = overrideDisplayName ?: file.nameWithoutExtension
    val fullUriPath = context.getExternalStorageContentUriFromAbsolutePath(destination, true)

    try {
        val directory = DocumentFile.fromTreeUri(context, fullUriPath)
        val fileToBeSavedTo = directory?.createFile(
            media.mimeType ?: Files.probeContentType(Path(media.absolutePath)),
            fileName
        )
        fileToBeSavedTo?.let { savedToFile ->
            copyUriToUri(
                from = media.uri,
                to = savedToFile.uri
            )

            return@withContext savedToFile.uri
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Couldn't copy media $media")
        e.printStackTrace()
    }

    return@withContext null
}

fun ContentResolver.copyUriToUri(from: Uri, to: Uri) {
    openFileDescriptor(to, "rw")?.let { toFd ->
        openFileDescriptor(from, "r")?.let { fromFd ->
            FileUtils.copy(fromFd.fileDescriptor, toFd.fileDescriptor)

            fromFd.close()
        }

        toFd.close()
    }
}


fun Context.getExternalStorageContentUriFromAbsolutePath(absolutePath: String, trimDoc: Boolean): Uri {
    val relative = absolutePath.replace(baseInternalStorageDirectory, "").removeSuffix("/")

	val needed = if (relative.trim() == "") {
		appRestoredFilesDir.replace(baseInternalStorageDirectory, "")
	} else relative

    val treeUri = DocumentsContract.buildTreeDocumentUri(EXTERNAL_DOCUMENTS_AUTHORITY, "primary:")
    val pathId = "primary:$needed"

    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, pathId).toString().let {
        if (trimDoc) it.replace("/document/primary%3A", "")
        else it
    }
    Log.d(TAG, "Path ID for $absolutePath is $pathId, with document id $documentUri")

    return Uri.parse(documentUri)
}

fun getHighestLevelParentFromAbsolutePath(absolutePath: String) : String {
    val relative = absolutePath.replace(baseInternalStorageDirectory, "")
    val depth =
        if (relative.startsWith("Android") || relative.startsWith(Environment.DIRECTORY_DOWNLOADS)) 1
        else 0

    val highestParent = getHighestParentPath(absolutePath, depth)

	Log.d(TAG, "Highest parent for $absolutePath is $highestParent")

    return baseInternalStorageDirectory + highestParent.toString()
}

fun getHighestParentPath(absolutePath: String, depth: Int) : String? {
   	val relative = absolutePath.replace(baseInternalStorageDirectory, "")

    return if (absolutePath.length > baseInternalStorageDirectory.length + 1
    	&& !(relative.startsWith(Environment.DIRECTORY_DOWNLOADS) && relative.removeSuffix("/").endsWith(Environment.DIRECTORY_DOWNLOADS))
   	) {
        baseInternalStorageDirectory + relative.split("/").subList(fromIndex = 0, toIndex = depth + 1).joinToString("/")
    } else {
        null
    }
}

fun ContentResolver.getUriFromAbsolutePath(absolutePath: String, type: MediaType) : Uri? {
	val contentUri = if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
	val data = if (type == MediaType.Image) MediaStore.Images.Media.DATA else MediaStore.Video.Media.DATA

	Log.d(TAG, "Absolute path is $absolutePath")

	val mediaCursor = query(
		contentUri,
        arrayOf(
            MediaColumns._ID,
            MediaColumns.DATA
        ),
       "$data = ?",
        arrayOf(absolutePath),
        null
	)


	mediaCursor?.let { cursor ->
	    val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)

		while (cursor.moveToFirst()) {
			val id = cursor.getLong(idColNum)

			return ContentUris.withAppendedId(contentUri, id)
		}

        cursor.close()
	}

	return null
}

fun ContentResolver.getMediaStoreDataFromUri(uri: Uri) : MediaStoreData? {
	val id = uri.lastPathSegment!!

	Log.d(TAG, "ID for media is $id with uri $uri")

	val mediaCursor = query(
		MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
		arrayOf(
		    MediaColumns._ID,
		    MediaColumns.DATA,
		    MediaColumns.DATE_MODIFIED,
		    MediaColumns.MIME_TYPE,
		    MediaColumns.DISPLAY_NAME
		),
		"${MediaColumns._ID} = ?",
		arrayOf(id),
		null
	)

	mediaCursor?.let { cursor ->
        val absolutePathColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mimeTypeColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val displayNameIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateModifiedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)

		while (cursor.moveToNext()) {
			val mimeType = cursor.getString(mimeTypeColNum)
			val absolutePath = cursor.getString(absolutePathColNum)
			val dateModified = cursor.getLong(dateModifiedColumn)
			val displayName = cursor.getString(displayNameIndex)

			val dateTaken = getDateTakenForMedia(absolutePath)

			val type =
				if (mimeType.contains("image")) MediaType.Image
				else MediaType.Video

			cursor.close()

			return MediaStoreData(
				type = type,
				id = id.toLong(),
				uri = uri,
				mimeType = mimeType,
				dateModified = dateModified,
				dateTaken = dateTaken,
				displayName = displayName,
				absolutePath = absolutePath
			)
       	}
	}

	return null
}
