package com.kaii.photos.compose.single_photo.editing_view

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.VideoEditorBottomBar
import com.kaii.photos.compose.app_bars.VideoEditorTopBar
import com.kaii.photos.compose.single_photo.VideoPlayerSeekbar
import com.kaii.photos.compose.single_photo.rememberExoPlayerWithLifeCycle
import com.kaii.photos.compose.single_photo.rememberPlayerView
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import kotlinx.coroutines.delay

private const val TAG = "VIDEO_EDITOR"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditor(
    uri: Uri,
    absolutePath: String,
    window: Window
) {
    val isPlaying = remember { mutableStateOf(false) }
    val isMuted = remember { mutableStateOf(false) }

    /** In Seconds */
    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
    val duration = remember { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource = uri,
        absolutePath = absolutePath,
        isPlaying = isPlaying,
        duration = duration,
        currentVideoPosition = currentVideoPosition
    )

    val leftTrimPosition = remember { mutableFloatStateOf(0f) }
    val rightTrimPosition = remember { mutableFloatStateOf(duration.floatValue) }

    LaunchedEffect(duration.floatValue) {
        rightTrimPosition.floatValue = duration.floatValue
    }

    LaunchedEffect(isMuted.value) {
        if (isMuted.value) exoPlayer.volume = 0f
        else exoPlayer.volume = 1f
    }

    LaunchedEffect(leftTrimPosition.floatValue, currentVideoPosition.floatValue) {
        if (currentVideoPosition.floatValue * 1000 < leftTrimPosition.floatValue * 1000) {
            exoPlayer.seekTo((leftTrimPosition.floatValue * 1000).toLong())
            currentVideoPosition.floatValue = leftTrimPosition.floatValue

            if (isPlaying.value) exoPlayer.play()
            else exoPlayer.pause()
        }
    }

    LaunchedEffect(isPlaying.value) {
        if (!isPlaying.value) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            exoPlayer.pause()
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.play()
        }

        currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f
        val end = rightTrimPosition.floatValue * 1000
        val threshold = 150f
        val current = currentVideoPosition.floatValue * 1000
        if (current in (end - threshold)..(end + threshold) || current >= end) {
            delay(1000)
            exoPlayer.pause()
            exoPlayer.seekTo((leftTrimPosition.floatValue * 1000).toLong())
            currentVideoPosition.floatValue = leftTrimPosition.floatValue
            Log.d(TAG, "Ending video...")
            isPlaying.value = false
        }

        while (isPlaying.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            // again here since exoplayer doesn't know we might be ending video early
            // because of the way it works (this is easier)
            val end = rightTrimPosition.floatValue * 1000
            val threshold = 150f
            val current = currentVideoPosition.floatValue * 1000
            if (current in (end - threshold)..(end + threshold) || current >= end) {
                delay(1000)
                exoPlayer.pause()
                exoPlayer.seekTo((leftTrimPosition.floatValue * 1000).toLong())
                currentVideoPosition.floatValue = leftTrimPosition.floatValue
                Log.d(TAG, "Ending video...")
                isPlaying.value = false
            }

            delay(100)
        }
    }

    val modifications = remember { mutableStateListOf<VideoModification>() }
    val lastSavedModCount =
        remember { mutableIntStateOf(2) } // starts at 2 cuz of below LaunchedEffect, and then 1 more for setting rightPosition to duration

    LaunchedEffect(leftTrimPosition.floatValue, rightTrimPosition.floatValue) {
        modifications.add(
            VideoModification.Trim(
                start = leftTrimPosition.floatValue,
                end = rightTrimPosition.floatValue
            )
        )
    }

    val pagerState = rememberPagerState { 4 }
    var containerDimens by remember { mutableStateOf(Size.Zero) }
    var videoDimens by remember { mutableStateOf(exoPlayer.videoSize) }
    val resetCrop = remember { mutableStateOf(false) }

    var rotation by remember { mutableFloatStateOf(0f) }
    val aspectRatio = remember { mutableStateOf(CroppingAspectRatio.FreeForm) }

    Scaffold(
        topBar = {
            VideoEditorTopBar(
                uri = uri,
                absolutePath = absolutePath,
                modifications = modifications,
                lastSavedModCount = lastSavedModCount,
                containerDimens = containerDimens,
                videoDimens = IntSize(
                    width = videoDimens.width,
                    height = videoDimens.height
                )
            )
        },
        bottomBar = {
            VideoEditorBottomBar(
                pagerState = pagerState,
                currentPosition = currentVideoPosition,
                duration = duration,
                absolutePath = absolutePath,
                leftPosition = leftTrimPosition,
                rightPosition = rightTrimPosition,
                imageAspectRatio = videoDimens.width.toFloat() / videoDimens.height,
                croppingAspectRatio = aspectRatio,
                onCropReset = {
                    resetCrop.value = true
                    rotation = 0f
                    aspectRatio.value = CroppingAspectRatio.FreeForm

                    modifications.add(
                        VideoModification.Rotation(
                            degrees = 0f
                        )
                    )
                },
                onSeek = { pos ->
                    val wasPlaying = isPlaying.value
                    exoPlayer.seekTo(
                        (pos * 1000f).coerceAtMost(rightTrimPosition.floatValue * 1000f).toLong()
                    )
                    isPlaying.value = wasPlaying
                },
                onRotate = {
                    rotation += 90f
                    modifications.add(
                        VideoModification.Rotation(
                            degrees = rotation
                        )
                    )
                },
                onSetVolume = { percent ->
                    modifications.add(
                        VideoModification.Volume(percent)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val animatedRotation by animateFloatAsState(
                targetValue = rotation
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(1f)
                    .padding(8.dp)
            ) {
                val context = LocalContext.current
                val playerView = rememberPlayerView(
                    exoPlayer = exoPlayer,
                    activity = context as Activity,
                    absolutePath = absolutePath,
                    useTextureView = true
                )

                val width by animateDpAsState(
                    targetValue = if (rotation % 180 == 0f) this@BoxWithConstraints.maxWidth else this@BoxWithConstraints.maxHeight
                )
                val height by animateDpAsState(
                    targetValue = if (rotation % 180f == 0f) this@BoxWithConstraints.maxHeight else this@BoxWithConstraints.maxWidth
                )

                Box(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .padding(8.dp)
                        .rotate(animatedRotation)
                        .align(Alignment.Center)
                ) {
                    AndroidView(
                        factory = {
                            playerView
                        },
                        modifier = Modifier
                            .fillMaxSize(1f)
                            .align(Alignment.Center)
                    )
                }

                Column(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .rotate(animatedRotation)
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = pagerState.currentPage == 1,
                        enter = fadeIn(
                            animationSpec = AnimationConstants.expressiveTween()
                        ),
                        exit = fadeOut(
                            animationSpec = AnimationConstants.expressiveTween()
                        ),
                        modifier = Modifier
                            .requiredSize(
                                width = width + 16.dp, // as to not clip the CropBox arcs
                                height = height + 16.dp
                            )
                    ) {
                        val localDensity = LocalDensity.current

                        var tries by remember { mutableIntStateOf(0) }

                        LaunchedEffect(videoDimens) {
                            if (videoDimens.width == 0 || videoDimens.height == 0 && tries < 10) {
                                videoDimens = exoPlayer.videoSize
                                tries += 1

                                println("VIDEO DIMENS ${videoDimens.width} and ${videoDimens.height}")
                                delay(100)
                            }
                        }

                        CropBox(
                            containerWidth = with(localDensity) { width.toPx() - 16.dp.toPx() }, // adjust for AnimatedVisibility size
                            containerHeight = with(localDensity) { height.toPx() - 16.dp.toPx() },
                            mediaAspectRatio = videoDimens.width.toFloat() / videoDimens.height,
                            reset = resetCrop,
                            aspectRatio = aspectRatio,
                            modifier = Modifier
                                .offset(16.dp, 16.dp) // adjust for top-left change from AnimatedVisibility
                        ) { area, original ->
                            modifications.add(
                                VideoModification.Crop(
                                    top = area.top,
                                    left = area.left,
                                    width = area.width,
                                    height = area.height
                                )
                            )

                            containerDimens = original
                        }
                    }
                }
            }

            VideoEditorBottomTools(
                pagerState = pagerState,
                isPlaying = isPlaying,
                isMuted = isMuted,
                currentPosition = currentVideoPosition,
                duration = duration.floatValue,
                setPlaybackSpeed = { speed ->
                    exoPlayer.setPlaybackSpeed(speed)
                },
                onSeek = { pos ->
                    val wasPlaying = isPlaying.value
                    exoPlayer.seekTo(
                        (pos * 1000f).coerceAtMost(duration.floatValue * 1000f).toLong()
                    )
                    isPlaying.value = wasPlaying
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VideoEditorBottomTools(
    pagerState: PagerState,
    currentPosition: MutableFloatState,
    duration: Float,
    isPlaying: MutableState<Boolean>,
    isMuted: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    setPlaybackSpeed: (speed: Float) -> Unit,
    onSeek: (position: Float) -> Unit
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
                    modifier = Modifier
                        .width(animatedSeekbarWidth)
                        .align(Alignment.CenterStart)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
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

            Spacer(modifier = Modifier.width(4.dp))

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
        }
    }
}

