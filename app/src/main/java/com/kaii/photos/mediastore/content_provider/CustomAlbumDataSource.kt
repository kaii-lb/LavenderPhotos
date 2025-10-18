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
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.formatDate
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
    private val cancellationSignal: CancellationSignal,
    private val displayDateFormat: DisplayDateFormat
) {
    companion object {
        private val PROJECTION =
            arrayOf(
                LavenderMediaColumns.URI,
                LavenderMediaColumns.ID
            )
    }

    fun loadMediaStoreData(): Flow<Flow<List<MediaStoreData>>> = callbackFlow {
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

    fun query(): Flow<List<MediaStoreData>> = callbackFlow {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        var map = mutableMapOf<MediaStoreData, MutableList<MediaStoreData>>()

        val mediaCursor =
            context.contentResolver.query(
                LavenderContentProvider.CONTENT_URI,
                PROJECTION,
                "${LavenderMediaColumns.PARENT_ID} = ?",
                arrayOf(parentId.toString()),
                null
            )

        mediaCursor?.use { cursor ->
            val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
            val idCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.ID)

            while (cursor.moveToNext()) {
                val uri = cursor.getString(uriCol).toUri()
                val id = cursor.getInt(idCol)

                val new = context.contentResolver.getMediaStoreDataFromUri(context = context, uri = uri)?.copy(customId = id)!!

                val day =
                    when (sortMode) {
                        MediaItemSortMode.LastModified -> new.getLastModifiedDay()
                        MediaItemSortMode.MonthTaken -> new.getDateTakenMonth()
                        MediaItemSortMode.DateTaken -> new.getDateTakenDay()
                        else -> MediaStoreData.dummyItem.getDateTakenDay()
                    }

                val key = map.keys.find { section ->
                    when (sortMode) {
                        MediaItemSortMode.LastModified -> section.dateModified == day
                        else -> section.dateTaken == day
                    }
                }

                if (key == null) {
                    val title = formatDate(day, sortMode, displayDateFormat)
                    val section =
                        MediaStoreData(
                            type = MediaType.Section,
                            dateModified = day,
                            dateTaken = day,
                            uri = "$title $day".toUri(),
                            displayName = title,
                            id = 0L,
                            mimeType = null,
                            section = SectionItem(
                                date = day,
                                childCount = 0
                            )
                        )

                    map[section] = mutableListOf(new)

                    map = map.toSortedMap(
                        compareByDescending { data ->
                            data.dateTaken
                        }
                    )

                    if (map.keys.size in 5..10 || (sortMode == MediaItemSortMode.Disabled && map.firstKey().size in 20..100)) {
                        if (sortMode != MediaItemSortMode.Disabled) {
                            map.keys.forEach { key ->
                                key.section = SectionItem(
                                    date = key.dateTaken,
                                    childCount = map[key]?.size ?: 0
                                )

                                map[key]?.onEach {
                                    it.section = key.section
                                }
                            }
                        }

                        send(
                            map.flatMap { (key, value) ->
                                val keyList = if (sortMode == MediaItemSortMode.Disabled) {
                                    emptyList()
                                } else {
                                    listOf(key)
                                }

                                keyList +
                                        value.sortedByDescending { data ->
                                            when (sortMode) {
                                                MediaItemSortMode.LastModified -> data.dateModified
                                                else -> data.dateTaken
                                            }
                                        }
                            }
                        )
                    }
                } else {
                    map[key]?.add(new)
                }
            }
        }

        if (sortMode != MediaItemSortMode.Disabled) {
            map.keys.forEach { key ->
                key.section = SectionItem(
                    date = key.dateTaken,
                    childCount = map[key]?.size ?: 0
                )

                map[key]?.onEach {
                    it.section = key.section
                }
            }
        }

        send(
            map.flatMap { (key, value) ->
                val keyList = if (sortMode == MediaItemSortMode.Disabled) {
                    emptyList()
                } else {
                    listOf(key)
                }

                keyList +
                        value.sortedByDescending { data ->
                            when (sortMode) {
                                MediaItemSortMode.LastModified -> data.dateModified
                                else -> data.dateTaken
                            }
                        }
            }
        )

        awaitClose {}
    }
}
