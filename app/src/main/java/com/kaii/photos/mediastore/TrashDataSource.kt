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
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaDataSource.Companion.MEDIA_STORE_FILE_URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.mediastore.TrashDataSource"

/** Loads metadata from the media store for images and videos. */
class TrashDataSource(
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
                MediaColumns.IS_TRASHED,
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
        bundle.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        bundle.putString(
            ContentResolver.QUERY_ARG_SQL_SELECTION,
            "(${MediaColumns.IS_TRASHED} = 1 AND ((${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}) OR (${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO})))"
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
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)

        val holderMap = mutableListOf<MediaStoreData>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColNum)
            val mimeType = cursor.getString(mimeTypeColNum)
            val absolutePath = cursor.getString(absolutePathColNum)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val displayName = cursor.getString(displayNameIndex)
            val size = cursor.getLong(sizeColumn)

            val type =
                if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                else MediaType.Video

            val uriParentPath =
                if (type == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uri = ContentUris.withAppendedId(uriParentPath, id)

            val new =
                MediaStoreData(
                    type = type,
                    id = id,
                    uri = uri.toString(),
                    mimeType = mimeType,
                    dateModified = dateModified,
                    dateTaken = dateModified,
                    displayName = displayName,
                    absolutePath = absolutePath,
                    size = size,
                    customId = null,
                    immichUrl = null,
                    immichThumbnail = null,
                    hash = null
                )

            holderMap.add(new)
        }

        cursor.close()

        return holderMap
    }
}
