package com.kaii.photos.compose.videoplayer

import android.app.Activity
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.VideoPlayerConstants
import com.kaii.photos.helpers.scrolling.SinglePhotoScrollState
import com.kaii.photos.helpers.video.VideoPlayerState
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun VideoPlayer(
    item: MediaStoreData,
    accessToken: String,
    state: VideoPlayerState,
    appBarsVisible: MutableState<Boolean>,
    scrollState: SinglePhotoScrollState,
    window: Window,
    shouldPlay: () -> Boolean,
    blurViews: Boolean,
    useBlackBackground: Boolean,
    useCache: Boolean,
    modifier: Modifier = Modifier,
    isOpenWithView: Boolean = false,
    isSecuredMedia: Boolean = false
) {
    val context = LocalContext.current
    var securedMediaProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(shouldPlay()) {
        if (shouldPlay()) {
            state.setSource(
                context = context,
                item = item,
                accessToken = accessToken,
                shouldPlay = shouldPlay,
                progress = {
                    securedMediaProgress = it
                }
            )
        }
    }

    if (isSecuredMedia) {
        if (securedMediaProgress < 1f) {
            Column(
                modifier = modifier
                    .fillMaxSize(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.video_decrypting),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.media_progress),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = {
                        securedMediaProgress
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    gapSize = 0.dp,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.6f),
                )
            }

            return
        }
    }

    BackHandler(
        enabled = isOpenWithView
    ) {
        state.pause()
        state.resetMute()
        state.release(context)

        (context as Activity).finish()
    }

    var showVideoPlayerControlsTimeout by remember { mutableIntStateOf(0) }
    LaunchedEffect(showVideoPlayerControlsTimeout) {
        delay(VideoPlayerConstants.CONTROLS_HIDE_TIMEOUT)
        setBarVisibility(
            visible = false,
            window = window
        ) {
            appBarsVisible.value = it
            state.controlsVisible = it
        }

        showVideoPlayerControlsTimeout = 0
    }

    LaunchedEffect(state.controlsVisible) {
        if (state.controlsVisible) showVideoPlayerControlsTimeout += 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
            .then(
                if (state.isPlaying && shouldPlay()) {
                    Modifier.keepScreenOn()
                } else Modifier.Companion
            )
    ) {
        if (!shouldPlay()) {
            GlideImage(
                model = item.uri.toUri(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                loading = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                it.signature(item.signature())
                    .diskCacheStrategy(if (useCache) DiskCacheStrategy.ALL else DiskCacheStrategy.NONE)
            }
        } else {
            val playerView = rememberPlayerView(
                useTextureView = false,
                blurViews = blurViews,
                useBlackBackground = useBlackBackground
            )

            AndroidView(
                factory = {
                    playerView
                },
                update = {
                    state.linkPlayerView(it)
                },
                modifier = modifier
                    .align(Alignment.Center)
            )
        }

        var doubleTapDisplayTimeMillis by remember { mutableIntStateOf(0) }
        val isLandscape by rememberDeviceOrientation()
        val seekBackBackgroundColor by animateColorAsState(
            targetValue = if (doubleTapDisplayTimeMillis < 0) MaterialTheme.colorScheme.primary.copy(
                alpha = 0.4f
            ) else Color.Transparent,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate double tap to skip background color"
        )
        val seekForwardBackgroundColor by animateColorAsState(
            targetValue = if (doubleTapDisplayTimeMillis > 0) MaterialTheme.colorScheme.primary.copy(
                alpha = 0.4f
            ) else Color.Transparent,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate double tap to skip background color"
        )

        LaunchedEffect(doubleTapDisplayTimeMillis) {
            delay(1000)
            doubleTapDisplayTimeMillis = 0
        }
        LaunchedEffect(isLandscape) {
            setBarVisibility(
                visible = !isLandscape,
                window = window
            ) {
                appBarsVisible.value = it
                if (!isLandscape) state.controlsVisible = it
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            if (!scrollState.videoLock && doubleTapDisplayTimeMillis == 0) {
                                setBarVisibility(
                                    visible = if (isLandscape) false else !state.controlsVisible,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }

                                state.controlsVisible = !state.controlsVisible
                            } else if (!scrollState.videoLock && doubleTapDisplayTimeMillis != 0) {
                                if (position.x < size.width / 2) {
                                    if (doubleTapDisplayTimeMillis > 0) doubleTapDisplayTimeMillis =
                                        0
                                    doubleTapDisplayTimeMillis -= 1000

                                    state.seekBack()
                                } else if (position.x >= size.width / 2) {
                                    if (doubleTapDisplayTimeMillis < 0) doubleTapDisplayTimeMillis =
                                        0
                                    doubleTapDisplayTimeMillis += 1000

                                    state.seekForward()
                                }

                                showVideoPlayerControlsTimeout += 1
                            }
                        },

                        onDoubleTap = { position ->
                            if (!scrollState.videoLock && position.x < size.width / 2) {
                                if (doubleTapDisplayTimeMillis > 0) doubleTapDisplayTimeMillis = 0
                                doubleTapDisplayTimeMillis -= 1000

                                state.seekBack()
                            } else if (!scrollState.videoLock && position.x >= size.width / 2) {
                                if (doubleTapDisplayTimeMillis < 0) doubleTapDisplayTimeMillis = 0
                                doubleTapDisplayTimeMillis += 1000

                                state.seekForward()
                            }

                            showVideoPlayerControlsTimeout += 1
                        }
                    )
                }
                .then(modifier)
        )

        // seperate boxes to avoid touch blocking due to zindex ordering
        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .zIndex(2f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(0, 100, 100, 0))
                    .background(seekBackBackgroundColor)
            ) {
                AnimatedVisibility(
                    visible = doubleTapDisplayTimeMillis < 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    exit =
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_rewind),
                        contentDescription = stringResource(id = R.string.video_seek_icon_desc),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(100, 0, 0, 100))
                    .background(seekForwardBackgroundColor)
            ) {
                AnimatedVisibility(
                    visible = doubleTapDisplayTimeMillis > 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    exit =
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        ) + scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_forward),
                        contentDescription = stringResource(id = R.string.video_seek_icon_desc),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(
                animationSpec = AnimationConstants.expressiveTween()
            ),
            exit = fadeOut(
                animationSpec = AnimationConstants.expressiveTween()
            ),
            modifier = Modifier
                .fillMaxSize(1f)
                .align(Alignment.Center)
        ) {
            VideoPlayerControls(
                isPlaying = {
                    state.isPlaying
                },
                isMuted = {
                    state.isMuted
                },
                currentVideoPosition = {
                    state.currentPosition
                },
                duration = {
                    state.duration
                },
                title = {
                    state.videoTitle
                },
                playbackSpeed = {
                    state.playbackSpeed
                },
                modifier = Modifier
                    .fillMaxSize(1f),
                onAnyTap = {
                    showVideoPlayerControlsTimeout += 1
                },
                togglePlayPause = {
                    if (state.isPlaying) state.pause()
                    else state.play()
                },
                seekBack = state::seekBack,
                seekForward = state::seekForward,
                seekTo = state::seekTo,
                toggleMute = state::toggleMute,
                setPlaybackSpeed = state::setPlaybackSpeed
            )
        }

        if ((scrollState.videoLock || state.controlsVisible) && isLandscape) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .animateContentSize()
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                FilledTonalIconToggleButton(
                    checked = scrollState.videoLock,
                    onCheckedChange = {
                        scrollState.setVideoLock(it)
                        showVideoPlayerControlsTimeout += 1
                    },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors().copy(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Top)
                ) {
                    Icon(
                        painter = painterResource(id = if (scrollState.videoLock) R.drawable.secure_folder else R.drawable.unlock),
                        contentDescription = stringResource(id = R.string.video_lock_screen),
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

                if (state.controlsVisible) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 4.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                setBarVisibility(
                                    visible = !appBarsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }

                                showVideoPlayerControlsTimeout += 1
                                scrollState.setVideoLock(false)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.more_options),
                                contentDescription = stringResource(id = R.string.show_options),
                                modifier = Modifier
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}