package com.kaii.photos.compose.editing_view.image_editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.widgets.ColorRangeSlider
import com.kaii.photos.compose.widgets.PopupPillSlider
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditorTabs
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaAdjustments

@Composable
fun ImageEditorAdjustmentTools(
    drawingPaintState: DrawingPaintState,
    modifications: SnapshotStateList<ImageModification>,
    currentEditorPage: Int,
    totalModCount: MutableIntState,
    modifier: Modifier = Modifier,
    addModification: (mod: ImageModification, modIndex: Int?) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val latestAdjustment by remember {
            derivedStateOf {
                modifications.lastOrNull { it is ImageModification.Adjustment } as? ImageModification.Adjustment
            }
        }

        val sliderVal = remember(currentEditorPage) {
            mutableFloatStateOf(
                if (currentEditorPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw)) drawingPaintState.strokeWidth / 100f
                else latestAdjustment?.value ?: 1f
            )
        }

        val changesSize = remember { mutableIntStateOf(0) }
        var isDragging by remember { mutableStateOf(false) }
        LaunchedEffect(latestAdjustment, totalModCount.intValue) {
            if (latestAdjustment != null && !isDragging) {
                sliderVal.floatValue = latestAdjustment!!.value
            }
        }

        AnimatedContent(
            targetState = latestAdjustment?.type == MediaAdjustments.ColorTint,
            modifier = Modifier
                .weight(1f)
        ) { targetState ->
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
            ) {
                if (targetState) {
                    ColorRangeSlider(
                        sliderValue = sliderVal,
                        enabled = latestAdjustment != null,
                        confirmValue = {
                            val new = latestAdjustment!!.copy(
                                value = sliderVal.floatValue
                            )

                            modifications.remove(latestAdjustment!!)
                            modifications.add(new)
                            totalModCount.intValue += 1
                        }
                    )
                } else {
                    val textMeasurer = rememberTextMeasurer()

                    LaunchedEffect(drawingPaintState.strokeWidth) {
                        if (!isDragging && currentEditorPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw)) sliderVal.floatValue = drawingPaintState.strokeWidth / 128f
                    }

                    PopupPillSlider(
                        sliderValue = sliderVal,
                        changesSize = changesSize, // not using totalModCount since that would cook the performance
                        popupPillHeightOffset = 6.dp,
                        enabled = latestAdjustment != null || currentEditorPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw),
                        range =
                            if (currentEditorPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw)) 0f..100f
                            else -100f..100f,
                        confirmValue = {
                            isDragging = false
                            if (currentEditorPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw)) {
                                // set brush width
                                drawingPaintState.setStrokeWidth(
                                    strokeWidth = sliderVal.floatValue * 128f,
                                    textMeasurer = textMeasurer,
                                    currentTime = 0f
                                )
                            } else {
                                // set adjustment values
                                val new = latestAdjustment!!.copy(
                                    value = sliderVal.floatValue
                                )

                                modifications.remove(latestAdjustment!!)
                                modifications.add(new)

                                addModification(new, MediaAdjustments.entries.indexOf(new.type))

                                totalModCount.intValue += 1
                            }
                        },
                        onValueChange = {
                            isDragging = true
                            // to update the preview immediately
                            if (currentEditorPage == ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw)) {
                                // set brush width
                                drawingPaintState.setStrokeWidth(
                                    strokeWidth = sliderVal.floatValue * 128f,
                                    textMeasurer = textMeasurer,
                                    currentTime = 0f
                                )
                            } else {
                                // set adjustment values
                                val new = latestAdjustment!!.copy(
                                    value = sliderVal.floatValue
                                )

                                modifications.remove(latestAdjustment!!)
                                modifications.add(new)
                                totalModCount.intValue += 1
                            }
                        }
                    )
                }
            }
        }
    }
}