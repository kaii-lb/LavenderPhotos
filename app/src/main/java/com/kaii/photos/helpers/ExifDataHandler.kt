package com.kaii.photos.helpers

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

fun GetDateTakenForMedia(uri: String) : Long {
    val exifInterface = ExifInterface(uri)
    val exifDateTimeFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    val lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(File(uri).lastModified()), ZoneId.systemDefault()).format(exifDateTimeFormat)
    val datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: (exifInterface.getAttribute(ExifInterface.TAG_DATETIME) ?: lastModified) // this really should not get to last modified

    val dateTimeSinceEpoch = LocalDateTime.parse(datetime, exifDateTimeFormat).atZone(ZoneId.systemDefault()).toEpochSecond()
	//println("DATE TIME IS $datetime and since epoch $dateTimeSinceEpoch")
    return dateTimeSinceEpoch
}
