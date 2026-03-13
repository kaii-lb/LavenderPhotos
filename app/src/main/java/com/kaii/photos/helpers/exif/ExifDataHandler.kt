package com.kaii.photos.helpers.exif

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.text.format.DateFormat
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.InputStream
import java.time.format.DateTimeFormatter
import kotlin.math.round
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "com.kaii.photos.helpers.ExifDataHandler"

/** @param fallback in seconds */
@OptIn(ExperimentalTime::class)
fun getExifDataForMedia(
    context: Context,
    inputStream: InputStream,
    absolutePath: String,
    fallback: Long
): Map<MediaData, Any> {
    val list = emptyMap<MediaData, Any?>().toMutableMap()
    val file = File(absolutePath)

    list[MediaData.Name] = file.name
    list[MediaData.Path] = file.absolutePath
    list[MediaData.Resolution] = "Loading..."

    try {
        val exifInterface = ExifInterface(inputStream)

        val datetime = getDateTakenForMedia(inputStream = inputStream, dateModified = fallback)
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

        inputStream.close()

        return list
            .mapNotNull { (key, value) ->
                if (value != null) key to value else null
            }
            .toMap()
    } catch (e: Throwable) {
        inputStream.close()

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