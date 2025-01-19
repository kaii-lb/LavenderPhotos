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
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kaii.photos.helpers.EXTERNAL_DOCUMENTS_AUTHORITY
import com.kaii.photos.helpers.getBaseInternalStorageDirectory
import com.kaii.photos.helpers.setDateTakenForMedia
import com.kaii.photos.mediastore.MediaStoreDataSource.Companion.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

private const val TAG = "MEDIA_STORE_UTILS"

/** @param media the [MediaStoreData] to copy
 * @param destination the relative path to copy [media] to */
suspend fun ContentResolver.copyMedia(
    context: Context,
    media: MediaStoreData,
    destination: String,
    overrideDisplayName: String? = null,
    setExifDateBeforeCopy: Boolean = false
): Uri? = withContext(Dispatchers.IO) {
    if (setExifDateBeforeCopy) {
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
        destination.startsWith(Environment.DIRECTORY_DCIM) || destination.startsWith(Environment.DIRECTORY_PICTURES) || destination.startsWith(Environment.DIRECTORY_MOVIES) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
    val fullUriPath = getExternalStorageContentUriFromAbsolutePath(destination, true)

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
        openFileDescriptor(from, "rw")?.let { fromFd ->
            FileUtils.copy(fromFd.fileDescriptor, toFd.fileDescriptor)

            fromFd.close()
        }

        toFd.close()
    }
}

fun ContentResolver.getContentUriForAbsolutePath(absolutePath: String): Uri? {
    val mediaCursor = query(
        MEDIA_STORE_FILE_URI,
        arrayOf(
            MediaColumns._ID,
            MediaColumns.DATA,
            MediaColumns.MIME_TYPE
        ),
        "${MediaColumns.DATA} = ?",
        arrayOf("%$absolutePath%"),
        null
    )

    mediaCursor?.let { cursor ->
        cursor.moveToFirst()

        val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)

        val id = cursor.getLong(idColNum)
        val mimeType = cursor.getString(mimeTypeColNum)

        val uriParentPath =
            if (mimeType.contains("image")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = ContentUris.withAppendedId(uriParentPath, id)

        cursor.close()

        return uri
    }

    return null
}

fun getExternalStorageContentUriFromAbsolutePath(absolutePath: String, trimDoc: Boolean): Uri {
    val relative = absolutePath.replace(getBaseInternalStorageDirectory(), "").removeSuffix("/")

    val treeUri = DocumentsContract.buildTreeDocumentUri(EXTERNAL_DOCUMENTS_AUTHORITY, "primary:")
    val pathId = "primary:$relative"


    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, pathId).toString().let {
        if (trimDoc) it.replace("/document/primary%3A", "")
        else it
    }
    Log.d(TAG, "Path ID for $absolutePath is $pathId, with document id $documentUri")

    return Uri.parse(documentUri)
}
