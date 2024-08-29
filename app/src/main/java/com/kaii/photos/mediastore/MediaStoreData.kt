package com.kaii.photos.mediastore

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Locale

/** A data model containing data for a single media item. */
@Parcelize
data class MediaStoreData(
    val type: Type = Type.IMAGE,
    var rowId: Long = 0L,
    val uri: Uri = Uri.parse(""),
    val mimeType: String? = "image",
    val dateModified: Long = 0L,
    val orientation: Int = 0,
    val dateTaken: Long = 0L,
    val displayName: String? = "",
    val dateAdded: Long = 0L,

    var gridPosition: Int = 0
) : Parcelable {
	/** gets the last modified date in dat (no hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch millis*/
    fun getLastModifiedDay() : Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken * 1000
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }
    /** gets the last modified date in months (no days/hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch millis*/
    fun getLastModifiedMonth() : Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken * 1000
            set(Calendar.DAY_OF_MONTH, 0)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }
}

/** The type of data. */
enum class Type {
    VIDEO,
    IMAGE,
    SECTION,
    FOLDER,
    ImageOrVideo
}
