package com.kaii.photos.compose.single_photo.editing_view

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.SelectableDropDownMenuItem
import com.kaii.photos.compose.SimpleTab
import com.kaii.photos.compose.single_photo.rememberExoPlayerWithLifeCycle
import com.kaii.photos.compose.single_photo.rememberPlayerView
import com.kaii.photos.helpers.getFileNameFromPath
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditor(
    uri: Uri,
    absolutePath: String
) {
    val isPlaying = remember { mutableStateOf(false) }
    // val isMuted = remember { mutableStateOf(false) }

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

    // var controlsVisible by remember { mutableStateOf(true) }
    // var taps by remember { mutableIntStateOf(0) }
    // var lastTaps by remember { mutableIntStateOf(0) }
    //
    // LaunchedEffect(controlsVisible) {
    //     if (controlsVisible && taps == lastTaps) {
    //         lastTaps = taps
    //         delay(VideoPlayerConstants.CONTROLS_HIDE_TIMEOUT_SHORT)
    //         controlsVisible = false
    //     }
    // }

    val leftTrimPosition = remember { mutableFloatStateOf(0f) }
    val rightTrimPosition = remember { mutableFloatStateOf(duration.floatValue) }

    Scaffold(
        topBar = {
            VideoEditorTopBar(
                uri = uri,
                absolutePath = absolutePath,
                leftTrimPosition = leftTrimPosition,
                rightTrimPosition = rightTrimPosition
            )
        },
        bottomBar = {
            BottomBar(
                currentPosition = currentVideoPosition,
                duration = duration,
                absolutePath = absolutePath,
                exoPlayer = exoPlayer,
                leftTrimPosition = leftTrimPosition,
                rightTrimPosition = rightTrimPosition
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(1f)
            ) {
                val context = LocalContext.current
                val playerView = rememberPlayerView(
                    exoPlayer = exoPlayer,
                    activity = context as Activity,
                    absolutePath = absolutePath
                )

                AndroidView(
                    factory = {
                        playerView
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }

            // VideoPlayerControllerBottomControls(
            //     currentVideoPosition = currentVideoPosition,
            //     duration = duration,
            //     isPlaying = isPlaying,
            //     isMuted = isMuted,
            //     exoPlayer = exoPlayer,
            //     showPlayPauseButton = true,
            //     onAnyTap = {}
            // )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoEditorTopBar(
    uri: Uri,
    absolutePath: String,
    leftTrimPosition: MutableFloatState,
    rightTrimPosition: MutableFloatState
) {
    val navController = LocalNavController.current

    TopAppBar(
        title = {},
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(8.dp, 0.dp, 0.dp, 0.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        navController.popBackStack() // TODO: ask to save modifications
                    },
                    enabled = true,
                    modifier = Modifier
                        .height(40.dp)
                        .width(56.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(id = R.string.editing_close_desc),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        },
        actions = {
            var showDropDown by remember { mutableStateOf(false) }

            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = {
                    showDropDown = false
                },
                shape = RoundedCornerShape(24.dp),
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_overwrite_desc),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = false
                ) {
                    showDropDown = false
                }

                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_save),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = true
                ) {
                    showDropDown = false
                }
            }

            SplitButtonLayout(
                leadingButton = {
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()

                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            coroutineScope.launch {
                                val isLoading = mutableStateOf(true)

                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.LoadingEvent(
                                        message = context.resources.getString(R.string.editing_export_video_loading),
                                        icon = R.drawable.videocam,
                                        isLoading = isLoading
                                    )
                                )

                                val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs((leftTrimPosition.floatValue * 1000f).toLong())
                                    .setEndPositionMs((rightTrimPosition.floatValue * 1000f).toLong())
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

                                            coroutineScope.launch {
                                                LavenderSnackbarController.pushEvent(
                                                    LavenderSnackbarEvents.MessageEvent(
                                                        message = context.resources.getString(R.string.editing_export_video_failed),
                                                        icon = R.drawable.error_2,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                )
                                            }
                                        }
                                    })
                                    .build()

                                transformer.start(
                                    mediaItem,
                                    absolutePath.replace(absolutePath.getFileNameFromPath(), absolutePath.getFileNameFromPath().substringBefore(".") + "_edited." + absolutePath.getFileNameFromPath().substringAfter("."))
                                )
                            }
                        }
                    ) {
                        Text(
                            text = "Save"
                        )
                    }
                },
                trailingButton = {
                    // TODO: remove when material expressive is not broken like this
                    // HACKY workaround for a random trigger by onCheckedChange of TrailingButton
                    var openedTimes by remember { mutableIntStateOf(0) }

                    SplitButtonDefaults.TrailingButton(
                        checked = showDropDown,
                        onCheckedChange = {
                            openedTimes += 1
                            if (openedTimes % 2 != 0) {
                                showDropDown = !showDropDown
                            }
                        }
                    ) {
                        val rotation: Float by animateFloatAsState(
                            targetValue = if (showDropDown) 180f else 0f
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.drop_down_arrow),
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer {
                                    rotationZ = rotation
                                },
                            contentDescription = "Dropdown icon"
                        )
                    }
                }
            )
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun BottomBar(
    currentPosition: MutableFloatState,
    duration: MutableFloatState,
    absolutePath: String,
    exoPlayer: ExoPlayer,
    rightTrimPosition: MutableFloatState,
    leftTrimPosition: MutableFloatState
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 4 }

    BottomAppBar(
        modifier = Modifier
            .height(160.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(selectedTabIndex = pagerState.currentPage, matchContentSize = false)
                            .padding(4.dp)
                            .fillMaxHeight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .zIndex(1f)
                    )
                },
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth(1f)
            ) {
                SimpleTab(text = stringResource(id = R.string.editing_trim), selected = pagerState.currentPage == 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }

                SimpleTab(text = stringResource(id = R.string.editing_crop), selected = pagerState.currentPage == 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                }

                SimpleTab(text = stringResource(id = R.string.editing_adjust), selected = pagerState.currentPage == 2) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                }

                SimpleTab(text = stringResource(id = R.string.editing_draw), selected = pagerState.currentPage == 3) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(3)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                snapPosition = SnapPosition.Center,
                pageSize = PageSize.Fill,
            ) { index ->
                when (index) {
                    0 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .padding(8.dp)
                        ) {
                            VideoEditorTrimContent(
                                currentPosition = currentPosition,
                                duration = duration,
                                absolutePath = absolutePath,
                                leftPosition = leftTrimPosition,
                                rightPosition = rightTrimPosition,
                                onSeek = { pos ->
                                    exoPlayer.seekTo(
                                        (pos * 1000f).coerceAtMost(duration.floatValue * 1000f).toLong()
                                    )
                                },
                                setExoPlayerScrubbingState = { enabled ->
                                    exoPlayer.isScrubbingModeEnabled = enabled
                                }
                            )
                        }
                    }

                    else -> {
                        Text(text = "This definitely has been coded in")
                    }
                }
            }
        }
    }
}