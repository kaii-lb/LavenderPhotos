package com.kaii.photos.helpers.editing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
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
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

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
    textMeasurer: TextMeasurer,
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

    val filter = modifications.lastOrNull {
        it is VideoModification.Filter
    } as? VideoModification.Filter

    if (filter != null) {
        modList.add(
            filter.type.toEffect()
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

    val overlayEffects = mutableListOf<BitmapOverlay>()
    val textOverlays =
        modifications.mapNotNull {
            it as? VideoModification.DrawingText
        }
    if (textOverlays.isNotEmpty()) {
        textOverlays.forEach { overlay ->
            overlayEffects.add(
                overlay.type.toEffect(
                    value = overlay,
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    resolution = canvasSize,
                    textMeasurer = textMeasurer
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
                        resolution = canvasSize,
                        textMeasurer = textMeasurer
                    )
                } else {
                    overlay.type.toEffect(
                        value = overlay.path,
                        timespan = overlay.timespan,
                        ratio = ratio,
                        context = context,
                        resolution = canvasSize,
                        textMeasurer = textMeasurer
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
                    value = overlay,
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    resolution = canvasSize,
                    textMeasurer = textMeasurer
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
        .setEncoderFactory(
            DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(videoEditingState.bitrate)
                        .build()
                )
                .build()
        )
        .build()

    transformer.start(
        editedMediaItem,
        neededPath
    )
}

suspend fun saveImage(
    context: Context,
    image: ImageBitmap,
    absolutePath: String,
    containerDimens: Size,
    drawingPaintState: DrawingPaintState,
    imageEditingState: ImageEditingState,
    modifications: List<ImageModification>,
    textMeasurer: TextMeasurer,
    actualLeft: Float,
    actualTop: Float,
    overwrite: Boolean
) {
    val isLoading = mutableStateOf(true)

    LavenderSnackbarController.pushEvent(
        LavenderSnackbarEvents.LoadingEvent(
            message = context.resources.getString(R.string.editing_saving),
            icon = R.drawable.image_arrow_up,
            isLoading = isLoading
        )
    )

    val images = drawingPaintState.modifications
        .fastMapNotNull { mod ->
            mod as? SharedModification.DrawingImage
        }
        .fastMap { mod ->
            context.contentResolver.openInputStream(mod.image.bitmapUri).use { inputStream ->
                Pair(mod.image.bitmapUri, BitmapFactory.decodeStream(inputStream).asImageBitmap())
            }
        }

    val crop =
        imageEditingState.modificationList.lastOrNull {
            it is ImageModification.Crop
        } as? ImageModification.Crop ?: ImageModification.Crop(0f, 0f, 0f, 0f)

    val left = (crop.left / containerDimens.width)
    val top = (crop.top / containerDimens.height)
    val width = (crop.width / containerDimens.width)
    val height = (crop.height / containerDimens.height)

    val bitmap = Bitmap
        .createBitmap(
            image.asAndroidBitmap(),
            (left * image.width).roundToInt(),
            (top * image.height).roundToInt(),
            (width * image.width).roundToInt(),
            (height * image.height).roundToInt()
        )
        .copy(Bitmap.Config.ARGB_8888, true)
        .asImageBitmap()

    val ratio = min(bitmap.width / containerDimens.width, bitmap.height / containerDimens.height)

    val adjustmentCanvas = Canvas(bitmap)
    val mods = modifications + drawingPaintState.modifications
    val sorted = mods.sortedBy { mod ->
        if (mod is ImageModification.Adjustment) {
            MediaAdjustments.entries.indexOf(mod.type)
        } else {
            MediaAdjustments.entries.size + 1
        }
    }

    val drawScope = CanvasDrawScope()
    sorted.forEach { mod ->
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = adjustmentCanvas,
            size = Size(
                width = bitmap.width.toFloat(),
                height = bitmap.height.toFloat()
            )
        ) {
            if (mod is ImageModification.Filter) {
                drawImage(
                    image = bitmap,
                    colorFilter = ColorFilter.colorMatrix(mod.type.matrix)
                )
            } else if (mod is ImageModification.Adjustment) {
                drawImage(
                    image = bitmap,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(mod.type.getMatrix(mod.value)))
                )
            }
        }
    }

    val canvas = Canvas(bitmap)

    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat()
        )
    ) {
        scale(scale = ratio, pivot = Offset.Zero) {
            drawingPaintState.modifications.forEach { modification ->
                when (modification) {
                    is SharedModification.DrawingPath -> {
                        val path = modification.path

                        drawPath(
                            path = path.path,
                            style = Stroke(
                                width = path.paint.strokeWidth,
                                cap = path.paint.strokeCap,
                                join = path.paint.strokeJoin,
                                miter = path.paint.strokeMiterLimit,
                                pathEffect = path.paint.pathEffect
                            ),
                            blendMode = path.paint.blendMode,
                            color = path.paint.color,
                            alpha = path.paint.alpha
                        )
                    }

                    is SharedModification.DrawingText -> {
                        val text = modification.text

                        rotate(text.rotation, text.position + text.size.toOffset() / 2f) {
                            translate(text.position.x, text.position.y) {
                                val textLayout = textMeasurer.measure(
                                    text = text.text,
                                    style = DrawableText.Styles.Default.copy(
                                        color = text.paint.color,
                                        fontSize = text.paint.strokeWidth.sp
                                    ),
                                    softWrap = false
                                )

                                drawText(
                                    textLayoutResult = textLayout,
                                    blendMode = text.paint.blendMode
                                )

                                if (drawingPaintState.selectedItem == modification) {
                                    drawRoundRect(
                                        color = text.paint.color,
                                        topLeft = text.size.toOffset().copy(y = 0f) * -0.1f / 2f,
                                        cornerRadius = CornerRadius(16.dp.toPx() * text.paint.strokeWidth / 128f),
                                        size = text.size.toSize() * 1.1f,
                                        style = Stroke(
                                            width = text.paint.strokeWidth / 2,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is SharedModification.DrawingImage -> {
                        val image = modification.image

                        rotate(image.rotation, image.position + image.size.toOffset() / 2f) {
                            translate(image.position.x, image.position.y) {
                                drawImage(
                                    image = images.firstOrNull { it.first == image.bitmapUri }?.second ?: ImageBitmap(512, 512),
                                    dstSize = image.size,
                                    filterQuality = FilterQuality.Medium,
                                    blendMode = image.paint.blendMode
                                )

                                if (drawingPaintState.selectedItem == modification) {
                                    drawRoundRect(
                                        color = image.paint.color,
                                        cornerRadius = CornerRadius(16.dp.toPx() * image.paint.strokeWidth / 128f),
                                        size = image.size.toSize(),
                                        style = Stroke(
                                            width = image.paint.strokeWidth / 2,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val file = File(absolutePath)
    val uri = context.contentResolver.getUriFromAbsolutePath(absolutePath, MediaType.Image)

    if (uri == null) {
        isLoading.value = false

        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvents.MessageEvent(
                message = context.resources.getString(R.string.editing_failed),
                icon = R.drawable.broken_image,
                duration = SnackbarDuration.Short
            )
        )

        return
    }

    val media = MediaStoreData(
        displayName = file.name,
        absolutePath = file.absolutePath,
        dateTaken = System.currentTimeMillis() / 1000,
        dateModified = System.currentTimeMillis() / 1000,
        type = MediaType.Image,
        mimeType = "image/png",
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

    if (newUri == null) {
        isLoading.value = false

        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvents.MessageEvent(
                message = context.resources.getString(R.string.editing_failed),
                icon = R.drawable.broken_image,
                duration = SnackbarDuration.Short
            )
        )

        return
    }

    val newMedia = context.contentResolver.getMediaStoreDataFromUri(newUri)
    val neededPath = newMedia?.absolutePath
    if (neededPath == null) {
        isLoading.value = false

        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvents.MessageEvent(
                message = context.resources.getString(R.string.editing_failed),
                icon = R.drawable.broken_image,
                duration = SnackbarDuration.Short
            )
        )

        return
    }

    context.contentResolver.openOutputStream(newMedia.uri)?.use { outputStream ->
        bitmap.asAndroidBitmap().compress(
            Bitmap.CompressFormat.PNG,
            100,
            outputStream
        )
    } ?: run {
        isLoading.value = false

        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvents.MessageEvent(
                message = context.resources.getString(R.string.editing_failed),
                icon = R.drawable.broken_image,
                duration = SnackbarDuration.Short
            )
        )
    }

    isLoading.value = false
}