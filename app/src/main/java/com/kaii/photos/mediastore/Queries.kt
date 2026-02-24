package com.kaii.photos.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.parent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getAllMediaStoreIds(context: Context): Set<Long> {
    val cursor =
        context.contentResolver.query(
            MEDIA_STORE_FILE_URI,
            arrayOf(MediaColumns._ID),
            "(${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO}))",
            null,
            null,
        ) ?: return emptySet()

    val ids = mutableSetOf<Long>()

    val idCol = cursor.getColumnIndexOrThrow(MediaColumns._ID)

    while (cursor.moveToNext()) {
        ids.add(cursor.getLong(idCol))
    }

    cursor.close()

    return ids
}

suspend fun getMediaStoreDataForIds(
    ids: Set<Long>,
    context: Context
): Set<MediaStoreData> = withContext(Dispatchers.IO) {
    val items = mutableSetOf<MediaStoreData>()

    ids.chunked(500).forEach { chunk ->
        val selection = "${MediaColumns._ID} IN (${chunk.joinToString { "?" }})"
        val selectionArgs = chunk.map { it.toString() }.toTypedArray()

        val projection =
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATA,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_ADDED,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                MediaColumns.SIZE,
                MediaColumns.IS_FAVORITE
            )

        context.contentResolver.query(
            MEDIA_STORE_FILE_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)
            val favouritedColumn = cursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

            while (cursor.moveToNext()) {
                val absolutePath = cursor.getString(absolutePathColNum)

                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val displayName = cursor.getString(displayNameIndex)
                val size = cursor.getLong(sizeColumn)
                val favourited = cursor.getInt(favouritedColumn) == 1

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
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
                val uri = ContentUris.withAppendedId(uriParentPath, id)

                val new =
                    MediaStoreData(
                        type = type,
                        id = id,
                        uri = uri.toString(),
                        mimeType = mimeType,
                        dateModified = dateModified,
                        dateTaken = dateTaken,
                        displayName = displayName,
                        absolutePath = absolutePath,
                        parentPath = absolutePath.parent(),
                        size = size,
                        immichUrl = null,
                        immichThumbnail = null,
                        hash = null,
                        favourited = favourited
                    )

                items.add(new)
            }
        }
    }

    return@withContext items
}

suspend fun chunkLoadMediaData(
    ids: Set<Long>,
    context: Context,
    onLoadChunk: suspend (chunk: Set<MediaStoreData>) -> Unit
) = withContext(Dispatchers.IO) {
    val items = mutableSetOf<MediaStoreData>()
    val syncManager = SyncManager(context)

    ids.chunked(500).forEach { chunk ->
        items.clear()
        val selection = "${MediaColumns._ID} IN (${chunk.joinToString { "?" }}) AND (${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO}))"
        val selectionArgs = chunk.map { it.toString() }.toTypedArray()

        val projection =
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATA,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_ADDED,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                MediaColumns.SIZE,
                MediaColumns.IS_FAVORITE,
                MediaColumns.GENERATION_MODIFIED
            )

        var newGen = syncManager.getGeneration()
        context.contentResolver.query(
            MEDIA_STORE_FILE_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val genColNum = cursor.getColumnIndexOrThrow(MediaColumns.GENERATION_MODIFIED)
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)
            val favouritedColumn = cursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

            while (cursor.moveToNext()) {
                val gen = cursor.getLong(genColNum)
                if (gen > newGen) newGen = gen

                val absolutePath = cursor.getString(absolutePathColNum)
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val displayName = cursor.getString(displayNameIndex)
                val size = cursor.getLong(sizeColumn)
                val favourited = cursor.getInt(favouritedColumn) == 1

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
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
                val uri = ContentUris.withAppendedId(uriParentPath, id)

                val new =
                    MediaStoreData(
                        type = type,
                        id = id,
                        uri = uri.toString(),
                        mimeType = mimeType,
                        dateModified = dateModified,
                        dateTaken = dateTaken,
                        displayName = displayName,
                        absolutePath = absolutePath,
                        parentPath = absolutePath.parent(),
                        size = size,
                        immichUrl = null,
                        immichThumbnail = null,
                        hash = null,
                        favourited = favourited
                    )

                items.add(new)
            }
        }

        syncManager.setGeneration(gen = newGen)
        onLoadChunk(items)
    }
}

suspend fun loadMediaDataDelta(
    context: Context
) = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaStoreData>()
    val syncManager = SyncManager(context)

    items.clear()
    val selectionArgs = arrayOf(syncManager.getGeneration().toString())
    val selection = "${MediaStore.Images.Media.GENERATION_MODIFIED} > ? " +
            "AND (${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO}))"

    val projection =
        arrayOf(
            MediaColumns._ID,
            MediaColumns.DATA,
            MediaColumns.DATE_TAKEN,
            MediaColumns.DATE_ADDED,
            MediaColumns.DATE_MODIFIED,
            MediaColumns.MIME_TYPE,
            MediaColumns.DISPLAY_NAME,
            FileColumns.MEDIA_TYPE,
            MediaColumns.SIZE,
            MediaColumns.IS_FAVORITE,
            MediaColumns.GENERATION_MODIFIED
        )

    var newGen = syncManager.getGeneration()
    context.contentResolver.query(
        MEDIA_STORE_FILE_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        val genColNum = cursor.getColumnIndexOrThrow(MediaColumns.GENERATION_MODIFIED)
        val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)
        val favouritedColumn = cursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

        while (cursor.moveToNext()) {
            val gen = cursor.getLong(genColNum)
            if (gen > newGen) newGen = gen

            val absolutePath = cursor.getString(absolutePathColNum)
            val id = cursor.getLong(idColNum)
            val mimeType = cursor.getString(mimeTypeColNum)
            val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val displayName = cursor.getString(displayNameIndex)
            val size = cursor.getLong(sizeColumn)
            val favourited = cursor.getInt(favouritedColumn) == 1

            val type =
                if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
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
            val uri = ContentUris.withAppendedId(uriParentPath, id)

            val new =
                MediaStoreData(
                    type = type,
                    id = id,
                    uri = uri.toString(),
                    mimeType = mimeType,
                    dateModified = dateModified,
                    dateTaken = dateTaken,
                    displayName = displayName,
                    absolutePath = absolutePath,
                    parentPath = absolutePath.parent(),
                    size = size,
                    immichUrl = null,
                    immichThumbnail = null,
                    hash = null,
                    favourited = favourited
                )

            items.add(new)
        }
    }

    syncManager.setGeneration(gen = newGen)

    return@withContext items
}