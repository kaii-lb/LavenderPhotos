package com.kaii.photos.file_management.editing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExoPlayerAssetLoader
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.kaii.photos.R
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawableText
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.SharedModification
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.toOffset
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

interface GenericFileEditor {
    companion object {
        private val TAG = GenericFileEditor::class.qualifiedName
    }

    data class VideoEditResult(
        val newUri: Uri,
        val tempPath: String?,
        val deletedUri: Uri?,
    )

    sealed interface EditParameters {
        data class Image(
            val context: Context,
            val image: ImageBitmap,
            val uri: String,
            val containerDimens: Size,
            val exportQuality: Int,
            val drawingPaintState: DrawingPaintState,
            val imageEditingState: ImageEditingState,
            val modifications: List<ImageModification>,
            val textMeasurer: TextMeasurer,
            val actualLeft: Float,
            val actualTop: Float,
            val overwrite: Boolean,
            val isFromOpenWithView: Boolean
        ) : EditParameters

        data class Video(
            val context: Context,
            val modifications: List<VideoModification>,
            val videoEditingState: VideoEditingState,
            val basicVideoData: BasicVideoData,
            val uri: String,
            val info: ImmichBasicInfo,
            val overwrite: Boolean,
            val containerDimens: Size,
            val canvasSize: Size,
            val textMeasurer: TextMeasurer,
            val isFromOpenWithView: Boolean
        ) : EditParameters
    }

    val mediaDao: MediaDao

    suspend fun editVideo(
        context: Context,
        modifications: List<VideoModification>,
        videoEditingState: VideoEditingState,
        basicVideoData: BasicVideoData,
        uri: String,
        info: ImmichBasicInfo,
        overwrite: Boolean,
        containerDimens: Size,
        canvasSize: Size,
        textMeasurer: TextMeasurer,
        isFromOpenWithView: Boolean
    ): Long?

    @OptIn(UnstableApi::class)
    suspend fun editVideoImpl(
        context: Context,
        modifications: List<VideoModification>,
        videoEditingState: VideoEditingState,
        basicVideoData: BasicVideoData,
        media: MediaStoreData,
        info: ImmichBasicInfo,
        overwrite: Boolean,
        containerDimens: Size,
        canvasSize: Size,
        textMeasurer: TextMeasurer,
        percentage: MutableFloatState,
        body: MutableState<String>,
        totalPercentage: Float,
        progressHolder: ProgressHolder
    ): VideoEditResult? = withContext(Dispatchers.Main) {
        val trimPositions = (modifications.lastOrNull {
            it is VideoModification.Trim
        } ?: VideoModification.Trim(start = 0f, end = 0f)) as VideoModification.Trim

        val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs((trimPositions.start * 1000f).toLong())
            .setEndPositionMs((trimPositions.end * 1000f).toLong())
            .build()

        val modList = mutableListOf<Effect>()

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
            .setUri(
                if (media.isCloud) {
                    info.endpoint + media.immichVideoUrl!!
                } else {
                    media.uri
                }
            )
            .setClippingConfiguration(clippingConfiguration)
            .build()

        val audioEffectList = mutableListOf<AudioProcessor>()

        // try to "linearize" as best as possible
        val audioProcessor = ChannelMixingAudioProcessor()
        val minDb = -40f
        val maxDb = 0f

        val dbChange = minDb + (maxDb - minDb) * videoEditingState.volume
        val linearGain = 10f.pow(dbChange / 20f)

        Log.d(TAG, "Item has ${basicVideoData.audioChannelCount} audio channels.")

        audioProcessor.putChannelMixingMatrix(
            ChannelMixingMatrix.createForConstantGain(
                basicVideoData.audioChannelCount,
                basicVideoData.audioChannelCount
            ).scaleBy(linearGain)
        )

        audioEffectList.add(audioProcessor)

        val ratio =
            max(
                basicVideoData.width / containerDimens.width,
                basicVideoData.height / containerDimens.height
            )

        val overlayEffects = mutableListOf<BitmapOverlay>()

        modifications.forEach { overlay ->
            val effect = when (overlay) {
                is VideoModification.DrawingText -> overlay.type.toEffect(
                    value = overlay,
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    externalCanvasSize = canvasSize,
                    textMeasurer = textMeasurer
                )

                is VideoModification.DrawingPath -> overlay.type.toEffect(
                    value = overlay.path,
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    externalCanvasSize = canvasSize,
                    textMeasurer = textMeasurer
                )

                is VideoModification.DrawingImage -> overlay.type.toEffect(
                    value = overlay,
                    timespan = overlay.timespan,
                    ratio = ratio,
                    context = context,
                    externalCanvasSize = canvasSize,
                    textMeasurer = textMeasurer
                )

                else -> null
            }
            effect?.let { overlayEffects.add(it) }
        }

        val overlayEffectsList = listOf(
            OverlayEffect(
                overlayEffects.toList()
            )
        )

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(audioEffectList, modList + videoEditingState.effectList + overlayEffectsList))
            .setSpeed(
                object : SpeedProvider {
                    override fun getSpeed(p0: Long) = videoEditingState.speed

                    override fun getNextSpeedChangeTimeUs(p0: Long) = C.TIME_UNSET
                }
            )
            .build()

        val tempFile = File(context.cacheDir, "/${media.displayName}")

        if (tempFile.exists()) tempFile.delete()
        if (tempFile.parentFile?.exists() != true) tempFile.parentFile?.mkdirs()
        withContext(Dispatchers.IO) {
            tempFile.createNewFile()
        }

        var completions = 0
        var error = false
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    super.onCompleted(composition, exportResult)

                    if (completions >= 1) {
                        tempFile.delete()
                    }

                    // not using newMedia.uri since we don't have access to that after deletion
                    context.contentResolver.getUriFromAbsolutePath(absolutePath = tempFile.absolutePath, type = MediaType.Video)?.let { newUri ->
                        context.contentResolver.update(
                            newUri,
                            ContentValues().apply {
                                put(MediaStore.MediaColumns.IS_PENDING, 0)
                            },
                            null
                        )
                        context.contentResolver.notifyChange(newUri, null)
                    }

                    completions += 1
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    super.onError(composition, exportResult, exportException)

                    Log.d(TAG, exportException.message.toString())
                    exportException.printStackTrace()

                    error = true
                }
            })
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder()
                            .setBitrate(
                                if (videoEditingState.bitrate == 0) basicVideoData.bitrate
                                else videoEditingState.bitrate
                            )
                            .build()
                    )
                    .build()
            )
            .apply {
                if (media.isCloud) {
                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(info.auth.headers)

                    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                    val assetLoaderFactory = ExoPlayerAssetLoader.Factory(
                        context,
                        DefaultDecoderFactory.Builder(context).build(),
                        Clock.DEFAULT,
                        mediaSourceFactory
                    )

                    setAssetLoaderFactory(assetLoaderFactory)
                }
            }
            .build()


        // TODO: SEVERELY inefficient please fix
        transformer.start(
            editedMediaItem,
            tempFile.absolutePath
        )

        while (completions == 0) {
            transformer.getProgress(progressHolder)
            percentage.floatValue = (progressHolder.progress / totalPercentage)

            body.value = context.resources.getString(R.string.editing_export_video_loading_body, 1, 3)

            delay(1000)
        }

        if (error) return@withContext null

        progressHolder.progress = 0 // reset for second operation
        delay(1000)

        modList.clear()

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

        if (!tempFile.exists() && tempFile.length() <= 0) {
            percentage.floatValue = 1f
            return@withContext null
        }

        val finalMediaItem = MediaItem.Builder()
            .setUri(tempFile.toUri())
            .build()

        val finalEditedMediaItem = EditedMediaItem.Builder(finalMediaItem)
            .setEffects(Effects(emptyList(), modList))
            .build()

        val tempFileCrop = File(context.cacheDir, "/crop-${media.displayName}")
        if (tempFileCrop.exists()) tempFileCrop.delete()
        if (tempFileCrop.parentFile?.exists() != true) tempFileCrop.parentFile?.mkdirs()
        withContext(Dispatchers.IO) {
            tempFileCrop.createNewFile()
        }

        transformer.start(
            finalEditedMediaItem,
            tempFileCrop.absolutePath
        )

        // wait again while that does its thing
        while (completions == 1) {
            transformer.getProgress(progressHolder)
            percentage.floatValue = (100f / 240f) + (progressHolder.progress / totalPercentage) // (100f / 240f) since the previous "half" was done

            if (percentage.floatValue >= 0.5f) {
                body.value = context.resources.getString(R.string.editing_export_video_loading_body, 2, 3)
            }

            delay(1000)
        }
        delay(1000)

        if (!tempFileCrop.exists() || tempFileCrop.length() <= 0) {
            percentage.floatValue = 1f
            return@withContext null
        }

        if (tempFile.exists()) tempFile.delete()

        return@withContext VideoEditResult(
            newUri = tempFileCrop.toUri(),
            tempPath = tempFileCrop.absolutePath,
            deletedUri = media.uri.toUri().takeIf { overwrite }
        )
    }

    suspend fun editImage(
        context: Context,
        image: ImageBitmap,
        uri: String,
        containerDimens: Size,
        exportQuality: Int,
        drawingPaintState: DrawingPaintState,
        imageEditingState: ImageEditingState,
        modifications: List<ImageModification>,
        textMeasurer: TextMeasurer,
        actualLeft: Float,
        actualTop: Float,
        overwrite: Boolean,
        isFromOpenWithView: Boolean
    ): Long?

    suspend fun editImageImpl(
        context: Context,
        image: ImageBitmap,
        containerDimens: Size,
        drawingPaintState: DrawingPaintState,
        imageEditingState: ImageEditingState,
        modifications: List<ImageModification>,
        textMeasurer: TextMeasurer,
        actualLeft: Float,
        actualTop: Float
    ): Bitmap? = withContext(Dispatchers.IO) {
        val images = drawingPaintState.modifications
            .fastMapNotNull { mod ->
                mod as? SharedModification.DrawingImage
            }
            .fastMap { mod ->
                context.contentResolver.openInputStream(mod.image.bitmapUri).use { inputStream ->
                    Pair(mod.image.bitmapUri, BitmapFactory.decodeStream(inputStream).asImageBitmap())
                }
            }

        val bitmap = image.asAndroidBitmap()
            .copy(Bitmap.Config.ARGB_8888, true)
            .asImageBitmap()

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

        val ratio =
            max(
                image.width.toFloat() / containerDimens.width,
                image.height.toFloat() / containerDimens.height
            )

        val translateX = -(actualLeft * ratio)
        val translateY = -(actualTop * ratio)

        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(
                width = bitmap.width.toFloat(),
                height = bitmap.height.toFloat()
            )
        ) {
            translate(left = translateX, top = translateY) {
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
                                    }
                                }
                            }
                        }
                    }
                }
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

        val cropped = Bitmap
            .createBitmap(
                bitmap.asAndroidBitmap(),
                (left * bitmap.width).roundToInt(),
                (top * bitmap.height).roundToInt(),
                (width * bitmap.width).roundToInt(),
                (height * bitmap.height).roundToInt(),
                Matrix().apply {
                    postRotate(imageEditingState.rotation)
                },
                true
            )
            .scale(
                if (imageEditingState.rotation % 180f == 0f) imageEditingState.resolution.width else imageEditingState.resolution.height,
                if (imageEditingState.rotation % 180f == 0f) imageEditingState.resolution.height else imageEditingState.resolution.width
            )

        Log.d(TAG, "Image crop left ${left * bitmap.width} and width ${width * bitmap.width}")

        return@withContext cropped
    }
}