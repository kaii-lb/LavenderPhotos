package com.kaii.photos.mediastore.content_provider

import android.content.Context
import android.os.CancellationSignal
import androidx.core.net.toUri
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaStoreDataSource
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.multi_album.groupPhotosBy

// private const val TAG = "CUSTOM_ALBUM_DATA_SOURCE"

/** Loads metadata from the media store for images and videos. */
class CustomAlbumDataSource(
    context: Context,
    private val parentId: Int,
    sortBy: MediaItemSortMode,
    cancellationSignal: CancellationSignal
) : MediaStoreDataSource(
    context, "", sortBy, cancellationSignal,
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                LavenderMediaColumns.URI
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
                LavenderContentProvider.CONTENT_URI,
                PROJECTION,
                "${LavenderMediaColumns.PARENT_ID} = ?",
                arrayOf(parentId.toString()),
                null,
            ) ?: return data

        val uriCol = mediaCursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
        mediaCursor.use { cursor ->
            while (cursor.moveToNext()) {
                val uri = cursor.getString(uriCol).toUri()

                val mediaItem = context.contentResolver.getMediaStoreDataFromUri(uri = uri)

                if (mediaItem != null) data.add(mediaItem)
            }
        }
        mediaCursor.close()

        return groupPhotosBy(data, sortBy)
    }
}
