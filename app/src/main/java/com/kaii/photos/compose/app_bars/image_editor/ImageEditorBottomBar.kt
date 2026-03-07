package com.kaii.photos.compose.app_bars.image_editor

import android.net.Uri
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kaii.photos.compose.editing_view.EditorApp
import com.kaii.photos.compose.editing_view.SharedEditorDrawContent
import com.kaii.photos.compose.editing_view.SharedEditorFilterContent
import com.kaii.photos.compose.editing_view.SharedEditorMoreContent
import com.kaii.photos.compose.editing_view.getAvailableEditorsForType
import com.kaii.photos.compose.editing_view.image_editor.ImageEditorAdjustContent
import com.kaii.photos.compose.editing_view.image_editor.ImageEditorCropContent
import com.kaii.photos.compose.widgets.SimpleTab
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageEditorTabs
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.launch

@Composable
fun ImageEditorBottomBar(
    modifications: SnapshotStateList<ImageModification>,
    originalAspectRatio: Float,
    imageEditingState: ImageEditingState,
    drawingPaintState: DrawingPaintState,
    pagerState: PagerState,
    uri: Uri,
    modifier: Modifier = Modifier,
    increaseModCount: () -> Unit,
    saveEffect: (MediaColorFilters) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val navBarHeight = with(localDensity) {
        WindowInsets.navigationBars.getBottom(localDensity).toDp()
    }

    BottomAppBar(
        modifier = modifier
            .height(120.dp + navBarHeight)
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
                    mediaType = MediaType.Image
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
                ImageEditorTabs.entries.forEach { entry ->
                    if (entry != ImageEditorTabs.More || availableEditors.isNotEmpty()) {
                        SimpleTab(
                            text = stringResource(id = entry.title),
                            selected = pagerState.currentPage == ImageEditorTabs.entries.indexOf(entry)
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(ImageEditorTabs.entries.indexOf(entry))
                            }
                        }
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
                    ImageEditorTabs.entries.indexOf(ImageEditorTabs.Crop) -> {
                        ImageEditorCropContent(
                            imageAspectRatio = originalAspectRatio,
                            croppingAspectRatio = imageEditingState.croppingAspectRatio,
                            rotation = imageEditingState.rotation,
                            resolutionScale = imageEditingState.resolutionScale,
                            setCroppingAspectRatio = imageEditingState::setCroppingAspectRatio,
                            setRotation = {
                                imageEditingState.rotation = it
                            },
                            resetCrop = {
                                imageEditingState.resetCrop(true)
                            },
                            getScaledResolution = {
                                imageEditingState.getScaledResolution(it)
                            },
                            scaleResolution = {
                                imageEditingState.resolution = imageEditingState.getScaledResolution(it)
                            }
                        )
                    }

                    ImageEditorTabs.entries.indexOf(ImageEditorTabs.Adjust) -> {
                        ImageEditorAdjustContent(
                            modifications = modifications,
                            increaseModCount = increaseModCount
                        )
                    }

                    ImageEditorTabs.entries.indexOf(ImageEditorTabs.Filters) -> {
                        SharedEditorFilterContent(
                            modifications = drawingPaintState.modifications,
                            saveEffect = saveEffect
                        )
                    }

                    ImageEditorTabs.entries.indexOf(ImageEditorTabs.Draw) -> {
                        SharedEditorDrawContent(
                            drawingPaintState = drawingPaintState,
                            currentTime = 0f
                        )
                    }

                    ImageEditorTabs.entries.indexOf(ImageEditorTabs.More) -> {
                        SharedEditorMoreContent(
                            apps = availableEditors,
                            uri = uri,
                            mediaType = MediaType.Image
                        )
                    }
                }
            }
        }
    }
}