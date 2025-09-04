package com.kaii.photos.compose.single_photo.editing_view.video_editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.single_photo.VideoPlayerSeekbar
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorTabs
import com.kaii.photos.compose.widgets.ColorRangeSlider
import com.kaii.photos.compose.widgets.PopupPillSlider
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.editing.MediaAdjustments
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
    modifier: Modifier = Modifier,
    setPlaybackSpeed: (speed: Float) -> Unit,
    onSeek: (position: Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    val toolsPagerState = rememberPagerState {
        if (pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust)) 2
        else 1
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
                setPlaybackSpeed = setPlaybackSpeed,
                onSeek = onSeek,
                onSeekFinished = onSeekFinished,
                modifier = Modifier
            )
        } else {
            VideoEditorAdjustmentTools(
                modifications = modifications,
                state = toolsPagerState,
                totalModCount = totalModCount
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
    setPlaybackSpeed: (speed: Float) -> Unit,
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
                targetValue = if (pagerState.currentPage != 0) this.maxWidth else 0.dp
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

            var currentPlaybackSpeed by remember { mutableFloatStateOf(1f) }
            FilledTonalButton(
                onClick = {
                    val new =
                        when (currentPlaybackSpeed) {
                            1f -> 1.5f
                            1.5f -> 2f
                            2f -> 4f
                            4f -> 0.5f
                            else -> 1f
                        }

                    setPlaybackSpeed(new)
                    currentPlaybackSpeed = new
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .width(40.dp)
            ) {
                Text(
                    text = "${currentPlaybackSpeed}X",
                    fontSize = TextUnit(TextStylingConstants.EXTRA_SMALL_TEXT_SIZE, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            }

            val coroutineScope = rememberCoroutineScope()
            val animatedWidth by animateDpAsState(
                targetValue =
                    if (pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust)) 32.dp
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val sliderVal = remember { mutableFloatStateOf(1f) }
        val changesSize = remember { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        val latestAdjustment by remember {
            derivedStateOf {
                modifications.lastOrNull { it is VideoModification.Adjustment } as? VideoModification.Adjustment
            }
        }

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
                    PopupPillSlider(
                        sliderValue = sliderVal,
                        changesSize = changesSize, // not using totalModCount since that would cook the performance
                        popupPillHeightOffset = 6.dp,
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
                }
            }
        }
    }
}