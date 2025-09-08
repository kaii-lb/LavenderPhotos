package com.kaii.photos.helpers.editing

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import com.kaii.photos.mediastore.insertMedia
import java.io.File
import kotlin.math.max
import kotlin.math.pow

private const val TAG = "com.kaii.photos.helpers.editing.FileHandling"

@OptIn(UnstableApi::class)
suspend fun saveVideo(
    context: Context,
    modifications: List<VideoModification>,
    videoEditingState: VideoEditingState,
    basicVideoData: BasicVideoData,
    uri: Uri,
    absolutePath: String,
    overwrite: Boolean,
    containerDimens: Size,
    canvasSize: Size,
    onFailure: () -> Unit
) {
    val isLoading = mutableStateOf(true)

    LavenderSnackbarController.pushEvent(
        LavenderSnackbarEvents.LoadingEvent(
            message = context.resources.getString(R.string.editing_export_video_loading),
            icon = R.drawable.videocam,
            isLoading = isLoading
        )
    )

    val trimPositions = (modifications.lastOrNull {
        it is VideoModification.Trim
    } ?: VideoModification.Trim(start = 0f, end = 0f)) as VideoModification.Trim

    val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs((trimPositions.start * 1000f).toLong())
        .setEndPositionMs((trimPositions.end * 1000f).toLong())
        .build()

    val modList = mutableListOf<Effect>()

    val cropArea = modifications.lastOrNull {
        it is VideoModification.Crop
    } as? VideoModification.Crop

    if (cropArea != null && !cropArea.left.isNaN() && !cropArea.right.isNaN() && !cropArea.top.isNaN() && !cropArea.bottom.isNaN()) {
        val left = (cropArea.left / containerDimens.width).coerceIn(-1f, 1f)
        val right = (cropArea.right / containerDimens.width).coerceIn(-1f, 1f)
        val top = (cropArea.top / containerDimens.height).coerceIn(-1f, 1f)
        val bottom = (cropArea.bottom / containerDimens.height).coerceIn(-1f, 1f)

        val normalizedLeft = (2f * left) - 1f
        val normalizedRight = (2f * right) - 1f
        val normalizedTop = -(2f * top) + 1f
        val normalizedBottom = -(2f * bottom) + 1f

        Log.d(TAG, "$normalizedTop $normalizedBottom ${cropArea.top / containerDimens.height} ${cropArea.bottom / containerDimens.height}")

        modList.add(
            Crop(
                normalizedLeft,
                normalizedRight,
                normalizedBottom,
                normalizedTop
            )
        )

        val widthPercent = right - left
        val heightPercent = bottom - top

        val newWidth = (basicVideoData.width * widthPercent).toInt()
        val newHeight = (basicVideoData.height * heightPercent).toInt()

        Log.d(TAG, "New width $newWidth and height $newHeight")

        modList.add(
            Presentation.createForShortSide(if (newWidth > newHeight) newWidth else newHeight)
        )
    }

    if (videoEditingState.rotation != 0f) {
        modList.add(
            ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(-videoEditingState.rotation) // negative since our rotation is clockwise
                .build()
        )
    }

    val mediaItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(clippingConfiguration)
        .build()

    val audioEffectList = mutableListOf<AudioProcessor>()

    // try to "linearize" as best as possible
    val audioProcessor = ChannelMixingAudioProcessor()
    val minDb = -40f
    val maxDb = 0f

    val dbChange = minDb + (maxDb - minDb) * videoEditingState.volume
    val linearGain = 10f.pow(dbChange / 20f)

    audioProcessor.putChannelMixingMatrix(
        ChannelMixingMatrix.createForConstantGain(2, 2).scaleBy(linearGain)
    )

    audioEffectList.add(audioProcessor)

    if (videoEditingState.speed != 1f) {
        modList.add(
            SpeedChangeEffect(videoEditingState.speed)
        )
        audioEffectList.add(SonicAudioProcessor().apply {
            setSpeed(videoEditingState.speed)
        })
    }

    val ratio =
        max(
            basicVideoData.width / containerDimens.width,
            basicVideoData.height / containerDimens.height
        ) / (containerDimens.width / containerDimens.height)

    Log.d(TAG, "Ratio $ratio other ${containerDimens.width / containerDimens.height}")

    val overlayEffects = mutableListOf<BitmapOverlay>()
    val textOverlays =
        modifications.mapNotNull {
            it as? VideoModification.DrawingText
        }
    if (textOverlays.isNotEmpty()) {
        textOverlays.forEach { overlay ->
            overlayEffects.add(
                overlay.type.toEffect(
                    value = DrawableText(
                        text = overlay.text.text,
                        position = Offset(0f, 0f),
                        paint = DrawingPaint(strokeWidth = overlay.text.paint.strokeWidth, color = overlay.text.paint.color),
                        rotation = overlay.text.rotation,
                        size = IntSize.Companion.Zero
                    ),
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    resolution = canvasSize
                )
            )
        }
    }

    val pathOverlays =
        modifications.mapNotNull {
            it as? VideoModification.DrawingPath
        }
    if (pathOverlays.isNotEmpty()) {
        pathOverlays.forEach { overlay ->
            val effect =
                if (overlay.type == DrawingItems.Pencil) {
                    overlay.type.toEffect(
                        value = overlay.path,
                        timespan = overlay.timespan,
                        ratio = ratio,
                        context = context,
                        resolution = canvasSize
                    )
                } else {
                    overlay.type.toEffect(
                        value = overlay.path,
                        timespan = overlay.timespan,
                        ratio = ratio,
                        context = context,
                        resolution = canvasSize
                    )
                }

            overlayEffects.add(effect)
        }
    }

    val bitmapOverlays =
        modifications.mapNotNull {
            it as? VideoModification.DrawingImage
        }

    if (bitmapOverlays.isNotEmpty()) {
        bitmapOverlays.forEach { overlay ->
            val effect =
                overlay.type.toEffect(
                    value = overlay.image,
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    resolution = canvasSize
                )

            overlayEffects.add(effect)
        }
    }

    val overlayEffectsList = listOf(
        OverlayEffect(
            overlayEffects.toList()
        )
    )

    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
        .setEffects(Effects(audioEffectList, modList + videoEditingState.effectList + overlayEffectsList))
        .build()

    val file = File(absolutePath)
    val uri = context.contentResolver.getUriFromAbsolutePath(absolutePath, MediaType.Video)

    if (uri == null) return

    val media = MediaStoreData(
        displayName = file.name,
        absolutePath = file.absolutePath,
        dateTaken = System.currentTimeMillis() / 1000,
        dateModified = System.currentTimeMillis() / 1000,
        type = MediaType.Video,
        mimeType = "video/mp4",
        uri = uri
    )

    val newUri =
        if (!overwrite) {
            context.contentResolver.insertMedia(
                context = context,
                media = media,
                destination = absolutePath.getParentFromPath(),
                overwriteDate = true,
                basePath = absolutePath.toBasePath(),
                currentVolumes = MediaStore.getExternalVolumeNames(context),
                overrideDisplayName = file.name.removeSuffix(file.extension) + "mp4",
                onInsert = { _, _ -> }
            )
        } else {
            uri
        }

    if (newUri == null) return

    val newMedia = context.contentResolver.getMediaStoreDataFromUri(newUri)
    val neededPath = newMedia?.absolutePath
    if (neededPath == null) return

    // because: java.io.FileNotFoundException open failed: EEXIST (File exists)
    // if the file exists
    permanentlyDeletePhotoList(context = context, list = listOf(newMedia.uri))

    val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                super.onCompleted(composition, exportResult)

                isLoading.value = false

                // not using newMedia.uri since we don't have access to that after deletion
                context.contentResolver.getUriFromAbsolutePath(absolutePath = neededPath, type = MediaType.Video)?.let { newUri ->
                    context.contentResolver.update(
                        newUri,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        },
                        null
                    )
                    context.contentResolver.notifyChange(newUri, null)
                }
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                super.onError(composition, exportResult, exportException)

                Log.d(TAG, exportException.message.toString())
                exportException.printStackTrace()

                onFailure()
            }
        })
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .build()

    transformer.start(
        editedMediaItem,
        neededPath
    )
}