package com.kaii.photos.mediastore

import android.content.Context
import android.content.ContentResolver
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
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "ALBUM_STORE_DATA_SOURCE"

/** Loads metadata from the media store for images and videos. */
class AlbumStoreDataSource
internal constructor(
    private val context: Context,
    private val albumPaths: List<String>,
    private val cancellationSignal: CancellationSignal
) {
    companion object {
        private val MEDIA_STORE_FILE_URI = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
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

    fun loadMediaStoreData(): Flow<List<MediaStoreData>> = callbackFlow {
        var localCancellationSignal = CancellationSignal()
        val mutex = Mutex()

        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch(Dispatchers.IO) {
                        mutex.withLock {
                            localCancellationSignal.cancel()
                            localCancellationSignal = CancellationSignal()
                        }

                        runCatching {
			            	val list = mutableListOf<MediaStoreData>()
			            	for (path in albumPaths) {
			            		list.add(query(path))
			            	}

                            trySend(list)
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
            	val list = mutableListOf<MediaStoreData>()
            	for (path in albumPaths) {
            		list.add(query(path))
            	}

                trySend(list)
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
            localCancellationSignal.cancel()
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }

    private fun query(path: String): MediaStoreData {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        var data = MediaStoreData()
        val queryArgs = Bundle().apply {
        	putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaColumns.DATE_MODIFIED))
        	putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        	putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        	putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})) AND ${FileColumns.RELATIVE_PATH} = ?")
        	putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(path.removeSuffix("/") + "/"))
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
            val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) // look into using the uri + id if this is deprecated
            val dateModifiedColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColNum)
                val absolutePath = cursor.getString(absolutePathColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val dateModified = cursor.getLong(dateModifiedColNum)
                val displayName = cursor.getString(displayNameIndex)
                val type =
                	if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                	else MediaType.Video

				Log.d(TAG, "Album $path latest media is $absolutePath")
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

        mediaCursor.close()

        return data
    }
}
