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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import kotlin.time.Duration.Companion.milliseconds

// TODO: support secure folder
@OptIn(UnstableApi::class)
class MotionPhotoState(
    private val context: Context
) {
    companion object {
        private val TAG = MotionPhotoState::class.qualifiedName
    }

    var isMotionPhoto by mutableStateOf(false)
        private set

    private sealed interface ParsingState {
        data class Parsed(val xmpMeta: XmpMeta) : ParsingState
        object HasNone : ParsingState
        object Failed : ParsingState
    }

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

        var trialCount = 0
        var parsedData = parseXmpFromUri(uri.toUri())

        while (parsedData is ParsingState.Failed && trialCount < 5) {
            trialCount += 1
            parsedData = parseXmpFromUri(uri.toUri())
            delay(500.milliseconds)
        }

        isMotionPhoto = if (parsedData is ParsingState.Parsed) {
            parsedData.xmpMeta.rdf.description.motionPhoto == 1
        } else {
            false
        }
    }

    @Suppress("DEPRECATION")
    @kotlin.OptIn(ExperimentalXmlUtilApi::class)
    private fun parseXmpFromUri(uri: Uri): ParsingState =
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ParsingState.Failed
            val exifInterface = ExifInterface(inputStream)

            val xmpData = exifInterface.getAttribute(ExifInterface.TAG_XMP)

            if (xmpData == null) {
                inputStream.close()
                return ParsingState.HasNone
            }

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

            return ParsingState.Parsed(
                xmpMeta = xml.decodeFromString(serializer, xmpData)
            )
        } catch (e: XmlParsingException) {
            Log.d(TAG, "Xml parsing failed. ${e.message}")

            ParsingState.Failed
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Cannot access content $uri. ${e.message}")
            ParsingState.Failed
        } catch (e: Throwable) {
            Log.d(TAG, e.message.toString())
            ParsingState.HasNone
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