package com.kaii.photos.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.helpers.GetDateTakenForMedia
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/** Loads metadata from the media store for images and videos. */
class AlbumStoreDataSource
internal constructor(
    private val context: Context,
    private val multiplePaths: List<String>,
) {
    companion object {

        private val MEDIA_STORE_FILE_URI = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaStore.Images.Media.DATA,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.DATE_ADDED,
                MediaColumns.MIME_TYPE,
                MediaColumns.ORIENTATION,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE
            )
    }

    fun loadMediaStoreData(): Flow<LinkedHashMap<String, MediaStoreData>> = callbackFlow {
        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch {
                        val returnVal = LinkedHashMap<String, MediaStoreData>()
                        for (directory in multiplePaths) {
                            returnVal[directory] = if (query(directory).isNotEmpty()) query(directory)[0] else MediaStoreData()
                        }
                        trySend(returnVal)
                    }
                }
            }

        context.contentResolver.registerContentObserver(
            MEDIA_STORE_FILE_URI,
            true,
            contentObserver
        )

        val returnVal = LinkedHashMap<String, MediaStoreData>()
        for (directory in multiplePaths) {
            returnVal[directory] = if (query(directory).isNotEmpty()) query(directory)[0] else MediaStoreData()
        }
        trySend(returnVal)

        awaitClose { context.contentResolver.unregisterContentObserver(contentObserver) }
    }

    private fun query(neededPath: String): List<MediaStoreData> {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )
        val data: MutableList<MediaStoreData> = emptyList<MediaStoreData>().toMutableList()
        val mediaCursor =
            context.contentResolver.query(
                MEDIA_STORE_FILE_URI,
                PROJECTION,
                FileColumns.MEDIA_TYPE +
                        " = " +
                        FileColumns.MEDIA_TYPE_IMAGE +
                        " AND " +
                        FileColumns.RELATIVE_PATH +
                        " LIKE ? " +
                        " OR " +
                        FileColumns.MEDIA_TYPE +
                        " = " +
                        FileColumns.MEDIA_TYPE_VIDEO +
                        " AND " +
                        FileColumns.RELATIVE_PATH +
                        " LIKE ? ",
                arrayOf("%$neededPath%", "%$neededPath%"),
                "${MediaColumns.DATE_MODIFIED} DESC"
            ) ?: return data

        mediaCursor.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) // look into using the uri + id if this is deprecated
            val dateModifiedColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val orientationColNum = cursor.getColumnIndexOrThrow(MediaColumns.ORIENTATION)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateAddedColumnNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColNum)
                val dateTaken = GetDateTakenForMedia(
                    cursor.getString(absolutePathColNum)
                )
                val mimeType = cursor.getString(mimeTypeColNum)
                val dateModified = cursor.getLong(dateModifiedColNum)
                val orientation = cursor.getInt(orientationColNum)
                val displayName = cursor.getString(displayNameIndex)
                val dateAdded = cursor.getLong(dateAddedColumnNum)
                val type = if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                else MediaType.Video
                data.add(
                    MediaStoreData(
                        type = type,
                        id = id,
                        uri = Uri.withAppendedPath(MEDIA_STORE_FILE_URI, id.toString()),
                        mimeType = mimeType,
                        dateModified = dateModified,
                        orientation = orientation,
                        dateTaken = dateTaken,
                        displayName = displayName,
                        dateAdded = dateAdded,
                    )
                )
            }
        }
        mediaCursor.close()

		data.sortByDescending {
			it.dateTaken
		}
        return data
    }
}
