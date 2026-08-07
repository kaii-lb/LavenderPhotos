package com.kaii.photos.helpers.motion_photo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import java.io.FileNotFoundException

private const val TAG = "com.kaii.photos.helpers.MotionPhoto"

// TODO: support secure folder
@OptIn(UnstableApi::class)
class MotionPhotoState(
    private val context: Context
) {
    var isMotionPhoto by mutableStateOf(false)
        private set

    fun reset() {
        isMotionPhoto = false
    }

    suspend fun getFor(
        uri: String,
        type: MediaType
    ) = withContext(Dispatchers.IO) {
        if (type == MediaType.Video || uri.startsWith("/api")) {
            isMotionPhoto = false
            return@withContext
        }

        val xmp = getXmpData(uri.toUri())

        isMotionPhoto = if (xmp != null) {
            xmp.rdf.description.motionPhoto == 1
        } else {
            false
        }
    }

    @Suppress("DEPRECATION")
    @kotlin.OptIn(ExperimentalXmlUtilApi::class)
    private fun getXmpData(uri: Uri): XmpMeta? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val exifInterface = ExifInterface(inputStream)

            val xmpData = exifInterface.getAttribute(ExifInterface.TAG_XMP)

            if (xmpData != null) {
                val serializer = serializer<XmpMeta>()
                val xml = XML {
                    fast()
                    autoPolymorphic = true

                    // ignore unknown keys
                    val policy = DefaultXmlSerializationPolicy.BuilderCompat()
                        .apply {
                            unknownChildHandler = UnknownChildHandler { _, _, _, _, _ ->
                                emptyList()
                            }
                        }
                        .build()

                    this.policy = policy
                }

                inputStream.close()

                return xml.decodeFromString(serializer, xmpData)
            }

            inputStream.close()
        } catch (_: XmlParsingException) {
        } catch (e: FileNotFoundException) {
            Log.d(TAG, e.message.toString())
        }

        return null
    }
}

@Composable
fun rememberMotionPhotoState(): MotionPhotoState {
    val context = LocalContext.current

    return remember(context) {
        MotionPhotoState(
            context = context
        )
    }
}