package com.kaii.photos.helpers.exif

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.text.format.DateFormat
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.kaii.photos.R
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.FileDescriptor
import java.time.format.DateTimeFormatter
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "com.kaii.photos.helpers.ExifDataHandler"

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

/** @param dateTaken is in seconds since epoch */
@OptIn(ExperimentalTime::class)
fun setDateTakenForMedia(fd: FileDescriptor, dateTaken: Long) {
    try {
        val exifInterface = ExifInterface(fd)

        val newDateString =
            Instant.fromEpochSeconds(dateTaken)
                .format(
                    DateTimeComponents.Format {
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

        exifInterface.saveAttributes()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
    }
}

/** @param dateModified in seconds */
@OptIn(ExperimentalTime::class)
fun getExifDataForMedia(context: Context, absolutePath: String, dateModified: Long): Map<MediaData, Any> {
    val list = emptyMap<MediaData, Any?>().toMutableMap()
    val file = File(absolutePath)

    list[MediaData.Name] = file.name
    list[MediaData.Path] = file.absolutePath
    list[MediaData.Resolution] = "Loading..."

    try {
        val exifInterface = ExifInterface(absolutePath)

        val datetime = getDateTakenForMedia(absolutePath, dateModified)
        val is24Hr = DateFormat.is24HourFormat(context)
        val formattedDateTime =
            Instant.fromEpochSeconds(datetime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(
                    DateTimeFormatter.ofPattern(
                        if (is24Hr) "EEE dd MMM yyyy - HH:mm:ss"
                        else "EEE dd MMM yyyy - h:mm:ss a"
                    )
                )

        list[MediaData.Date] = formattedDateTime

        list[MediaData.LatLong] = exifInterface.latLong

        list[MediaData.Device] = exifInterface.getAttribute(ExifInterface.TAG_MODEL)

        val fNumber = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
        list[MediaData.FNumber] = if (fNumber != null) {
            "f/$fNumber"
        } else null

        val shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        list[MediaData.ShutterSpeed] = shutterSpeed?.toDouble()?.toFraction()

        val size = file.length().let { bytes ->
            if (bytes < 1000000) { // less than a mb display in kb
                val kb = round(bytes * 10 / 1000f) / 10
                "$kb KB"
            } else {
                val mb = round(bytes / 100000f) / 10
                "$mb MB"
            }
        }
        list[MediaData.Size] = size

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(absolutePath, options)
        val resValue = run {
            if (options.outWidth == -1 && options.outHeight == -1) {
                val metadataRetriever = MediaMetadataRetriever()
                metadataRetriever.setDataSource(absolutePath)

                val resX = metadataRetriever.frameAtTime?.width ?: -1
                val resY = metadataRetriever.frameAtTime?.height ?: -1

                "${resX}x${resY}"
            } else {
                "${options.outWidth}x${options.outHeight}"
            }
        }

        list[MediaData.Resolution] = resValue

        list[MediaData.MegaPixels] = run {
            val split = resValue.split("x")
            val x = split[0].toInt()
            val y = split[1].toInt()

            round((x * y) / 100000f) / 10f // divide by 1mil then multiply by 10, so divide by 100k
        }

        return list
            .mapNotNull { (key, value) ->
                if (value != null) key to value else null
            }
            .toMap()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()

        return emptyMap()
    }
}

fun eraseExifMedia(absolutePath: String) {
    try {
        val exifInterface = ExifInterface(absolutePath)

        exifInterface.setLatLong(0.0, 0.0)

        exifInterface.setAttribute(
            ExifInterface.TAG_MODEL,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_F_NUMBER,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME_ORIGINAL,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_SHUTTER_SPEED_VALUE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_EXPOSURE_TIME,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_ARTIST,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_CAMERA_OWNER_NAME,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_GPS_ALTITUDE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_GPS_LONGITUDE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_GPS_LATITUDE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.LATITUDE_NORTH,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.LATITUDE_SOUTH,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.LONGITUDE_EAST,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.LONGITUDE_WEST,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_USER_COMMENT,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME_DIGITIZED,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_MAKE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_MAKER_NOTE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_LENS_MAKE,
            null
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_LENS_MODEL,
            null
        )

        exifInterface.saveAttributes()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
    }
}

enum class MediaData(val icon: Int, val description: Int) {
    Name(icon = R.drawable.name, description = R.string.exif_name),
    Path(icon = R.drawable.folder, description = R.string.exif_path),
    Date(icon = R.drawable.calendar, description = R.string.exif_date),
    LatLong(icon = R.drawable.location, description = R.string.exif_latlong),
    Device(icon = R.drawable.camera, description = R.string.exif_device),
    FNumber(icon = R.drawable.light, description = R.string.exif_fnumber),
    ShutterSpeed(icon = R.drawable.shutter_speed, description = R.string.exif_shutter_speed),
    MegaPixels(icon = R.drawable.maybe_megapixel, description = R.string.exif_mp),
    Resolution(icon = R.drawable.resolution, description = R.string.exif_res),
    Size(icon = R.drawable.storage, description = R.string.exif_size)
}
