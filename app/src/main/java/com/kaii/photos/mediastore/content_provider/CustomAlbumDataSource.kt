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
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource"

/** Loads metadata from the media store for images and videos. */
class CustomAlbumDataSource(
    private val context: Context,
    private val parentId: Int,
    private val sortMode: MediaItemSortMode,
    private val cancellationSignal: CancellationSignal
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                LavenderMediaColumns.URI,
                LavenderMediaColumns.ID
            )
    }

    fun loadMediaStoreData(): Flow<List<MediaStoreData>> = callbackFlow {
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
                Log.e(TAG, e.toString())
            }
        }

        awaitClose {
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }.conflate()

    fun query(): List<MediaStoreData> {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        val cursor =
            context.contentResolver.query(
                LavenderContentProvider.CONTENT_URI,
                PROJECTION,
                "${LavenderMediaColumns.PARENT_ID} = ?",
                arrayOf(parentId.toString()),
                null
            ) ?: return emptyList()

        val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
        val idCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.ID)

        val holderMap = mutableMapOf<Long, MutableList<MediaStoreData>>()

        while (cursor.moveToNext()) {
            val uri = cursor.getString(uriCol).toUri()
            val id = cursor.getLong(idCol)

            val new = context.contentResolver.getMediaStoreDataFromUri(context = context, uri = uri)?.copy(customId = id)!!

            val day =
                when (sortMode) {
                    MediaItemSortMode.DateModified -> new.getDateModifiedDay()
                    MediaItemSortMode.MonthTaken -> new.getDateTakenMonth()
                    MediaItemSortMode.DateTaken -> new.getDateTakenDay()
                    else -> MediaStoreData.dummyItem.getDateTakenDay()
                }

            if (sortMode == MediaItemSortMode.Disabled) holderMap.getOrPut(0L) { mutableListOf() }.add(new)
            else holderMap.getOrPut(day) { mutableListOf() }.add(new)
        }

        cursor.close()

        val sortedMap = holderMap.toSortedMap(compareByDescending { it })

        if (sortMode == MediaItemSortMode.Disabled) {
            return sortedMap[0L]?.sortedByDescending { it.dateTaken } ?: emptyList()
        }

        return holderMap.flatMap { it.value }
    }
}
