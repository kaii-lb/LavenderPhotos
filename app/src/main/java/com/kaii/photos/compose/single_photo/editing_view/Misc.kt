package com.kaii.photos.compose.single_photo.editing_view

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.HslAdjustment
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
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import java.io.File
import kotlin.math.pow

private const val TAG = "EDITOR_MISC"

enum class SliderStates {
    FontScaling,
    Zooming,
    SelectedTextScaling
}

data class BasicVideoData(
    val duration: Float,
    val frameRate: Float,
    val absolutePath: String,
    val width: Int,
    val height: Int
) {
    val aspectRatio = width.toFloat() / height
}

interface VideoModification {
    data class Trim(
        val start: Float,
        val end: Float
    ) : VideoModification

    data class Crop(
        val top: Float,
        val left: Float,
        val width: Float,
        val height: Float
    ) : VideoModification {
        val right = left + width
        val bottom = top + height
    }

    data class Rotation(
        val degrees: Float
    ) : VideoModification

    data class Volume(
        val percentage: Float
    ) : VideoModification

    data class Speed(
        val multiplier: Float
    ) : VideoModification

    data class FrameDrop(
        val targetFps: Int
    ) : VideoModification

    data class Adjustment(
        val type: MediaAdjustments,
        val matrix: FloatArray,
        val value: Float
    ) : VideoModification {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Adjustment

            if (type != other.type) return false
            if (!matrix.contentEquals(other.matrix)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + matrix.contentHashCode()
            return result
        }
    }
}

enum class SelectedCropArea {
    TopLeftCorner,
    TopRightCorner,
    BottomLeftCorner,
    BottomRightCorner,
    TopEdge,
    LeftEdge,
    BottomEdge,
    RightEdge,
    None,
    Whole
}

@OptIn(UnstableApi::class)
suspend fun saveVideo(
    context: Context,
    modifications: SnapshotStateList<VideoModification>,
    uri: Uri,
    absolutePath: String,
    overwrite: Boolean,
    containerDimens: Size,
    videoDimens: IntSize,
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

    val effectList = mutableListOf<Effect>()

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

        effectList.add(
            Crop(
                normalizedLeft,
                normalizedRight,
                normalizedBottom,
                normalizedTop
            )
        )

        val widthPercent = right - left
        val heightPercent = bottom - top

        val newWidth = (videoDimens.width * widthPercent).toInt()
        val newHeight = (videoDimens.height * heightPercent).toInt()

        Log.d(TAG, "New width $newWidth and height $newHeight")

        effectList.add(
            Presentation.createForShortSide(if (newWidth > newHeight) newWidth else newHeight)
        )
    }

    val rotation = modifications.lastOrNull {
        it is VideoModification.Rotation
    } as? VideoModification.Rotation

    if (rotation != null) {
        effectList.add(
            ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(-rotation.degrees) // negative since our rotation is clockwise
                .build()
        )
    }

    val frameDrop = modifications.lastOrNull {
        it is VideoModification.FrameDrop
    } as? VideoModification.FrameDrop

    if (frameDrop != null) {
        effectList.add(
            FrameDropEffect.createDefaultFrameDropEffect(frameDrop.targetFps.toFloat())
        )
    }

    modifications.mapNotNull { it as? VideoModification.Adjustment }.forEach { adjustment ->
        // TODO: find way to sync this and one in VideoEditor.kt
        effectList.add(
            when (adjustment.type) {
                MediaAdjustments.Contrast -> Contrast(adjustment.value)
                MediaAdjustments.Saturation -> HslAdjustment.Builder().adjustSaturation(adjustment.value * 100).build()

                else -> Contrast(0f) // placeholder
            }
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

    val volumePercentage =
        modifications.lastOrNull {
            it is VideoModification.Volume
        } as? VideoModification.Volume ?: VideoModification.Volume(1f)

    val dbChange = minDb + (maxDb - minDb) * volumePercentage.percentage
    val linearGain = 10f.pow(dbChange / 20f)

    audioProcessor.putChannelMixingMatrix(
        ChannelMixingMatrix.createForConstantGain(2, 2).scaleBy(linearGain)
    )

    audioEffectList.add(audioProcessor)

    val speedMultiplier =
        modifications.lastOrNull {
            it is VideoModification.Speed
        } as? VideoModification.Speed

    if (speedMultiplier?.multiplier != null && speedMultiplier.multiplier != 1f) {
        effectList.add(SpeedChangeEffect(speedMultiplier.multiplier))
        audioEffectList.add(SonicAudioProcessor().apply {
            setSpeed(speedMultiplier.multiplier)
        })
    }

    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
        .setEffects(Effects(audioEffectList, effectList))
        .build()

    val file = File(absolutePath)

    val dupeAdd =
        if (!overwrite) {
            val new = absolutePath.getParentFromPath() + "/" + file.nameWithoutExtension + "_edited.mp4"
            if (File(new).exists()) " (1)" else ""
        } else ""

    val neededPath =
        if (overwrite) absolutePath.replaceAfterLast(".", "mp4")
        else absolutePath.getParentFromPath() + "/" + file.nameWithoutExtension + "_edited" + dupeAdd + ".mp4"


    val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                super.onCompleted(composition, exportResult)

                isLoading.value = false

                context.contentResolver.getUriFromAbsolutePath(absolutePath = neededPath, type = MediaType.Video)?.let { newUri ->
                    context.contentResolver.update(
                        newUri,
                        ContentValues().apply {
                            put(MediaColumns.IS_PENDING, 0)
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

fun DrawScope.createCropRectBorderArc(
    left: Float,
    top: Float
) = Path().apply {
    val radius = 16.dp.toPx()
    moveTo(x = left - radius / 2, y = top - radius / 2)

    lineTo(x = left, y = top - radius / 2)

    arcTo(
        rect = Rect(
            left = left - radius / 2,
            top = top - radius / 2,
            right = left + radius / 2,
            bottom = top + radius / 2
        ),
        startAngleDegrees = 270f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false
    )

    lineTo(x = left + radius / 2, y = top + radius / 2)
}

enum class CroppingAspectRatio(
    var ratio: Float,
    @param:StringRes val title: Int
) {
    FreeForm(0f, R.string.bottom_sheets_freeform),
    ByImage(-1f, R.string.bottom_sheets_image_ratio),
    Square(1f, R.string.bottom_sheets_square),
    SixteenByNine(16f / 9f, R.string.bottom_sheets_sixteen_by_nine),
    NineBySixteen(9f / 16f, R.string.bottom_sheets_nine_by_sixteen),
    NineByTwentyOne(9f / 21f, R.string.bottom_sheets_nine_by_twentyone),
    FiveByFour(5f / 4f, R.string.bottom_sheets_five_by_four),
    FourByThree(4f / 3f, R.string.bottom_sheets_four_by_three),
    ThreeByTwo(3f / 2f, R.string.bottom_sheets_three_by_two)
}

enum class MediaAdjustments {
    Contrast,
    Brightness,
    Saturation,
    BlackPoint,
    WhitePoint,
    Warmth,
    ColorTint,
    Highlights
}

enum class VideoEditorTabs(
    @param:StringRes val title: Int
) {
    Trim(R.string.editing_trim),
    Crop(R.string.editing_crop),
    Video(R.string.video),
    Draw(R.string.editing_draw),
    Adjust(R.string.editing_adjust),
    Filters(R.string.editing_filters)
}