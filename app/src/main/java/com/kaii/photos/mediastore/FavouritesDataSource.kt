package com.kaii.photos.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.core.net.toUri
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.formatDate
import com.kaii.photos.mediastore.MediaDataSource.Companion.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

private const val TAG = "com.kaii.photos.mediastore.TrashDataSource"

/** Loads metadata from the media store for images and videos. */
class FavouritesDataSource(
    private val context: Context,
    private val sortMode: MediaItemSortMode,
    private val cancellationSignal: CancellationSignal,
    private val displayDateFormat: DisplayDateFormat
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
                MediaColumns.SIZE
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
            MEDIA_STORE_FILE_URI,
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
                cancel("Cancelling TrashDataSource channel because of exit signal...")
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

        val bundle = Bundle()
        bundle.putInt(MediaStore.QUERY_ARG_MATCH_FAVORITE, MediaStore.MATCH_ONLY)
        bundle.putString(
            ContentResolver.QUERY_ARG_SQL_SELECTION,
            "(${MediaColumns.IS_FAVORITE} = 1 AND ((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})))"
        )

        val cursor = context.contentResolver.query(
            MEDIA_STORE_FILE_URI,
            PROJECTION,
            bundle,
            null
        ) ?: return emptyList()

        val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)

        val dao = MediaDatabase.getInstance(context).mediaEntityDao()
        val allEntities = dao.getAll().associate { it.id to it.dateTaken } // map lookups are faster

        val entitiesToBeInserted = mutableListOf<MediaEntity>() // bulk insert is faster
        val holderMap = mutableMapOf<Long, MutableList<MediaStoreData>>() // maps are faster, AGAIN

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColNum)
            val mimeType = cursor.getString(mimeTypeColNum)
            val absolutePath = cursor.getString(absolutePathColNum)
            val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val displayName = cursor.getString(displayNameIndex)
            val size = cursor.getLong(sizeColumn)

            val type =
                if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                else MediaType.Video

            val possibleDateTaken = allEntities[id]

            val dateTaken =
                when {
                    possibleDateTaken != null && possibleDateTaken > 0L -> possibleDateTaken

                    mediaStoreDateTaken > 0L -> mediaStoreDateTaken

                    type == MediaType.Image -> {
                        getDateTakenForMedia(absolutePath, dateModified).let { exifDateTaken ->
                            entitiesToBeInserted.add(
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

            val new =
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

            val day =
                when (sortMode) {
                    MediaItemSortMode.LastModified -> new.getLastModifiedDay()
                    MediaItemSortMode.MonthTaken -> new.getDateTakenMonth()
                    MediaItemSortMode.DateTaken -> new.getDateTakenDay()
                    else -> 0L
                }

            holderMap.getOrPut(day) { mutableListOf() }.add(new)
        }

        cursor.close()

        if (entitiesToBeInserted.isNotEmpty()) dao.insertAll(entitiesToBeInserted)

        val sortedMap = holderMap.toSortedMap(compareByDescending { it })
        val sorted = mutableListOf<MediaStoreData>()

        if (sortMode == MediaItemSortMode.DisabledLastModified || sortMode == MediaItemSortMode.Disabled) {
            return sortedMap.flatMap { it.value }.sortedByDescending { it.dateModified }
        }

        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = calendar.timeInMillis / 1000
        val daySeconds = 60 * 60 * 24
        val yesterday = today - daySeconds

        val todayString = context.resources.getString(R.string.today)
        val yesterdayString = context.resources.getString(R.string.yesterday)

        sortedMap.forEach { (day, items) ->
            val title = when (day) {
                today -> {
                    todayString
                }

                yesterday -> {
                    yesterdayString
                }

                else -> {
                    formatDate(
                        timestamp = day,
                        sortBy = sortMode,
                        format = displayDateFormat
                    )
                }
            }

            val sectionItem = SectionItem(
                date = day,
                childCount = items.size
            )

            val section =
                MediaStoreData(
                    type = MediaType.Section,
                    dateModified = day,
                    dateTaken = day,
                    uri = "$title $day".toUri(),
                    displayName = title,
                    id = 0L,
                    mimeType = null,
                    section = sectionItem
                )

            sorted.add(section)

            sorted.addAll(
                items.sortedByDescending { item ->
                    item.dateModified
                }.onEach { it.section = sectionItem }
            )
        }

        return sorted
    }
}
