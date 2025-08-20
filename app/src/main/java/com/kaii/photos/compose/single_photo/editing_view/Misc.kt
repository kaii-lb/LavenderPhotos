package com.kaii.photos.compose.single_photo.editing_view

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.helpers.getFileNameFromPath

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
    showErrorSnackbar: () -> Unit
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

    val mediaItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(clippingConfiguration)
        .build()

    val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                super.onCompleted(composition, exportResult)

                isLoading.value = false
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                super.onError(composition, exportResult, exportException)

                showErrorSnackbar()
            }
        })
        .build()

    transformer.start(
        mediaItem,
        absolutePath.replace(
            absolutePath.getFileNameFromPath(),
            absolutePath.getFileNameFromPath().substringBefore(".") +
                    if (!overwrite) "_edited." else "." +
                    absolutePath.getFileNameFromPath().substringAfter(".")
        )
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