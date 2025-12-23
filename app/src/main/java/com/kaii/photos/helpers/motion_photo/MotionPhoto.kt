package com.kaii.photos.helpers.motion_photo

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML

// private const val TAG = "com.kaii.photos.helpers.MotionPhoto"

@OptIn(UnstableApi::class)
class MotionPhoto(
    val uri: Uri,
    private val context: Context
) {
    var isMotionPhoto: Boolean = false
    var photoLength: Int = 0
    var photoPadding: Int = 0

    var videoLength: Int = 0
    var videoPadding: Int = 0

    init {
        val xmp = getXmpData()

        if (xmp != null) {
            isMotionPhoto = xmp.rdf.description.motionPhoto == 1

            xmp.rdf.description.directory?.seq?.items?.forEach { (_, item) ->
                if (MimeTypes.isVideo(item.mime)) {
                    videoLength = item.length ?: 0
                    videoPadding = item.padding ?: 0
                } else if (MimeTypes.isImage(item.mime)) {
                    photoLength = item.length ?: 0
                    photoPadding = item.padding ?: 0
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @kotlin.OptIn(ExperimentalXmlUtilApi::class)
    private fun getXmpData(): XmpMeta? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val exifInterface = ExifInterface(inputStream)

        val xmpData = exifInterface.getAttribute(ExifInterface.TAG_XMP)

        if (xmpData == null) return null

        val serializer = serializer<XmpMeta>()
        val xml = XML {
            autoPolymorphic = true

            // ignore unknown keys
            unknownChildHandler = UnknownChildHandler { _, _, _, _, _ ->
                emptyList()
            }
        }

        inputStream.close()

        return xml.decodeFromString(serializer, xmpData)
    }
}