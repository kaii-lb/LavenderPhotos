package com.kaii.photos.compose.single_photo.editing_view

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
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

private const val TAG = "EDITOR_MISC"

enum class SliderStates {
    FontScaling,
    Zooming,
    SelectedTextScaling
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
    None
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

    if (cropArea != null) {
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

    val mediaItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(clippingConfiguration)
        .build()

    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
        .setEffects(Effects(emptyList(), effectList))
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