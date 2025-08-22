package com.kaii.photos.compose.app_bars

import android.net.Uri
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
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.SelectableDropDownMenuItem
import com.kaii.photos.compose.SimpleTab
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.single_photo.editing_view.VideoEditorTrimContent
import com.kaii.photos.compose.single_photo.editing_view.VideoModification
import com.kaii.photos.compose.single_photo.editing_view.saveVideo
import com.kaii.photos.datastore.Editing
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditorBottomBar(
    pagerState: PagerState,
    currentPosition: MutableFloatState,
    duration: MutableFloatState,
    absolutePath: String,
    exoPlayer: ExoPlayer,
    leftPosition: MutableFloatState,
    rightPosition: MutableFloatState
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
                                leftPosition = leftPosition,
                                rightPosition = rightPosition,
                                onSeek = { pos ->
                                    exoPlayer.seekTo(
                                        (pos * 1000f).coerceAtMost(duration.floatValue * 1000f).toLong()
                                    )
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

                            coroutineScope.launch {
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
