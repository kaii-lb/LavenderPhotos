package com.kaii.photos.database.entities

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kaii.photos.mediastore.MediaType
import java.util.Calendar
import java.util.Locale

@Stable
@Immutable
@Entity(
    tableName = "media",
    indices = [
        Index(value = ["dateTaken"], orders = [Index.Order.DESC]),
        Index(value = ["hash"]),
        Index(value = ["immichUrl"], unique = true)
    ],
)
data class MediaStoreData(
    @PrimaryKey val id: Long,
    val uri: String,
    val absolutePath: String,
    val parentPath: String,
    val displayName: String,
    val dateTaken: Long,
    val dateModified: Long,
    val mimeType: String,
    val type: MediaType,
    val immichUrl: String?,
    val immichThumbnail: String?,
    val hash: String?,
    val size: Long,
    val customId: Long?,
    val favourited: Boolean
) {
    companion object {
        val dummyItem = MediaStoreData(
            id = 0L,
            uri = "",
            absolutePath = "",
            parentPath = "",
            displayName = "",
            dateTaken = 0L,
            dateModified = 0L,
            mimeType = "",
            type = MediaType.Section,
            immichUrl = null,
            immichThumbnail = null,
            hash = null,
            size = 0L,
            customId = null,
            favourited = false
        )
    }

    /** gets the date taken in days (no hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch seconds*/
    fun getDateTakenDay(): Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken * 1000
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }

    // TODO
    /** gets the date taken in months (no days/hours/minutes/seconds/milliseconds) */
    /** its returned in unix epoch seconds*/
    fun getDateTakenMonth(): Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = dateTaken * 1000
            set(Calendar.DAY_OF_MONTH, 1) // months don't start with day numbered 0 :|
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis / 1000
    }

    fun getDateModifiedDay(): Long {
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
