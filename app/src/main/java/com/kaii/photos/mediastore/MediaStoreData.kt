package com.kaii.photos.mediastore

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Locale

/** A data model containing data for a single media item. */
@Parcelize
data class MediaStoreData(
    val type: MediaType = MediaType.Image,
    var id: Long = 0L,
    val uri: Uri = Uri.parse(""),
    val mimeType: String? = "image",
    val dateModified: Long = 0L,
    val orientation: Int = 0,
    val dateTaken: Long = 0L,
    val displayName: String? = "",
    val dateAdded: Long = 0L,
    val absolutePath: String = "",

    var gridPosition: Int = 0
) : Parcelable {
	/** gets the date taken in days (no hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch millis*/
    fun getDateTakenDay() : Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken * 1000
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }
    /** gets the date taken in months (no days/hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch millis*/
    fun getDateTakenMonth() : Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken * 1000
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }

    fun getLastModifiedDay() : Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateModified * 1000
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }
}

/** The type of data. */
enum class MediaType {
    Video,
    Image,
    Section,
    Folder,
    ImageOrVideo
}
