package com.kaii.photos.helpers.exif

import android.util.Log
import com.kaii.photos.database.entities.ExifData
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round
import kotlin.time.Instant

private const val TAG = "com.kaii.photos.helpers.exif.Immich"

/** @param fallback in seconds */
fun exifDataToMediaData(
    name: String,
    path: String,
    info: ExifData,
    is24Hr: Boolean,
    fallback: Long
): Map<MediaData, String> {
    val list = emptyMap<MediaData, String?>().toMutableMap()

    list[MediaData.Name] = name
    list[MediaData.Path] = path
    list[MediaData.Resolution] = "${info.exifImageWidth}x${info.exifImageHeight}"

    try {
        val parsedDateTime = info.dateTimeOriginal?.replace("T", " ")?.let {
            it.substringBefore(" ").replace(":", "-") +
                    "T" + it.substringAfter(" ").substringBefore("+")
        }

        val seconds =
            parsedDateTime?.let {
                LocalDateTime
                    .parse(it, LocalDateTime.Formats.ISO)
                    .toInstant(TimeZone.currentSystemDefault())
                    .epochSeconds
            } ?: fallback

        val formattedDateTime =
            Instant.fromEpochSeconds(seconds)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toJavaLocalDateTime()
                .format(
                    DateTimeFormatter.ofPattern(
                        if (is24Hr) "EEE dd MMM yyyy - HH:mm:ss"
                        else "EEE dd MMM yyyy - h:mm:ss a"
                    )
                )

        list[MediaData.Date] = formattedDateTime

        list[MediaData.LatLong] = info.latitude?.let { "${info.latitude} ${info.longitude}" }

        list[MediaData.Device] = info.model

        list[MediaData.FNumber] = info.fNumber?.let { "f/$it" }

        list[MediaData.ShutterSpeed] = info.exposureTime?.toDouble()?.toFraction()

        list[MediaData.Size] = info.fileSizeInByte?.let { bytes ->
            if (bytes < 1000000) { // less than a mb display in kb
                val kb = round(bytes * 10 / 1000f) / 10
                "$kb KB"
            } else {
                val mb = round(bytes / 100000f) / 10
                "$mb MB"
            }
        }

        list[MediaData.MegaPixels] = list[MediaData.Resolution]?.let {
            val split = it.split("x")
            val x = split[0].toInt()
            val y = split[1].toInt()

            round((x * y) / 100000f) / 10f // divide by 1mil then multiply by 10, so divide by 100k
        }?.toString()

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