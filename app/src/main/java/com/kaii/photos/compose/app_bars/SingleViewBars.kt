package com.kaii.photos.compose.app_bars

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.SelectableDropDownMenuItem
import com.kaii.photos.compose.SimpleTab
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.single_photo.editing_view.BasicVideoData
import com.kaii.photos.compose.single_photo.editing_view.CroppingAspectRatio
import com.kaii.photos.compose.single_photo.editing_view.TrimContent
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorAdjustContent
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorCropContent
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorFilterContent
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorProcessingContent
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorTabs
import com.kaii.photos.compose.single_photo.editing_view.VideoModification
import com.kaii.photos.compose.single_photo.editing_view.saveVideo
import com.kaii.photos.datastore.Editing
import com.kaii.photos.helpers.VideoPlayerConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

// insane amount of vars, should probably clean up
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditorBottomBar(
    pagerState: PagerState,
    currentPosition: MutableFloatState,
    basicData: BasicVideoData,
    leftPosition: MutableFloatState,
    rightPosition: MutableFloatState,
    modifications: SnapshotStateList<VideoModification>,
    croppingAspectRatio: MutableState<CroppingAspectRatio>,
    increaseModCount: () -> Unit,
    onCropReset: () -> Unit,
    onSeek: (Float) -> Unit,
    onRotate: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

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
            SecondaryScrollableTabRow(
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
                VideoEditorTabs.entries.forEach { entry ->
                    SimpleTab(text = stringResource(id = entry.title), selected = pagerState.currentPage == VideoEditorTabs.entries.indexOf(entry)) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(VideoEditorTabs.entries.indexOf(entry))
                        }
                    }
                }
            }

            // preload and save for all so we don't have to retrieve every time user navigates to first tab
            val coroutineScope = rememberCoroutineScope()
            val metadata = MediaMetadataRetriever()
            val thumbnails = remember { mutableStateListOf<Bitmap>() }
            val windowInfo = LocalWindowInfo.current

            LaunchedEffect(basicData) {
                Log.d("SINGLE_VIEW_BARS", "Basic data updated $basicData")
                if (basicData.duration <= 0f) return@LaunchedEffect

                coroutineScope.launch(Dispatchers.IO) {
                    metadata.setDataSource(basicData.absolutePath)

                    val stepSize = basicData.duration.roundToInt().seconds.inWholeMicroseconds / 6

                    for (i in 0..(VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - 1)) {
                        val new = metadata.getScaledFrameAtTime(
                            stepSize * i,
                            MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
                            windowInfo.containerSize.width / (VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - 2),
                            windowInfo.containerSize.width / (VideoPlayerConstants.TRIM_THUMBNAIL_COUNT - 2)
                        )

                        new?.let { thumbnails.add(it) }
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
                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Trim) -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .padding(8.dp)
                        ) {
                            TrimContent(
                                currentPosition = currentPosition,
                                leftPosition = leftPosition,
                                rightPosition = rightPosition,
                                thumbnails = thumbnails,
                                onSeek = onSeek,
                                basicData = basicData
                            )
                        }
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Crop) -> {
                        VideoEditorCropContent(
                            imageAspectRatio = basicData.aspectRatio,
                            croppingAspectRatio = croppingAspectRatio,
                            onReset = onCropReset,
                            onRotate = onRotate
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Video) -> {
                        VideoEditorProcessingContent(
                            basicData = basicData,
                            modifications = modifications
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust) -> {
                        VideoEditorAdjustContent(
                            modifications = modifications,
                            increaseModCount = increaseModCount
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Filters) -> {
                        VideoEditorFilterContent(
                            thumbnails = thumbnails,
                            modifications = modifications,
                            increaseModCount = increaseModCount
                        )
                    }

                    else -> {
                        Text(
                            text = "This definitely has been coded in",
                            modifier = Modifier
                                .fillMaxSize(1f)
                        )
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoEditorTopBar(
    uri: Uri,
    absolutePath: String,
    modifications: SnapshotStateList<VideoModification>,
    lastSavedModCount: MutableIntState,
    containerDimens: Size,
    videoDimens: IntSize
) {
    val navController = LocalNavController.current

    TopAppBar(
        title = {},
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(8.dp, 0.dp, 0.dp, 0.dp)
            ) {
                val showDialog = remember { mutableStateOf(false) }

                if (showDialog.value) {
                    ConfirmationDialog(
                        showDialog = showDialog,
                        dialogTitle = stringResource(id = R.string.editing_discard_desc),
                        confirmButtonLabel = stringResource(id = R.string.editing_discard)
                    ) {
                        navController.popBackStack()
                    }
                }
                FilledTonalIconButton(
                    onClick = {
                        if (lastSavedModCount.intValue < modifications.size) {
                            showDialog.value = true
                        } else {
                            navController.popBackStack()
                        }
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

            val mainViewModel = LocalMainViewModel.current
            val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)
            var overwrite by remember { mutableStateOf(false) }

            LaunchedEffect(overwriteByDefault) {
                overwrite = overwriteByDefault
            }

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
                    overwrite = true
                    showDropDown = false
                }

                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_save),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = true
                ) {
                    overwrite = false
                    showDropDown = false
                }
            }

            SplitButtonLayout(
                leadingButton = {
                    val context = LocalContext.current
                    val resources = LocalResources.current
                    val coroutineScope = rememberCoroutineScope()

                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            lastSavedModCount.intValue = modifications.size

                            // mainViewModel so it doesn't die if user exits before video is saved
                            mainViewModel.launch {
                                saveVideo(
                                    context = context,
                                    modifications = modifications,
                                    uri = uri,
                                    absolutePath = absolutePath,
                                    overwrite = overwrite,
                                    containerDimens = containerDimens,
                                    videoDimens = videoDimens
                                ) {
                                    coroutineScope.launch {
                                        LavenderSnackbarController.pushEvent(
                                            event = LavenderSnackbarEvents.MessageEvent(
                                                message = resources.getString(R.string.editing_export_video_failed),
                                                icon = R.drawable.error_2,
                                                duration = SnackbarDuration.Short
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            text =
                                if (overwrite) stringResource(id = R.string.editing_overwrite)
                                else stringResource(id = R.string.editing_save)
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
