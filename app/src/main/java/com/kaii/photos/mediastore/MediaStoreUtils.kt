package com.kaii.photos.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.EXTERNAL_DOCUMENTS_AUTHORITY
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.toRelativePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path


private const val TAG = "com.kaii.photos.mediastore.MediaStoreUtils"

const val LAVENDER_FILE_PROVIDER_AUTHORITY = "com.kaii.photos.LavenderPhotos.fileprovider"

/** @param media the [MediaStoreData] to copy
 * @param destination the absolute path to copy [media] to */
suspend fun ContentResolver.insertMedia(
    context: Context,
    media: MediaStoreData,
    basePath: String,
    destination: String,
    overwriteDate: Boolean,
    currentVolumes: Set<String>,
    overrideDisplayName: String? = null,
    onInsert: (origin: Uri, new: Uri) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    val file = File(media.absolutePath)
    val currentTime = System.currentTimeMillis()
    val volumeName =
        if (basePath == baseInternalStorageDirectory) MediaStore.VOLUME_EXTERNAL
        else currentVolumes.find {
            val possible = basePath.replace("/storage/", "").removeSuffix("/")
            it == possible || it == possible.lowercase()
        }

    val relativeDestination = destination.toRelativePath().removePrefix("/")
    val storageContentUri = getStorageContentUri(
        absolutePath = destination,
        type = media.type,
        volumeName = volumeName
    )

    if (storageContentUri != null && volumeName == MediaStore.VOLUME_EXTERNAL) {
        val contentValues = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, file.name)
            put(MediaColumns.RELATIVE_PATH, relativeDestination)
            put(MediaColumns.MIME_TYPE, media.mimeType)
            put(MediaColumns.DATE_TAKEN, media.dateTaken * 1000)
        }

        val newUri = insert(
            storageContentUri,
            contentValues
        )

        newUri?.let { uri ->
            onInsert(media.uri, uri)

            getMediaStoreDataFromUri(uri)?.let { newMedia ->
                File(newMedia.absolutePath).setLastModified(
                    if (overwriteDate) currentTime
                    else media.dateTaken * 1000
                )
            }

            return@withContext uri
        }

        return@withContext null
    }

    val fileName = overrideDisplayName ?: file.nameWithoutExtension
    val fullUriPath = context.getExternalStorageContentUriFromAbsolutePath(destination, true)

    try {
        val directory = DocumentFile.fromTreeUri(context, fullUriPath)
        Log.d(TAG, "Directory URI path ${directory?.uri}")
        val fileToBeSavedTo = directory?.createFile(
            media.mimeType ?: Files.probeContentType(Path(media.absolutePath)),
            fileName
        )

        fileToBeSavedTo?.let { savedToFile ->
            onInsert(media.uri, savedToFile.uri)

            getMediaStoreDataFromUri(savedToFile.uri)?.let { newMedia ->
                File(newMedia.absolutePath).setLastModified(
                    if (overwriteDate) currentTime
                    else media.dateTaken * 1000
                )
            }

            return@withContext savedToFile.uri
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Couldn't copy media $media")
        e.printStackTrace()
    }

    return@withContext null
}

fun ContentResolver.copyUriToUri(from: Uri, to: Uri) {
    openInputStream(from)?.buffered()?.use { fis ->
        openOutputStream(to)?.buffered()?.use { fos ->
            fis.copyTo(fos)
        }
    }
}

fun Context.getExternalStorageContentUriFromAbsolutePath(
    absolutePath: String,
    trimDoc: Boolean
): Uri {
    val relative = absolutePath.toRelativePath()
    val basePath = absolutePath.toBasePath().let {
        if (it == baseInternalStorageDirectory) "primary:"
        else it.replace("/storage/", "").replace("/", "") + ":"
    }

    val needed = if (relative.trim() == "") {
        appRestoredFilesDir.toRelativePath()
    } else relative

    val treeUri = DocumentsContract.buildTreeDocumentUri(EXTERNAL_DOCUMENTS_AUTHORITY, basePath)
    val pathId = "$basePath${needed.removePrefix("/")}"

    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, pathId).toString().let {
        if (trimDoc) it.replace("${basePath.removeSuffix(":")}%3A/document/", "")
        else it
    }
    Log.d(TAG, "Path ID for $absolutePath is $pathId, with document id $documentUri")

    return documentUri.toUri()
}

fun ContentResolver.getUriFromAbsolutePath(absolutePath: String, type: MediaType): Uri? {
    val contentUri =
        if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val data =
        if (type == MediaType.Image) MediaStore.Images.Media.DATA else MediaStore.Video.Media.DATA

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

fun ContentResolver.getMediaStoreDataFromUri(uri: Uri): MediaStoreData? {
    val mediaCursor = query(
        uri,
        arrayOf(
            MediaColumns._ID,
            MediaColumns.DATA,
            MediaColumns.DATE_MODIFIED,
            MediaColumns.MIME_TYPE,
            MediaColumns.DISPLAY_NAME
        ),
        null,
        null,
        null
    )

    mediaCursor?.let { cursor ->
        val contentIdColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mimeTypeColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val displayNameIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateModifiedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val contentId = cursor.getLong(contentIdColNum)
            val mimeType = cursor.getString(mimeTypeColNum)
            val absolutePath = cursor.getString(absolutePathColNum)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val displayName = cursor.getString(displayNameIndex)

            val dateTaken = getDateTakenForMedia(absolutePath)

            val type =
                if (mimeType.contains("image")) MediaType.Image
                else MediaType.Video

            val uriParentPath =
                if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val contentUri = ContentUris.withAppendedId(uriParentPath, contentId)

            cursor.close()

            return MediaStoreData(
                type = type,
                id = contentId,
                uri = contentUri,
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

/** returns the media store query and the individual paths
 * albums needed cuz the query has ? instead of the actual paths for...reasons */
fun getSQLiteQuery(albums: List<String>): SQLiteQuery {
    if (albums.isEmpty()) {
        return SQLiteQuery(query = "AND false", paths = null, includedBasePaths = null)
    }

    val colName = FileColumns.RELATIVE_PATH
    val base = "($colName = ?)"

    val list = mutableListOf<String>()
    var string = base
    val firstAlbum = albums.first().toRelativePath().removeSuffix("/").removePrefix("/")
    list.add("$firstAlbum/")

    for (i in 1..<albums.size) {
        val album = albums[i].toRelativePath().removeSuffix("/").removePrefix("/")

        string += " OR $base"
        list.add("$album/")
    }

    val query = "AND ($string)"
    return SQLiteQuery(query = query, paths = list, includedBasePaths = albums)
}

private fun getStorageContentUri(
    absolutePath: String,
    type: MediaType,
    volumeName: String?
): Uri? {
    val relative = absolutePath.toRelativePath().removePrefix("/")

    return when {
        relative.startsWith(Environment.DIRECTORY_DCIM) || relative.startsWith(Environment.DIRECTORY_PICTURES) || absolutePath.startsWith(
            Environment.DIRECTORY_MOVIES
        ) -> {
            if (type == MediaType.Image) MediaStore.Images.Media.getContentUri(volumeName) else MediaStore.Video.Media.getContentUri(volumeName)
        }

        relative.startsWith(Environment.DIRECTORY_DOCUMENTS) || relative.startsWith(
            Environment.DIRECTORY_DOWNLOADS
        ) -> MediaStore.Files.getContentUri(volumeName)

        else -> null
    }
}