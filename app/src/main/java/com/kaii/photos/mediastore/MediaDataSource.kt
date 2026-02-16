package com.kaii.photos.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
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
import com.kaii.photos.helpers.exif.getDateTakenForMedia
import com.kaii.photos.helpers.parent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.models.multi_album.MultiAlbumViewModel"

/** Loads metadata from the media store for images and videos. */
class MediaDataSource(
    private val context: Context,
    private val cancellationSignal: CancellationSignal
) {
    companion object {
        val MEDIA_STORE_FILE_URI: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

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
                MediaColumns.SIZE,
                MediaColumns.IS_FAVORITE
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

    fun query(): List<MediaStoreData> {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        val cursor =
            context.contentResolver.query(
                MEDIA_STORE_FILE_URI,
                PROJECTION,
                "(${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO}))",
                null,
                "${MediaColumns.DATE_ADDED} DESC",
            ) ?: return emptyList()

        val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
        val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaColumns.SIZE)
        val favouritedColumn = cursor.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)

        val dataList = mutableListOf<MediaStoreData>()

        while (cursor.moveToNext()) {
            val absolutePath = cursor.getString(absolutePathColNum)

            val id = cursor.getLong(idColNum)
            val mimeType = cursor.getString(mimeTypeColNum)
            val mediaStoreDateTaken = cursor.getLong(dateTakenColumn) / 1000
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val displayName = cursor.getString(displayNameIndex)
            val size = cursor.getLong(sizeColumn)
            val favourited = cursor.getInt(favouritedColumn) == 1

            val type =
                if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                else MediaType.Video

            val dateTaken =
                when {
                    mediaStoreDateTaken > 0L -> mediaStoreDateTaken

                    type == MediaType.Image -> {
                        getDateTakenForMedia(absolutePath, dateModified)
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
                    uri = uri.toString(),
                    mimeType = mimeType,
                    dateModified = dateModified,
                    dateTaken = dateTaken,
                    displayName = displayName,
                    absolutePath = absolutePath,
                    parentPath = absolutePath.parent(),
                    size = size,
                    immichUrl = null,
                    immichThumbnail = null,
                    hash = null,
                    favourited = favourited
                )

            dataList.add(new)
        }

        cursor.close()

        return dataList
    }
}
