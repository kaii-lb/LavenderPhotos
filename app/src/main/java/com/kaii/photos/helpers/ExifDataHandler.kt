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

private const val TAG = "EXIF_DATA_HANDLER"

fun getDateTakenForMedia(uri: String): Long {
    try {
        val exifInterface = ExifInterface(uri)
        val exifDateTimeFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

        val lastModified = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(File(uri).lastModified()),
            ZoneId.systemDefault()
        ).format(exifDateTimeFormat)
        val datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: (exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                ?: lastModified) // this really should not get to last modified

        val parsedDateTime = datetime.replace("-", ":").replace("T", " ").substringBefore("+")
        val dateTimeSinceEpoch =
            LocalDateTime.parse(parsedDateTime, exifDateTimeFormat).atZone(ZoneId.systemDefault())
                .toEpochSecond()
        // println("DATE TIME IS $parsedDateTime and since epoch $dateTimeSinceEpoch")
        return dateTimeSinceEpoch
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        return 0L
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

        list[MediaData.Size] = "${round(file.length() / 100000f) / 10} MB"

        emit(
            list.filter { (_, value) ->
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
            list.filter { (_, value) ->
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

fun copyExifDataToFile(originalFilePath: String, targetFilePath: String) {
    val originalExifInterface = ExifInterface(originalFilePath)

    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(originalFilePath, options)

    val exifInterface = ExifInterface(targetFilePath)

    exifInterface.setAttribute(
        ExifInterface.TAG_X_RESOLUTION,
        options.outWidth.toString()
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_Y_RESOLUTION,
        options.outHeight.toString()
    )

    exifInterface.setLatLong(
        (originalExifInterface.latLong?.get(0) ?: 0).toDouble(),
        (originalExifInterface.latLong?.get(1) ?: 0).toDouble()
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_MODEL,
        originalExifInterface.getAttribute(ExifInterface.TAG_MODEL)
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_F_NUMBER,
        originalExifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_DATETIME,
        originalExifInterface.getAttribute(ExifInterface.TAG_DATETIME)
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_DATETIME_ORIGINAL,
        originalExifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        originalExifInterface.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
    )

    exifInterface.setAttribute(
        ExifInterface.TAG_EXPOSURE_TIME,
        originalExifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
    )
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
