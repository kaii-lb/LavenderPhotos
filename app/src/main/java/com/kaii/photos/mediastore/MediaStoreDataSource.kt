package com.kaii.photos.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import com.kaii.photos.MainActivity
import com.kaii.photos.database.entities.MediaEntity
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.models.gallery_model.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import kotlin.io.path.Path

/** Loads metadata from the media store for images and videos. */
class MediaStoreDataSource
internal constructor(
    private val context: Context,
    private val neededPath: String,
    private val sortBy: MediaItemSortMode,
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
                FileColumns.MEDIA_TYPE,

                // MediaColumns.DATE_TAKEN
            )
    }

    fun loadMediaStoreData(): Flow<List<MediaStoreData>> = callbackFlow {
        var cancellationSignal = CancellationSignal()
        val mutex = Mutex()

        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch(Dispatchers.IO) {
                        mutex.withLock {
                            cancellationSignal.cancel()
                            cancellationSignal = CancellationSignal()
                        }

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

        awaitClose {
            context.contentResolver.unregisterContentObserver(contentObserver)
            cancellationSignal.cancel()
        }
    }.conflate()

    private fun query(): List<MediaStoreData> {
        val database = MainActivity.applicationDatabase
        val mediaEntityDao = database.mediaEntityDao()

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
                "${MediaColumns.DATE_TAKEN} DESC"
            ) ?: return data

        mediaCursor.use { cursor ->
            val idColNum = cursor.getColumnIndexOrThrow(MediaColumns._ID)
            val absolutePathColNum = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) // look into using the uri + id if this is deprecated
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val orientationColNum = cursor.getColumnIndexOrThrow(MediaColumns.ORIENTATION)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val dateAddedColumnNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)

            // val dateModifiedColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            // val dateTakenColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
				// val dateModified = cursor.getLong(dateModifiedColNum)

                val uri = Uri.withAppendedPath(MEDIA_STORE_FILE_URI, id.toString())
                val absolutePath = cursor.getString(absolutePathColNum)
				val dateModified = Files.getLastModifiedTime(Path(absolutePath)).toMillis() / 1000
                val displayName = cursor.getString(displayNameIndex)

                val possibleDateTaken = mediaEntityDao.getDateTaken(id)
                val dateTaken = if (possibleDateTaken != 0L) {
                    // Log.d(TAG, "date taken from database is $possibleDateTaken")
                    possibleDateTaken
                } else {
                    val taken = getDateTakenForMedia(
                        cursor.getString(absolutePathColNum)
                    )
                    mediaEntityDao.insertEntity(
                        MediaEntity(
                            id = id,
                            mimeType = mimeType,
                            dateTaken = taken,
                            displayName = displayName
                        )
                    )
                    // Log.d(TAG, "date taken was not found in database, inserting $taken")
                    taken
                }
				// val dateTaken = cursor.getLong(dateTakenColNum)
                val orientation = cursor.getInt(orientationColNum)
                val dateAdded = cursor.getLong(dateAddedColumnNum)
                val type = if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) MediaType.Image
                    else MediaType.Video
                data.add(
                    MediaStoreData(
                        type = type,
                        id = id,
                        uri = uri,
                        mimeType = mimeType,
                        dateModified = dateModified,
                        orientation = orientation,
                        dateTaken = dateTaken,
                        displayName = displayName,
                        dateAdded = dateAdded,
                        absolutePath = absolutePath
                    )
                )
            }
        }
        mediaCursor.close()

        return groupPhotosBy(data, sortBy)
    }
}
