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
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.models.multi_album.groupPhotosBy

// private const val TAG = "MULTI_ALBUM_DATA_SOURCE"

/** Loads metadata from the media store for images and videos. */
class MultiAlbumDataSource(
    context: Context,
    private val queryString: SQLiteQuery,
    sortBy: MediaItemSortMode,
    cancellationSignal: CancellationSignal
) : MediaStoreDataSource(
    context, "", sortBy, cancellationSignal,
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATA,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                MediaColumns.IS_FAVORITE,
                MediaColumns.SIZE
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
                "((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})) ${queryString.query}",
                queryString.paths?.toTypedArray(),
                null,
            ) ?: return data

        val idColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mimeTypeColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val mediaTypeColumnIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val displayNameIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateModifiedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val dateTakenColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
        val sizeColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.SIZE)

        mediaCursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val displayName = cursor.getString(displayNameIndex)
                val size = cursor.getLong(sizeColumn)

                val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
                val dateTaken =
                    if (mediaStoreDateTaken == 0L) {
                        if (dateModified != 0L) {
                            dateModified
                        } else {
                            val possibleDateTaken = mediaEntityDao.getDateTaken(id)

                            if (possibleDateTaken != 0L) {
                                possibleDateTaken
                            } else {
                                val taken = getDateTakenForMedia(absolutePath)

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
                        }
                    } else {
                        mediaStoreDateTaken
                    }

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video

                val uriParentPath =
                    if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(uriParentPath, id)

                if (queryString.includedBasePaths?.contains(absolutePath.getParentFromPath()) == true || queryString.includedBasePaths == null) { // null for search to work since it pulls from everywhere
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
        }

        return groupPhotosBy(data, sortBy)
    }
}
