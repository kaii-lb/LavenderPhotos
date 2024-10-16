package com.kaii.photos.helpers

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round
import com.kaii.photos.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

private const val TAG = "EXIT_DATA_HANDLER"

fun getDateTakenForMedia(uri: String) : Long {
	try {	
	    val exifInterface = ExifInterface(uri)
	    val exifDateTimeFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

	    val lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(File(uri).lastModified()), ZoneId.systemDefault()).format(exifDateTimeFormat)
	    val datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
	        ?: (exifInterface.getAttribute(ExifInterface.TAG_DATETIME) ?: lastModified) // this really should not get to last modified

	    val parsedDateTime = datetime.replace("-", ":").replace("T", " ").substringBefore("+")
	    val dateTimeSinceEpoch = LocalDateTime.parse(parsedDateTime, exifDateTimeFormat).atZone(ZoneId.systemDefault()).toEpochSecond()
		// println("DATE TIME IS $parsedDateTime and since epoch $dateTimeSinceEpoch")
	    return dateTimeSinceEpoch
	} catch (e: Throwable) {
		Log.e(TAG, e.toString())
		return 0L
	}
}

fun getExifDataForMedia(uri: String): Map<MediaData, Any> {
    val exifInterface = ExifInterface(uri)

    val list = HashMap<MediaData, Any?>().toMutableMap()
    val file = File(uri)

    list[MediaData.Name] = file.name
    list[MediaData.Path] = file.absolutePath

    val datetime = getDateTakenForMedia(uri)
    val formatter = DateTimeFormatter.ofPattern("h:mm:ss a - d MMM yyyy")
    val formattedDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(datetime), ZoneId.systemDefault()).format(formatter)
    list[MediaData.Date] = formattedDateTime

    list[MediaData.LatLong] = exifInterface.latLong

    val res = Pair(exifInterface.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION), exifInterface.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION))
    val resValue = if (res.first != null && res.second != null) {
        val x = res.first!!.toIntOrNull()
        val y = res.second!!.toIntOrNull()

        if (x != null && y != null) {
            "${x}x$y"
        } else null
    } else null
    list[MediaData.Resolution] = resValue

    list[MediaData.Device] = exifInterface.getAttribute(ExifInterface.TAG_MODEL)

    val fNumber = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
    list[MediaData.FNumber] = if (fNumber != null) {
       	"f/$fNumber"
    } else null

    val shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
    list[MediaData.ShutterSpeed] =
        if (shutterSpeed != null) {
            val splits = shutterSpeed.split("/")
            val apexValue = splits[0].toFloat() / splits[1].toFloat()
            2.0.pow((-apexValue).toDouble()).toFraction()
        } else null

    list[MediaData.MegaPixels] = if (resValue != null) {
        val split = resValue.split("x")
        val x = split[0].toInt()
        val y = split[1].toInt()

        round((x * y) / 100000f) / 10f // divide by 1mil then multiply by 100, so divide by 100k
    } else null

    list[MediaData.Size] = "${round(file.length() / 100000f) / 10} MB"

    val nonNullList = HashMap<MediaData, Any>().toMutableMap()

    list.forEach { (key, value) ->
        if (value != null) nonNullList[key] = value
    }

    return nonNullList
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
    Size(R.drawable.storage),
}

fun Double.toFraction(): String {
    val tolerance = 1.0E-6
    var h1 = 1.0
    var h2 = 0.0
    var k1 = 0.0
    var k2 = 1.0
    var b = this

    do {
        val a = floor(b)
        var aux = h1
        h1 = a * h1 + h2
        h2 = aux

        aux = k1
        k1 = a * k1 + k2
        k2 = aux

        b = 1/(b-a)
    } while (abs(this-h1/k1) > this * tolerance)

    return "$h1/$k1"
}
