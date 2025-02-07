package com.kaii.photos.mediastore

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.helpers.SectionItem
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Locale

/** A data model containing data for a single media item. 
 @param date* is in seconds */
@Immutable
@Parcelize
data class MediaStoreData(
    val type: MediaType = MediaType.Image,
    val id: Long = 0L,
    val uri: Uri = Uri.parse(""),
    val mimeType: String? = "image",
    val dateModified: Long = 0L,
    val dateTaken: Long = 0L,
    val displayName: String? = "",
    val absolutePath: String = "",
    var section: SectionItem = SectionItem(0L, 0),
    val bytes: ByteArray? = null
) : Parcelable {
    companion object {
        val dummyItem = MediaStoreData()
    }

	/** gets the date taken in days (no hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch seconds*/
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
    /** its returned in unix epoch seconds*/
    fun getDateTakenMonth() : Long {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaStoreData

        if (type != other.type) return false
        if (id != other.id) return false
        if (uri != other.uri) return false
        if (mimeType != other.mimeType) return false
        if (dateModified != other.dateModified) return false
        if (dateTaken != other.dateTaken) return false
        if (displayName != other.displayName) return false
        if (absolutePath != other.absolutePath) return false
        if (section != other.section) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + dateTaken.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + absolutePath.hashCode()
        result = 31 * result + section.hashCode()
        return result
    }
}

fun MediaStoreData.signature() = ObjectKey(dateTaken + dateModified + absolutePath.hashCode() + id + mimeType.hashCode())

/** The type of data. */
enum class MediaType {
    Video,
    Image,
    Section
}

fun String.toMediaType() = when (this) {
    "Image" -> MediaType.Image
    "Video" -> MediaType.Video
    "Section" -> MediaType.Section
    else -> MediaType.Section
}

