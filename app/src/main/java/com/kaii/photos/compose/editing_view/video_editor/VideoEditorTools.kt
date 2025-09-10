package com.kaii.photos.compose.editing_view.video_editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.single_photo.VideoPlayerSeekbar
import com.kaii.photos.compose.widgets.ColorRangeSlider
import com.kaii.photos.compose.widgets.PopupPillSlider
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.editing.DrawingItems
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.MediaAdjustments
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.VideoModification
import kotlinx.coroutines.launch

@Composable
fun VideoEditorBottomTools(
    pagerState: PagerState,
    modifications: SnapshotStateList<VideoModification>,
    currentPosition: MutableFloatState,
    duration: Float,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    totalModCount: MutableIntState,
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    modifier: Modifier = Modifier,
    onSeek: (position: Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    val toolsPagerState = rememberPagerState { 2 }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust)
            || pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)
        ) {
            toolsPagerState.animateScrollToPage(1)
        } else {
            toolsPagerState.animateScrollToPage(0)
        }
    }

    HorizontalPager(
        state = toolsPagerState,
        modifier = modifier
            .height(40.dp)
    ) { state ->
        if (state == 0) {
            VideoEditorPlaybackControls(
                pagerState = pagerState,
                toolsPagerState = toolsPagerState,
                currentPosition = currentPosition,
                duration = duration,
                isPlaying = isPlaying,
                isMuted = isMuted,
                videoEditingState = videoEditingState,
                onSeek = onSeek,
                onSeekFinished = onSeekFinished,
                modifier = Modifier
            )
        } else {
            VideoEditorAdjustmentTools(
                state = toolsPagerState,
                totalModCount = totalModCount,
                modifications = modifications,
                drawingPaintState = drawingPaintState,
                currentEditorPage = pagerState.currentPage,
                currentVideoPosition = currentPosition.floatValue
            )
        }
    }
}

@Composable
fun VideoEditorPlaybackControls(
    pagerState: PagerState,
    toolsPagerState: PagerState,
    currentPosition: MutableFloatState,
    duration: Float,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    videoEditingState: VideoEditingState,
    onSeek: (position: Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        FilledTonalIconButton(
            onClick = {
                isPlaying.value = !isPlaying.value
            },
            modifier = Modifier
                .size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isPlaying.value) R.drawable.pause else R.drawable.play_arrow),
                contentDescription = stringResource(id = R.string.video_play_toggle)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            val animatedSeekbarWidth by animateDpAsState(
                targetValue = if (pagerState.currentPage != VideoEditorTabs.entries.indexOf(VideoEditorTabs.Trim)) this.maxWidth else 0.dp
            )

            if (animatedSeekbarWidth != 0.dp) {
                VideoPlayerSeekbar(
                    currentPosition = currentPosition.floatValue,
                    duration = duration,
                    onValueChange = onSeek,
                    onValueChangeFinished = onSeekFinished,
                    modifier = Modifier
                        .width(animatedSeekbarWidth)
                        .align(Alignment.CenterStart)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                alignment = Alignment.CenterHorizontally,
                space = 4.dp
            )
        ) {
            FilledTonalIconButton(
                onClick = {
                    isMuted.value = !isMuted.value
                },
                modifier = Modifier
                    .size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (isMuted.value) R.drawable.volume_mute else R.drawable.volume_max),
                    contentDescription = stringResource(id = R.string.video_mute_toggle)
                )
            }

            FilledTonalButton(
                onClick = {
                    val new =
                        when (videoEditingState.speed) {
                            1f -> 1.5f
                            1.5f -> 2f
                            2f -> 4f
                            4f -> 0.5f
                            else -> 1f
                        }

                    videoEditingState.setSpeed(new)
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .width(40.dp)
            ) {
                Text(
                    text = "${videoEditingState.speed}X",
                    fontSize = TextUnit(TextStylingConstants.EXTRA_SMALL_TEXT_SIZE, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            }

            val coroutineScope = rememberCoroutineScope()
            val animatedWidth by animateDpAsState(
                targetValue =
                    if (pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust)
                        || pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)
                    ) 32.dp
                    else 0.dp,
                animationSpec = AnimationConstants.expressiveSpring()
            )

            FilledTonalIconButton(
                onClick = {
                    coroutineScope.launch {
                        toolsPagerState.animateScrollToPage(1)
                    }
                },
                modifier = Modifier
                    .height(32.dp)
                    .width(animatedWidth)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.other_page_indicator),
                    contentDescription = "Navigate to video adjustment controls",
                    modifier = Modifier
                        .padding(start = 2.dp) // TODO: fix the actual "other_page_indicator" being skewed
                )
            }
        }
    }
}

@Composable
fun VideoEditorAdjustmentTools(
    state: PagerState,
    totalModCount: MutableIntState,
    modifications: SnapshotStateList<VideoModification>,
    currentEditorPage: Int,
    currentVideoPosition: Float,
    drawingPaintState: DrawingPaintState,
    modifier: Modifier = Modifier
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
                modifications.lastOrNull { it is VideoModification.Adjustment } as? VideoModification.Adjustment
            }
        }

        val sliderVal = remember(currentEditorPage) {
            mutableFloatStateOf(
                if (currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)) drawingPaintState.strokeWidth / 100f
                else latestAdjustment?.value ?: 1f
            )
        }

        val changesSize = remember { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(latestAdjustment, totalModCount.intValue) {
            if (latestAdjustment != null) {
                sliderVal.floatValue = latestAdjustment!!.value
            }
        }

        FilledTonalIconButton(
            onClick = {
                coroutineScope.launch {
                    state.animateScrollToPage(0)
                }
            },
            modifier = Modifier
                .size(32.dp)
                .rotate(180f)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.other_page_indicator),
                contentDescription = "Navigate back to video playback controls",
                modifier = Modifier
                    .padding(start = 2.dp) // TODO: fix the actual "other_page_indicator" being skewed
            )
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

                    PopupPillSlider(
                        sliderValue = sliderVal,
                        changesSize = changesSize, // not using totalModCount since that would cook the performance
                        popupPillHeightOffset = 6.dp,
                        enabled = latestAdjustment != null || currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw),
                        range =
                            if (currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)) 0f..100f
                            else -100f..100f,
                        confirmValue = {
                            if (currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)) {
                                // set brush width
                                drawingPaintState.setStrokeWidth(
                                    strokeWidth = sliderVal.floatValue * 128f,
                                    textMeasurer = textMeasurer,
                                    currentTime = currentVideoPosition
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
                        },
                        onValueChange = {
                            // to update the preview immediately
                            if (currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)) {
                                // set brush width
                                drawingPaintState.setStrokeWidth(
                                    strokeWidth = sliderVal.floatValue * 128f,
                                    textMeasurer = textMeasurer,
                                    currentTime = currentVideoPosition
                                )
                            }
                        }
                    )
                }
            }
        }

        val animatedDeleteWidth by animateDpAsState(
            targetValue =
                // no check for paintType since selectedText == null implies
                // paintType != Text or no text to delete
                if (currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw)
                    && drawingPaintState.selectedText != null
                ) 32.dp
                else 0.dp,
            animationSpec = AnimationConstants.expressiveSpring()
        )

        FilledTonalIconButton(
            onClick = {
                drawingPaintState.modifications.remove(drawingPaintState.selectedText!!)
                drawingPaintState.setSelectedText(null)
            },
            modifier = Modifier
                .height(32.dp)
                .width(animatedDeleteWidth)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ink_eraser),
                contentDescription = "delete this text"
            )
        }

        if (animatedDeleteWidth >= 5.dp) Spacer(modifier = Modifier.width(8.dp))

        val animatedKeyframeWidth by animateDpAsState(
            targetValue =
                if (currentEditorPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw) && drawingPaintState.paintType == DrawingItems.Text) 32.dp
                else 0.dp,
            animationSpec = AnimationConstants.expressiveSpring()
        )

        FilledTonalIconToggleButton(
            checked = drawingPaintState.recordKeyframes,
            onCheckedChange = {
                drawingPaintState.setRecordKeyframes(
                    record = it,
                    currentTime = currentVideoPosition
                )
            },
            modifier = Modifier
                .height(32.dp)
                .width(animatedKeyframeWidth)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.animation),
                contentDescription = "toggle ability to record keyframes"
            )
        }
    }
}