package com.kaii.photos.mediastore

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.CancellationSignal
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.groupPhotosBy

/** Loads metadata from the media store for images and videos. */
class MultiAlbumDataSource(
    context: Context,
    private val queryString: SQLiteQuery,
    sortBy: MediaItemSortMode,
    cancellationSignal: CancellationSignal,
    private val displayDateFormat: DisplayDateFormat
) : MediaStoreDataSource(
    context, "", sortBy, cancellationSignal,
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATA,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_ADDED,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                MediaColumns.IS_FAVORITE,
                MediaColumns.SIZE,
                MediaColumns.WIDTH,
                MediaColumns.HEIGHT
            )
    }

    override fun query(): List<MediaStoreData> {
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
        val dateAddedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
        val sizeColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.SIZE)
        val widthColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.WIDTH)
        val heightColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.HEIGHT)

        val metadataRetriever = MediaMetadataRetriever()

        mediaCursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val mediaStoreDateTaken = cursor.getLong(dateTakenColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val displayName = cursor.getString(displayNameIndex)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)

                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video

                val dateTaken =
                    when {
                        mediaStoreDateTaken > 0L -> mediaStoreDateTaken / 1000

                        mediaStoreDateTaken == -1L && type == MediaType.Image -> getDateTakenForMedia(absolutePath)

                        dateAdded > 0L -> dateAdded

                        else -> {
                            dateModified
                        }
                    }

                val uriParentPath =
                    if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(uriParentPath, id)

                if (queryString.includedBasePaths?.contains(absolutePath.getParentFromPath()) == true || queryString.includedBasePaths == null) {
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
                            size = size,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        }

        metadataRetriever.release()

        return groupPhotosBy(data, sortBy, displayDateFormat, context)
    }
}
