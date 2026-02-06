package com.kaii.photos.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.MediaDataSource.Companion.MEDIA_STORE_FILE_URI

// private const val TAG = "com.kaii.photos.mediastore.SimpleMediaDataSource"

/** Loads metadata from the media store for images and videos. */
class SimpleMediaDataSource(
    private val context: Context,
    private val paths: List<String>,
    private val reversed: Boolean
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATA,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_MODIFIED,
                FileColumns.MEDIA_TYPE,
                MediaColumns.SIZE,
                MediaColumns.MIME_TYPE,
                MediaColumns.IS_FAVORITE
            )
    }

    fun query(): List<MediaStoreData> {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        val query =
            if (paths.isEmpty()) {
                ""
            } else {
                "AND ${FileColumns.DATA} IN (${paths.joinToString(", ") { it }})"
            }

        val cursor =
            context.contentResolver.query(
                MEDIA_STORE_FILE_URI,
                PROJECTION,
                "(${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO})) " + query,
                null,
                "${MediaColumns.DATE_TAKEN} ${if (reversed) "ASC" else "DESC"}",
            ) ?: return emptyList()

        val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val favouritedColumn = cursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

        val items = mutableListOf<MediaStoreData>()

        while (cursor.moveToNext()) {
            val absolutePath = cursor.getString(absolutePathColNum)

            val id = cursor.getLong(idColNum)
            val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
            val dateModified = cursor.getLong(dateModifiedColumn)
            val size = cursor.getLong(sizeColumn)
            val mimeType = cursor.getString(mimeTypeColumn)
            val isFavourite = cursor.getInt(favouritedColumn) == 1

            val type =
                if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                else MediaType.Video

            val dateTaken =
                when {
                    mediaStoreDateTaken > 0L -> mediaStoreDateTaken

                    type == MediaType.Image -> getDateTakenForMedia(absolutePath, dateModified)

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
                    displayName = absolutePath.filename(),
                    absolutePath = absolutePath,
                    parentPath = absolutePath.parent(),
                    size = size,
                    immichUrl = null,
                    immichThumbnail = null,
                    hash = null,
                    customId = null,
                    favourited = isFavourite
                )

            items.add(new)
        }

        cursor.close()

        return if (reversed) items.sortedBy { it.dateTaken } else items.sortedByDescending { it.dateTaken }
    }
}
