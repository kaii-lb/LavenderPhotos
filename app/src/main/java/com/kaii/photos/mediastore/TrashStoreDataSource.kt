package com.kaii.photos.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.groupPhotosBy

/** Loads metadata from the media store for images and videos. */
class TrashStoreDataSource(
    context: Context,
    sortBy: MediaItemSortMode,
    cancellationSignal: CancellationSignal,
    private val displayDateFormat: DisplayDateFormat
) : MediaStoreDataSource(
    context, "trash", sortBy, cancellationSignal
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaStore.Images.Media.DATA,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_ADDED,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                MediaColumns.IS_TRASHED,
                MediaColumns.SIZE
            )
    }

    override fun query(): List<MediaStoreData> {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )
        val data: MutableList<MediaStoreData> = emptyList<MediaStoreData>().toMutableList()

        val bundle = Bundle()
        bundle.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        bundle.putString(
            ContentResolver.QUERY_ARG_SQL_SELECTION,
            "(${MediaColumns.IS_TRASHED} = 1 AND ((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})))"
        )

        val mediaCursor = context.contentResolver.query(
            MEDIA_STORE_FILE_URI,
            PROJECTION,
            bundle,
            null
        ) ?: return data

        val dao = MediaDatabase.getInstance(context).mediaEntityDao()
        val allEntities = dao.getAll()

        mediaCursor.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateTakenColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
            val dateAddedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val sizeColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
                val dateAdded = cursor.getLong(dateAddedColumn)
                val displayName = cursor.getString(displayNameIndex)
                val size = cursor.getLong(sizeColumn)

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video

                val possibleDateTaken = allEntities.find { it.id == id }?.dateTaken

                val dateTaken =
                    when {
                        possibleDateTaken != null && possibleDateTaken > 0L -> possibleDateTaken

                        mediaStoreDateTaken > 0L -> mediaStoreDateTaken

                        type == MediaType.Image -> {
                            getDateTakenForMedia(absolutePath).let { exifDateTaken ->
                                dao.insertEntity(
                                    MediaEntity(
                                        id = id,
                                        dateTaken = exifDateTaken,
                                        mimeType = mimeType,
                                        displayName = displayName
                                    )
                                )

                                exifDateTaken
                            }
                        }

                        dateAdded > 0L -> dateAdded

                        else -> {
                            dateModified
                        }
                    }

                val uriParentPath =
                    if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(uriParentPath, id)

                data.add(
                    MediaStoreData(
                        type = type,
                        id = id,
                        uri = uri,
                        mimeType = mimeType,
                        dateModified = dateModified,
                        dateTaken = dateTaken,
                        displayName = displayName,
                        absolutePath = absolutePath,
                        size = size
                    )
                )
            }
        }
        mediaCursor.close()

        return groupPhotosBy(data, sortBy, displayDateFormat, context)
    }
}
