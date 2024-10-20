package com.kaii.photos.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.CancellationSignal
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.MainActivity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.models.gallery_model.groupPhotosBy

/** Loads metadata from the media store for images and videos. */
class DefaultMediaStoreDataSource(
    context: Context,
    neededPath: String,
    sortBy: MediaItemSortMode,
    cancellationSignal: CancellationSignal
) : MediaStoreDataSource(
    context, neededPath, sortBy, cancellationSignal,
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaStore.Images.Media.DATA,
                MediaColumns.DATE_ADDED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE
            )
    }

    override fun query(): List<MediaStoreData> {
        val database = MainActivity.applicationDatabase
        val mediaEntityDao = database.mediaEntityDao()

        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )
        val data: MutableList<MediaStoreData> = emptyList<MediaStoreData>().toMutableList()
        val mediaCursor =
            context.contentResolver.query(
                MEDIA_STORE_FILE_URI,
                PROJECTION,
                "(${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE} AND ${FileColumns.RELATIVE_PATH} LIKE ? AND ${FileColumns.RELATIVE_PATH} NOT LIKE ?) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO} AND ${FileColumns.RELATIVE_PATH} LIKE ? AND ${FileColumns.RELATIVE_PATH} NOT LIKE ?)",
                arrayOf("%$neededPath%", "%$neededPath/%/%", "%$neededPath%", "%$neededPath/%/%"),
                ""
            ) ?: return data

        mediaCursor.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val displayName = cursor.getString(displayNameIndex)

                val possibleDateTaken = mediaEntityDao.getDateTaken(id)
                val dateTaken = if (possibleDateTaken != 0L) {
                    possibleDateTaken
                } else {
                    val taken = getDateTakenForMedia(
                        cursor.getString(absolutePathColNum)
                    )
                    mediaEntityDao.insertEntity(
                        MediaEntity(
                            id = id,
                            mimeType = mimeType,
                            dateTaken = taken,
                            displayName = displayName
                        )
                    )
                    taken
                }

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video

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
                        absolutePath = absolutePath
                    )
                )
            }
        }
        mediaCursor.close()

        return groupPhotosBy(data, sortBy)
    }
}
