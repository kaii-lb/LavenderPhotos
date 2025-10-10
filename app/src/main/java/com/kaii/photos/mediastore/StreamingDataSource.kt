package com.kaii.photos.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.compose.ui.util.fastFirstOrNull
import androidx.core.net.toUri
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaStoreDataSource.Companion.MEDIA_STORE_FILE_URI
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.models.multi_album.MultiAlbumViewModel"

/** Loads metadata from the media store for images and videos. */
class StreamingDataSource(
    private val context: Context,
    private val sqliteQuery: SQLiteQuery,
    private val sortBy: MediaItemSortMode,
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
                MediaColumns.IS_FAVORITE,
                MediaColumns.SIZE
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
                cancel("Cancelling MediaStoreDataSource channel because of exit signal...")
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

        val mediaCursor =
            context.contentResolver.query(
                MEDIA_STORE_FILE_URI,
                PROJECTION,
                "((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})) ${sqliteQuery.query}",
                sqliteQuery.paths?.toTypedArray(),
                "${MediaColumns.DATE_ADDED} DESC",
            )

        var map = mutableMapOf<MediaStoreData, MutableList<MediaStoreData>>()

        mediaCursor?.use { cursor ->
            val idColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATA)
            val mimeTypeColNum = mediaCursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = mediaCursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateModifiedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val dateTakenColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
            val dateAddedColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
            val sizeColumn = mediaCursor.getColumnIndexOrThrow(MediaColumns.SIZE)

            val dao = MediaDatabase.getInstance(context).mediaEntityDao()
            val allEntities = dao.getAll()

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

                val possibleDateTaken = allEntities.fastFirstOrNull { it.id == id }?.dateTaken

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

                if (sqliteQuery.basePaths?.contains(absolutePath.toBasePath()) == true || sqliteQuery.basePaths == null) {
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
                        when (sortBy) {
                            MediaItemSortMode.LastModified -> new.getLastModifiedDay()
                            MediaItemSortMode.MonthTaken -> new.getDateTakenMonth()
                            else -> new.getDateTakenDay()
                        }

                    val key = map.keys.find {
                        when (sortBy) {
                            MediaItemSortMode.LastModified -> it.dateModified == day
                            MediaItemSortMode.MonthTaken -> it.dateTaken == day
                            else -> it.dateTaken == day
                        }
                    }

                    if (key == null) {
                        val title = formatDate(day, sortBy, displayDateFormat)
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

                        if (map.keys.size in 5..10) {
                            map.keys.forEach { key ->
                                key.section = SectionItem(
                                    date = key.dateTaken,
                                    childCount = map[key]?.size ?: 0
                                )

                                map[key]?.onEach {
                                    it.section = key.section
                                }
                            }

                            send(map.flatMap { keyVal ->
                                listOf(keyVal.key) + keyVal.value.sortedByDescending { it.dateTaken }
                            })
                        }
                    } else {
                        map[key]?.add(new)
                    }
                }
            }
        }

        map.keys.forEach { key ->
            key.section = SectionItem(
                date = key.dateTaken,
                childCount = map[key]?.size ?: 0
            )

            map[key]?.onEach {
                it.section = key.section
            }
        }

        trySend(map.flatMap { keyVal ->
            listOf(keyVal.key) + keyVal.value.sortedByDescending { it.dateTaken }
        })

        awaitClose {}
    }
}
