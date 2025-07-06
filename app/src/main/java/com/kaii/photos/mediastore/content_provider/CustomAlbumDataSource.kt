package com.kaii.photos.mediastore.content_provider

import android.content.Context
import android.database.ContentObserver
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.net.toUri
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaStoreDataSource
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

// private const val TAG = "CUSTOM_ALBUM_DATA_SOURCE"

/** Loads metadata from the media store for images and videos. */
class CustomAlbumDataSource(
    context: Context,
    private val parentId: Int,
    sortBy: MediaItemSortMode,
    private val cancellationSignal: CancellationSignal,
    private val displayDateFormat: DisplayDateFormat
) : MediaStoreDataSource(
    context, "", sortBy, cancellationSignal,
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                LavenderMediaColumns.URI,
                LavenderMediaColumns.ID
            )
    }

    override fun loadMediaStoreData(): Flow<List<MediaStoreData>> = channelFlow {
        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch(Dispatchers.IO) {
                        runCatching {
                            trySend(query())
                        }
                    }
                }
            }

        context.contentResolver.registerContentObserver(
            LavenderContentProvider.CONTENT_URI,
            true,
            contentObserver
        )

        launch(Dispatchers.IO) {
            runCatching {
                trySend(query())
            }
        }

        cancellationSignal.setOnCancelListener {
            try {
                cancel("Cancelling CustomAlbumDataSource channel because of exit signal...")
                channel.close()
            } catch (e: Throwable) {
                Log.e("MEDIA_STORE_DATASOURCE", e.toString())
            }
        }

        awaitClose {
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
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
        val idCol = mediaCursor.getColumnIndexOrThrow(LavenderMediaColumns.ID)

        mediaCursor.use { cursor ->
            while (cursor.moveToNext()) {
                val uri = cursor.getString(uriCol).toUri()
                val id = cursor.getInt(idCol)

                val mediaItem = context.contentResolver.getMediaStoreDataFromUri(uri = uri)

                if (mediaItem != null) data.add(mediaItem.copy(customId = id))
            }
        }
        mediaCursor.close()

        return groupPhotosBy(data, sortBy, displayDateFormat)
    }
}
