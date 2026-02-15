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
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.EXTERNAL_DOCUMENTS_AUTHORITY
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.exif.setDateTakenForMedia
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.mediastore.MediaDataSource.Companion.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


private const val TAG = "com.kaii.photos.mediastore.MediaStoreUtils"

const val LAVENDER_FILE_PROVIDER_AUTHORITY = "com.kaii.photos.LavenderPhotos.fileprovider"

/** @param media the [MediaStoreData] to copy
 * @param destination the absolute path to copy [media] to */
@OptIn(ExperimentalTime::class)
suspend fun ContentResolver.insertMedia(
    context: Context,
    media: MediaStoreData,
    basePath: String,
    destination: String,
    currentVolumes: Set<String>,
    preserveDate: Boolean = false,
    overrideDisplayName: String? = null,
    onInsert: (origin: Uri, new: Uri) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    val file = File(media.absolutePath)
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

    if (preserveDate) {
        setDateForMedia(
            uri = media.uri.toUri(),
            type = media.type,
            dateTaken = media.dateTaken
        )
    }

    if (storageContentUri != null && volumeName == MediaStore.VOLUME_EXTERNAL) {
        val contentValues = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, overrideDisplayName ?: file.name)
            put(MediaColumns.RELATIVE_PATH, relativeDestination)
            put(MediaColumns.MIME_TYPE, media.mimeType)

            if (preserveDate) {
                put(MediaColumns.DATE_ADDED, media.dateTaken)
                put(MediaColumns.DATE_MODIFIED, media.dateTaken)
                put(MediaColumns.DATE_ADDED, media.dateTaken * 1000)
            }
        }

        val newUri = insert(
            storageContentUri,
            contentValues
        )

        newUri?.let { contentUri ->
            onInsert(media.uri.toUri(), contentUri)

            if (!preserveDate) { // change date to most recent
                setDateForMedia(
                    uri = contentUri,
                    type = media.type,
                    dateTaken = Clock.System.now().epochSeconds
                )
            }

            return@withContext contentUri
        }

        return@withContext null
    }

    val fileName = overrideDisplayName ?: file.nameWithoutExtension
    val fullUriPath = context.getExternalStorageContentUriFromAbsolutePath(destination, true)

    try {
        val directory = DocumentFile.fromTreeUri(context, fullUriPath)
        Log.d(TAG, "Directory URI path ${directory?.uri}")

        val createdFile = directory?.createFile(media.mimeType, fileName)

        if (createdFile == null) {
            Log.e(TAG, "Unable to create document file for directory $destination and file ${file.absolutePath}")
            return@withContext null
        }

        val fileToBeSavedTo = DocumentFile.fromSingleUri(context, createdFile.uri)

        fileToBeSavedTo?.let { savedToFile ->
            onInsert(media.uri.toUri(), savedToFile.uri)

            if (!preserveDate) { // change date to most recent
                setDateForMedia(
                    uri = savedToFile.uri,
                    type = media.type,
                    dateTaken = Clock.System.now().epochSeconds
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

fun ContentResolver.getMediaStoreDataFromUri(context: Context, uri: Uri): MediaStoreData? {
    val mediaCursor = query(
        MEDIA_STORE_FILE_URI,
        arrayOf(
            MediaColumns._ID,
            FileColumns.DATA,
            MediaColumns.DATE_MODIFIED,
            MediaColumns.DATE_ADDED,
            MediaColumns.DATE_TAKEN,
            MediaColumns.MIME_TYPE,
            MediaColumns.DISPLAY_NAME,
            MediaColumns.IS_FAVORITE
        ),
        "${MediaColumns._ID} = ?",
        arrayOf(uri.lastPathSegment),
        null
    )

    mediaCursor?.let { cursor ->
        val contentIdColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mimeTypeColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val displayNameIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateModifiedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val dateTakenColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
        val dateAddedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
        val favouritedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

        while (cursor.moveToNext()) {
            val contentId = cursor.getLong(contentIdColNum)
            val mimeType = cursor.getString(mimeTypeColNum)
            val absolutePath: String? = cursor.getString(absolutePathColNum)
            val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val displayName = cursor.getString(displayNameIndex)
            val favourited = cursor.getInt(favouritedColumn) == 1

            Log.d(TAG, "Searching absolute path $absolutePath")

            if (absolutePath == null) return null

            val type =
                if (mimeType.contains("image")) MediaType.Image
                else MediaType.Video

            val dateTaken =
                when {
                    mediaStoreDateTaken > 0L -> mediaStoreDateTaken

                    type == MediaType.Image -> {
                        getDateTakenForMedia(absolutePath, dateModified)
                    }

                    dateAdded > 0L -> dateAdded

                    else -> {
                        dateModified
                    }
                }

            val uriParentPath =
                if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val contentUri = ContentUris.withAppendedId(uriParentPath, contentId)

            cursor.close()

            return MediaStoreData(
                type = type,
                id = contentId,
                uri = contentUri.toString(),
                mimeType = mimeType,
                dateModified = dateModified,
                dateTaken = dateTaken,
                displayName = displayName,
                absolutePath = absolutePath,
                parentPath = absolutePath.parent(),
                customId = null,
                immichUrl = null,
                immichThumbnail = null,
                hash = null,
                size = 0L,
                favourited = favourited
            )
        }
    }

    return null
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

/** @param dateTaken in seconds since epoch */
fun ContentResolver.setDateForMedia(
    uri: Uri,
    type: MediaType,
    dateTaken: Long,
    overwriteLastModified: Boolean = true
) {
    try {
        if (type == MediaType.Image) {
            openFileDescriptor(uri, "rw")?.use { fd ->
                setDateTakenForMedia(
                    fd = fd.fileDescriptor,
                    dateTaken = dateTaken
                )
            }
        }

        if (overwriteLastModified) {
            getAbsolutePathFromUri(uri)?.let {
                File(it).setLastModified(dateTaken * 1000)
            }
        }

        update(
            uri,
            ContentValues().apply {
                put(MediaColumns.DATE_ADDED, dateTaken)
                put(MediaColumns.DATE_TAKEN, dateTaken * 1000)

                if (overwriteLastModified) put(MediaColumns.DATE_MODIFIED, dateTaken)
            },
            null
        )
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
    }
}

fun ContentResolver.isFavourited(uri: Uri): Boolean {
    if (uri.lastPathSegment == null) return false

    val mediaCursor = query(
        MEDIA_STORE_FILE_URI,
        arrayOf(
            MediaColumns._ID,
            MediaColumns.IS_FAVORITE
        ),
        "${MediaColumns._ID} = ?",
        arrayOf(uri.lastPathSegment!!),
        null
    )

    mediaCursor?.use { cursor ->
        val favColumn = cursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

        while (cursor.moveToNext()) {
            val isFav = cursor.getInt(favColumn) == 1

            return isFav
        }
    }

    return false
}

fun ContentResolver.getAbsolutePathFromUri(uri: Uri): String? {
    if (uri.lastPathSegment == null) return null

    val mediaCursor = query(
        MEDIA_STORE_FILE_URI,
        arrayOf(
            MediaColumns._ID,
            MediaColumns.DATA
        ),
        "${MediaColumns._ID} = ?",
        arrayOf(uri.lastPathSegment!!),
        null
    )

    mediaCursor?.use { cursor ->
        val absolutePathColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA)

        while (cursor.moveToNext()) {
            val absolutePath = cursor.getString(absolutePathColumn)

            return absolutePath
        }
    }

    return null
}

fun ContentResolver.getPathsFromUriList(list: List<Uri>): List<Pair<Uri, String>> {
    val param = "(" + list.joinToString(",") { "?" } + ")"

    val mediaCursor = query(
        MEDIA_STORE_FILE_URI,
        arrayOf(
            MediaColumns._ID,
            MediaColumns.DATA,
            FileColumns.MEDIA_TYPE
        ),
        "${MediaColumns._ID} IN $param",
        list.fastMapNotNull { it.lastPathSegment }.toTypedArray(),
        null
    )

    val paths = mutableListOf<Pair<Uri, String>>()

    mediaCursor?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val absolutePath = cursor.getString(absolutePathColumn)
            val mediaType = cursor.getInt(mediaTypeColumn)

            val uriParentPath =
                if (mediaType == FileColumns.MEDIA_TYPE_IMAGE) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val uri = ContentUris.withAppendedId(uriParentPath, id)

            paths.add(Pair(uri, absolutePath))
        }
    }

    return paths
}