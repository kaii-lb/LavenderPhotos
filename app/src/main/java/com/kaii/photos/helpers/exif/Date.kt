package com.kaii.photos.helpers.exif

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.io.FileDescriptor
import java.io.InputStream
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "com.kaii.photos.helpers.exif.Date"

/** in epoch seconds */
@OptIn(ExperimentalTime::class)
fun getDateTakenForMedia(absolutePath: String, dateModified: Long): Long {
    try {
        val exifInterface = ExifInterface(absolutePath)
        val exifDateTimeFormat = LocalDateTime.Formats.ISO

        val lastModified = Instant
            .fromEpochSeconds(dateModified)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(exifDateTimeFormat)

        val datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: (exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                ?: lastModified) // this really should not get to last modified

        val parsedDateTime = datetime.replace("T", " ").let {
            it.substringBefore(" ").replace(":", "-") +
                    "T" + it.substringAfter(" ").substringBefore("+")
        }

        val dateTimeSinceEpoch =
            LocalDateTime
                .parse(parsedDateTime, exifDateTimeFormat)
                .toInstant(TimeZone.currentSystemDefault())
                .epochSeconds

        return dateTimeSinceEpoch
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
        return 0L
    }
}

@OptIn(ExperimentalTime::class)
fun getDateTakenForMedia(
    inputStream: InputStream,
    dateModified: Long
): Long {
    try {
        val exifInterface = ExifInterface(inputStream)
        val exifDateTimeFormat = LocalDateTime.Formats.ISO

        val lastModified = Instant
            .fromEpochSeconds(dateModified)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(exifDateTimeFormat)

        val datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: (exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                ?: lastModified) // this really should not get to last modified

        val parsedDateTime = datetime.replace("T", " ").let {
            it.substringBefore(" ").replace(":", "-") +
                    "T" + it.substringAfter(" ").substringBefore("+")
        }

        val dateTimeSinceEpoch =
            LocalDateTime
                .parse(parsedDateTime, exifDateTimeFormat)
                .toInstant(TimeZone.currentSystemDefault())
                .epochSeconds

        return dateTimeSinceEpoch
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
        return 0L
    }
}

/** @param dateTaken is in seconds since epoch */
@OptIn(ExperimentalTime::class)
fun setDateTakenForMedia(fd: FileDescriptor, dateTaken: Long) {
    try {
        val exifInterface = ExifInterface(fd)

        val newDateString =
            Instant.fromEpochSeconds(dateTaken)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(
                    LocalDateTime.Format {
                        year()
                        char(':')
                        monthNumber()
                        char(':')
                        day()
                        char(' ')
                        hour()
                        char(':')
                        minute()
                        char(':')
                        second()
                    }
                )

        Log.d(TAG, "NEW DATE STRING $newDateString")

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME,
            newDateString
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME_ORIGINAL,
            newDateString
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_OFFSET_TIME,
            Clock.System.now().offsetIn(TimeZone.currentSystemDefault()).format(UtcOffset.Formats.ISO)
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            Clock.System.now().offsetIn(TimeZone.currentSystemDefault()).format(UtcOffset.Formats.ISO)
        )

        exifInterface.saveAttributes()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
    }
}