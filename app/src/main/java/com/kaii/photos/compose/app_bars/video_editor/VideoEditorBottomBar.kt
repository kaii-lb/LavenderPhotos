package com.kaii.photos.compose.app_bars.video_editor

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import com.kaii.photos.compose.editing_view.EditorApp
import com.kaii.photos.compose.editing_view.SharedEditorCropContent
import com.kaii.photos.compose.editing_view.SharedEditorDrawContent
import com.kaii.photos.compose.editing_view.SharedEditorFilterContent
import com.kaii.photos.compose.editing_view.SharedEditorMoreContent
import com.kaii.photos.compose.editing_view.getAvailableEditorsForType
import com.kaii.photos.compose.editing_view.video_editor.TrimContent
import com.kaii.photos.compose.editing_view.video_editor.VideoEditorAdjustContent
import com.kaii.photos.compose.editing_view.video_editor.VideoEditorProcessingContent
import com.kaii.photos.compose.widgets.SimpleTab
import com.kaii.photos.helpers.VideoPlayerConstants
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.CroppingAspectRatio
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

private const val TAG = "com.kaii.photos.app_bars.video_editor.VideoEditorBottomBar"

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorBottomBar(
    pagerState: PagerState,
    currentPosition: MutableFloatState,
    basicData: BasicVideoData,
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    modifications: SnapshotStateList<VideoModification>,
    uri: Uri,
    increaseModCount: () -> Unit,
    onSeek: (Float) -> Unit,
    saveEffect: (MediaColorFilters) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val navBarHeight = with(localDensity) {
        WindowInsets.navigationBars.getBottom(localDensity).toDp()
    }

    BottomAppBar(
        modifier = Modifier
            .height(120.dp + navBarHeight)
            .then(
                if (pagerState.currentPage == VideoEditorTabs.entries.indexOf(VideoEditorTabs.Trim)) Modifier.systemGestureExclusion()
                else Modifier.Companion
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val context = LocalContext.current
            var availableEditors by remember {
                mutableStateOf(
                    emptyList<EditorApp>()
                )
            }

            LaunchedEffect(Unit) {
                availableEditors = getAvailableEditorsForType(
                    context = context,
                    mediaType = MediaType.Video,
                )
            }

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
                    if (entry != VideoEditorTabs.More || availableEditors.isNotEmpty()) {
                        SimpleTab(
                            text = stringResource(id = entry.title),
                            selected = pagerState.currentPage == VideoEditorTabs.entries.indexOf(entry)
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(VideoEditorTabs.entries.indexOf(entry))
                            }
                        }
                    }
                }
            }

            // preload and save for all so we don't have to retrieve every time user navigates to first tab
            val metadata = remember { MediaMetadataRetriever() }
            val thumbnails = remember { mutableStateListOf<Bitmap>() }
            val windowInfo = LocalWindowInfo.current

            LaunchedEffect(basicData) {
                Log.d(TAG, "Basic data updated $basicData")
                if (basicData.duration <= 0f) return@LaunchedEffect

                coroutineScope.launch(Dispatchers.IO) {
                    metadata.setDataSource(basicData.absolutePath)

                    val stepSize = basicData.duration.roundToInt().seconds.inWholeMicroseconds / 6

                    for (i in 0..<VideoPlayerConstants.TRIM_THUMBNAIL_COUNT) {
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
                                videoEditingState = videoEditingState,
                                thumbnails = thumbnails,
                                onSeek = onSeek,
                                basicData = basicData
                            )
                        }
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Crop) -> {
                        SharedEditorCropContent(
                            imageAspectRatio = basicData.aspectRatio,
                            croppingAspectRatio = videoEditingState.croppingAspectRatio,
                            rotation = videoEditingState.rotation,
                            setCroppingAspectRatio = videoEditingState::setCroppingAspectRatio,
                            setRotation = videoEditingState::setRotation,
                            resetCrop = {
                                videoEditingState.setRotation(0f)
                                videoEditingState.setCroppingAspectRatio(CroppingAspectRatio.FreeForm)
                                videoEditingState.resetCrop(true)
                            }
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Video) -> {
                        VideoEditorProcessingContent(
                            basicData = basicData,
                            videoEditingState = videoEditingState
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Adjust) -> {
                        VideoEditorAdjustContent(
                            modifications = modifications,
                            increaseModCount = increaseModCount
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Filters) -> {
                        SharedEditorFilterContent(
                            modifications = drawingPaintState.modifications,
                            saveEffect = saveEffect
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.Draw) -> {
                        SharedEditorDrawContent(
                            drawingPaintState = drawingPaintState,
                            currentTime = currentPosition.floatValue
                        )
                    }

                    VideoEditorTabs.entries.indexOf(VideoEditorTabs.More) -> {
                        SharedEditorMoreContent(
                            apps = availableEditors,
                            uri = uri,
                            mediaType = MediaType.Video
                        )
                    }
                }
            }
        }
    }
}