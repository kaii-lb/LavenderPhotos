package com.kaii.photos.mediastore

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
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
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.mediastore.content_provider.LavenderContentProvider
import com.kaii.photos.mediastore.content_provider.LavenderMediaColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

private const val TAG = "ALBUM_STORE_DATA_SOURCE"

/** Loads metadata from the media store for images and videos. */
class AlbumStoreDataSource
internal constructor(
    private val context: Context,
    private val albumQueryPairs: List<Pair<AlbumInfo, SQLiteQuery>>,
    private val cancellationSignal: CancellationSignal
) {
    companion object {
        private val MEDIA_STORE_FILE_URI =
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaStore.Images.Media.DATA,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE
            )
    }

    fun loadMediaStoreData(): Flow<List<Pair<AlbumInfo, MediaStoreData>>> = callbackFlow {
        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch(Dispatchers.IO) {
                        runCatching {
                            val result = mutableListOf<Pair<AlbumInfo, MediaStoreData>>()

                            albumQueryPairs.forEach { (album, queryString) ->
                                val item = if (album.isCustomAlbum) {
                                    val q1 = queryCustom(parentId = album.id)
                                    val q2 = query(sqlQuery = queryString)

                                    if (q1.dateModified > q2.dateModified) q1 else q2
                                } else {
                                    query(sqlQuery = queryString)
                                }

                                result.add(Pair(album, item))
                            }

                            trySend(result)
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
                val result = mutableListOf<Pair<AlbumInfo, MediaStoreData>>()

                albumQueryPairs.forEach { (album, queryString) ->
                    val item = if (album.isCustomAlbum) {
                        val q1 = queryCustom(parentId = album.id)
                        val q2 = query(sqlQuery = queryString)

                        if (q1.dateModified > q2.dateModified) q1 else q2
                    } else {
                        query(sqlQuery = queryString)
                    }

                    result.add(Pair(album, item))
                }

                trySend(result)
            }
        }

        cancellationSignal.setOnCancelListener {
            try {
                cancel("Cancelling AlbumStoreDataSource because of exit signal...")
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
            }
        }

        awaitClose {
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }.conflate()

    private fun queryCustom(parentId: Int): MediaStoreData {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        var data = MediaStoreData()

        val mediaCursor =
            context.contentResolver.query(
                LavenderContentProvider.CONTENT_URI,
                arrayOf(LavenderMediaColumns.URI, LavenderMediaColumns.DATE_TAKEN),
                "${LavenderMediaColumns.PARENT_ID} = ?",
                arrayOf(parentId.toString()),
                "${LavenderMediaColumns.DATE_TAKEN} DESC"
            ) ?: return data

        mediaCursor.use { cursor ->
            if (mediaCursor.moveToFirst()) {
                val uriCol = cursor.getColumnIndexOrThrow(LavenderMediaColumns.URI)
                val uri = cursor.getString(uriCol)

                val media = context.contentResolver.getMediaStoreDataFromUri(uri.toUri())

                // Log.d(TAG, "The latest media is ${media!!.absolutePath} for the following albums: $parentId")

                data = media!!
            }
        }

        return data
    }

    private fun query(sqlQuery: SQLiteQuery): MediaStoreData {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        var data = MediaStoreData.dummyItem
        val queryArgs = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaColumns.DATE_MODIFIED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})) ${sqlQuery.query}"
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                sqlQuery.paths?.toTypedArray()
            )
        }

        val mediaCursor =
            context.contentResolver.query(
                MEDIA_STORE_FILE_URI,
                PROJECTION,
                queryArgs,
                null
            ) ?: return data

        mediaCursor.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) // look into using the uri + id if this is deprecated
            val dateModifiedColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)

            while (cursor.moveToNext() && data == MediaStoreData.dummyItem) {
                val id = cursor.getLong(idColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val dateModified = cursor.getLong(dateModifiedColNum)
                val displayName = cursor.getString(displayNameIndex)
                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video

                if (sqlQuery.includedBasePaths?.contains(absolutePath.getParentFromPath()) == true) {
                    data =
                        MediaStoreData(
                            type = type,
                            id = id,
                            uri = Uri.withAppendedPath(MEDIA_STORE_FILE_URI, id.toString()),
                            mimeType = mimeType,
                            dateModified = dateModified,
                            dateTaken = dateModified,
                            displayName = displayName,
                            absolutePath = absolutePath
                        )
                }
            }
        }

        return data
    }
}
