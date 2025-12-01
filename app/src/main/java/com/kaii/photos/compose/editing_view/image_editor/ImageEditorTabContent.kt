package com.kaii.photos.compose.editing_view.image_editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.SliderDialog
import com.kaii.photos.compose.editing_view.EditingViewBottomAppBarItem
import com.kaii.photos.compose.editing_view.SharedEditorCropContent
import com.kaii.photos.helpers.editing.CroppingAspectRatio
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaAdjustments
import kotlin.math.floor

@Composable
fun ImageEditorAdjustContent(
    modifier: Modifier = Modifier,
    modifications: SnapshotStateList<ImageModification>,
    increaseModCount: () -> Unit
) {
    LazyRow(
        modifier = modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(
            count = MediaAdjustments.entries.size
        ) { index ->
            val entry = MediaAdjustments.entries[index]

            ImageEditingAdjustmentItem(
                type = entry,
                modifications = modifications,
                extraOnClick = increaseModCount
            )
        }
    }
}

@Composable
private fun ImageEditingAdjustmentItem(
    type: MediaAdjustments,
    modifications: SnapshotStateList<ImageModification>,
    extraOnClick: () -> Unit
) {
    EditingViewBottomAppBarItem(
        text = stringResource(id = type.title),
        icon = type.icon,
        selected = (modifications.lastOrNull {
            it is ImageModification.Adjustment
        } as? ImageModification.Adjustment)?.type == type,
        onClick = {
            val last = modifications.lastOrNull {
                it is ImageModification.Adjustment && it.type == type
            } as? ImageModification.Adjustment

            if (last != null) {
                val latest = modifications.lastOrNull { it is ImageModification.Adjustment } as? ImageModification.Adjustment

                if (latest?.type == type) { // double click for reset value
                    modifications.remove(last)
                    modifications.add(last.copy(value = type.startValue))
                } else {
                    modifications.remove(last)
                    modifications.add(last)
                }
            } else {
                modifications.add(
                    ImageModification.Adjustment(
                        type = type,
                        value = type.startValue
                    )
                )
            }

            extraOnClick()
        }
    )
}

@Composable
fun ImageEditorCropContent(
    imageAspectRatio: Float,
    croppingAspectRatio: CroppingAspectRatio,
    rotation: Float,
    resolution: IntSize,
    initialWidth: Int,
    setCroppingAspectRatio: (CroppingAspectRatio) -> Unit,
    setRotation: (Float) -> Unit,
    resetCrop: () -> Unit,
    getScaledResolution: (Float) -> IntSize,
    scaleResolution: (Float) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val dialogTitle = stringResource(id = R.string.exif_res)

    fun lookUpResScale(value: Float) =
        when (floor(value).toInt()) {
            1 -> 1f / 8f
            2 -> 1f / 4f
            3 -> 1f / 2f
            4 -> 3f / 4f

            else -> 1f
        }

    fun invertedLookUpResScale(value: Float) =
        when (value) {
            1f / 8f -> 1f
            1f / 4f -> 2f
            1f / 2f -> 3f
            3f / 4f -> 4f

            else -> 5f
        }

    if (showDialog) {
        SliderDialog(
            title = {
                val res = getScaledResolution(lookUpResScale(it))
                "$dialogTitle ${res.width}x${res.height}"
            },
            steps = 3,
            range = 1f..5f,
            startsAt = invertedLookUpResScale(resolution.width.toFloat() / initialWidth),
            onSetValue = {
                scaleResolution(lookUpResScale(it))
            },
            onDismiss = {
                showDialog = false
            }
        )
    }

    SharedEditorCropContent(
        imageAspectRatio = imageAspectRatio,
        croppingAspectRatio = croppingAspectRatio,
        rotation = rotation,
        setCroppingAspectRatio = setCroppingAspectRatio,
        setRotation = setRotation,
        resetCrop = resetCrop
    ) {
        EditingViewBottomAppBarItem(
            text = stringResource(id = R.string.editing_resolution_scale),
            icon = R.drawable.fit_page_width,
            onClick = {
                showDialog = true
            }
        )
    }
}