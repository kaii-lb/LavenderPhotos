package com.kaii.photos.helpers.motion_photo

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlParsingException

// private const val TAG = "com.kaii.photos.helpers.MotionPhoto"


// TODO: support secure folder
@OptIn(UnstableApi::class)
class MotionPhoto(
    val uri: Uri,
    private val context: Context,
    coroutineScope: CoroutineScope
) {
    var isMotionPhoto = mutableStateOf(false)

    init {
        coroutineScope.launch(Dispatchers.IO) {
            val xmp = getXmpData()

            if (xmp != null) {
                isMotionPhoto.value = xmp.rdf.description.motionPhoto == 1
            }
        }
    }

    @Suppress("DEPRECATION")
    @kotlin.OptIn(ExperimentalXmlUtilApi::class)
    private fun getXmpData(): XmpMeta? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val exifInterface = ExifInterface(inputStream)

            val xmpData = exifInterface.getAttribute(ExifInterface.TAG_XMP)

            if (xmpData != null) {
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
        } catch (_: XmlParsingException) {}

        return null
    }
}

@Composable
fun rememberMotionPhoto(
    uri: Uri
): MotionPhoto {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember(uri) {
        MotionPhoto(
            uri = uri,
            context = context,
            coroutineScope = coroutineScope
        )
    }
}