package com.kaii.photos.helpers

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.kaii.photos.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round

private const val TAG = "com.kaii.photos.helpers.ExifDataHandler"

fun getDateTakenForMedia(absolutePath: String): Long {
    try {
        val exifInterface = ExifInterface(absolutePath)
        val exifDateTimeFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

        val lastModified = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(File(absolutePath).lastModified()),
            ZoneId.systemDefault()
        ).format(exifDateTimeFormat)

        val datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: (exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                ?: lastModified) // this really should not get to last modified

        val parsedDateTime = datetime.replace("-", ":").replace("T", " ").substringBefore("+")
        val dateTimeSinceEpoch =
            LocalDateTime.parse(parsedDateTime, exifDateTimeFormat).atZone(ZoneId.systemDefault())
                .toEpochSecond()

        return dateTimeSinceEpoch
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        return 0L
    }
}

/** @param dateTaken is in seconds since epoch */
fun setDateTakenForMedia(absolutePath: String, dateTaken: Long) {
    try {
        val exifInterface = ExifInterface(absolutePath)
        val exifDateTimeFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

        val localDateTime =
            Instant.ofEpochSecond(dateTaken).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val datetime = localDateTime.format(exifDateTimeFormat)

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME,
            datetime
        )

        exifInterface.setAttribute(
            ExifInterface.TAG_DATETIME_ORIGINAL,
            datetime
        )

        exifInterface.saveAttributes()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
    }
}

fun getExifDataForMedia(absolutePath: String): Flow<Map<MediaData, Any>> = flow {
    val list = emptyMap<MediaData, Any?>().toMutableMap()
    val file = File(absolutePath)

    list[MediaData.Name] = file.name
    list[MediaData.Path] = file.absolutePath
    list[MediaData.Resolution] = "Loading..."

    emit(list.mapValues { (_, value) ->
        value!!
    })

    try {
        val exifInterface = ExifInterface(absolutePath)

        val datetime = getDateTakenForMedia(absolutePath)
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy - h:mm:ss a")
        val formattedDateTime =
            LocalDateTime.ofInstant(Instant.ofEpochSecond(datetime), ZoneId.systemDefault())
                .format(formatter)
        list[MediaData.Date] = formattedDateTime

        list[MediaData.LatLong] = exifInterface.latLong

        list[MediaData.Device] = exifInterface.getAttribute(ExifInterface.TAG_MODEL)

        val fNumber = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
        list[MediaData.FNumber] = if (fNumber != null) {
            "f/$fNumber"
        } else null

        val shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
        list[MediaData.ShutterSpeed] = shutterSpeed

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

        emit(
            list
                .filter { (_, value) ->
                    value != null
                }
                .mapValues { (_, value) ->
                    value!!
                }
        )

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

        emit(
            list
                .filter { (_, value) ->
                    value != null
                }
                .mapValues { (_, value) ->
                    value!!
                }
        )
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        emit(emptyMap())
    }
}

fun eraseExifMedia(absolutePath: String) {
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
}

enum class MediaData(val iconResInt: Int) {
    Name(R.drawable.name),
    Path(R.drawable.folder),
    Date(R.drawable.calendar),
    LatLong(R.drawable.location),
    Device(R.drawable.camera),
    FNumber(R.drawable.light),
    ShutterSpeed(R.drawable.shutter_speed),
    MegaPixels(R.drawable.maybe_megapixel),
    Resolution(R.drawable.resolution),
    Size(R.drawable.storage)
}
