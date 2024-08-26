package com.kaii.photos.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import android.text.format.DateFormat.format
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Calendar
import java.util.Locale

/** Loads metadata from the media store for images and videos. */
class MediaStoreDataSource
internal constructor(
    private val context: Context,
) {
    companion object {
        val neededPath = "DCIM"

        private val MEDIA_STORE_FILE_URI = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        private val PROJECTION =
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATE_TAKEN,
                MediaColumns.DATE_MODIFIED,
                MediaColumns.MIME_TYPE,
                MediaColumns.ORIENTATION,
                MediaColumns.DISPLAY_NAME,
                FileColumns.MEDIA_TYPE
            )
    }

    fun loadMediaStoreData(): Flow<List<MediaStoreData>> = callbackFlow {
        val contentObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    launch { trySend(query()) }
                }
            }

        context.contentResolver.registerContentObserver(
            MEDIA_STORE_FILE_URI,
            true,
            contentObserver
        )

        trySend(query())

        awaitClose { context.contentResolver.unregisterContentObserver(contentObserver) }
    }

    private fun query(): List<MediaStoreData> {
        Preconditions.checkArgument(
            Util.isOnBackgroundThread(),
            "Can only query from a background thread"
        )

        val data: MutableList<MediaStoreData> = ArrayList()
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
            val dateTakenColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
            val dateModifiedColNum = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
            val mimeTypeColNum = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
            val orientationColNum = cursor.getColumnIndexOrThrow(MediaColumns.ORIENTATION)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColNum)
                val dateTaken = cursor.getLong(dateTakenColNum)
                val mimeType = cursor.getString(mimeTypeColNum)
                val dateModified = cursor.getLong(dateModifiedColNum)
                val orientation = cursor.getInt(orientationColNum)
                val displayName = cursor.getString(displayNameIndex)
                val type =
                    if (cursor.getInt(mediaTypeColumnIndex) == FileColumns.MEDIA_TYPE_IMAGE) Type.IMAGE
                    else Type.VIDEO
                data.add(
                    MediaStoreData(
                        type = type,
                        rowId = id,
                        uri = Uri.withAppendedPath(MEDIA_STORE_FILE_URI, id.toString()),
                        mimeType = mimeType,
                        dateModified = dateModified,
                        orientation = orientation,
                        dateTaken = dateTaken,
                        displayName = displayName,
                    )
                )
            }
        }
        mediaCursor.close()
        return data
    }
}

/** A data model containing data for a single media item. */
@Parcelize
data class MediaStoreData(
    val type: Type,
    var rowId: Long,
    val uri: Uri,
    val mimeType: String?,
    val dateModified: Long,
    val orientation: Int,
    val dateTaken: Long,
    val displayName: String?,

    var gridPosition: Int = 0
) : Parcelable {
    /** gets the last modified date in months (no days/hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch millis*/
    fun getLastModifiedMonth() : Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken
            set(Calendar.DAY_OF_MONTH, 0)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }
}

/** The type of data. */
enum class Type {
    VIDEO,
    IMAGE,
    SECTION
}
