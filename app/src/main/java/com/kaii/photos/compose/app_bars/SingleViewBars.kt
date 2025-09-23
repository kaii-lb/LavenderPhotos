package com.kaii.photos.compose.app_bars

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.FullWidthDialogButton
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.WallpaperTypeDialog
import com.kaii.photos.compose.editing_view.SharedEditorDrawContent
import com.kaii.photos.compose.editing_view.SharedEditorFilterContent
import com.kaii.photos.compose.editing_view.image_editor.ImageEditorAdjustContent
import com.kaii.photos.compose.editing_view.video_editor.SharedEditorCropContent
import com.kaii.photos.compose.editing_view.video_editor.TrimContent
import com.kaii.photos.compose.editing_view.video_editor.VideoEditorAdjustContent
import com.kaii.photos.compose.editing_view.video_editor.VideoEditorProcessingContent
import com.kaii.photos.compose.widgets.SelectableDropDownMenuItem
import com.kaii.photos.compose.widgets.SimpleTab
import com.kaii.photos.datastore.Editing
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.VideoPlayerConstants
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.CroppingAspectRatio
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageEditorTabs
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.MediaColorFilters
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoEditorTabs
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.editing.saveVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    modifications: SnapshotStateList<VideoModification>,
    increaseModCount: () -> Unit,
    onSeek: (Float) -> Unit,
    saveEffect: (MediaColorFilters) -> Unit
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
            val metadata = remember { MediaMetadataRetriever() }
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
    basicVideoData: BasicVideoData,
    videoEditingState: VideoEditingState,
    drawingPaintState: DrawingPaintState,
    lastSavedModCount: MutableIntState,
    containerDimens: Size,
    canvasSize: Size,
    isFromOpenWithView: Boolean
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
                    val textMeasurer = rememberTextMeasurer()

                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            lastSavedModCount.intValue = modifications.size

                            // mainViewModel so it doesn't die if user exits before video is saved
                            mainViewModel.launch {
                                saveVideo(
                                    context = context,
                                    modifications = modifications + drawingPaintState.modifications.map {
                                        it as VideoModification
                                    },
                                    videoEditingState = videoEditingState,
                                    basicVideoData = basicVideoData,
                                    uri = uri,
                                    absolutePath = absolutePath,
                                    overwrite = overwrite,
                                    containerDimens = containerDimens,
                                    canvasSize = canvasSize,
                                    textMeasurer = textMeasurer,
                                    isFromOpenWithView = isFromOpenWithView
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WallpaperSetterTopBar(
    uri: Uri,
    mimeType: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .fillMaxWidth(1f)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(
            onClick = onDismiss,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .dropShadow(
                    shape = CircleShape,
                    shadow = Shadow(
                        radius = 6.dp,
                        color = Color.Black,
                        spread = (-4).dp,
                        alpha = 0.5f
                    )
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_arrow),
                contentDescription = stringResource(id = R.string.return_to_previous_page),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        val context = LocalContext.current
        val resources = LocalResources.current
        FilledTonalIconButton(
            onClick = {
                val intent = Intent().apply {
                    action = Intent.ACTION_ATTACH_DATA
                    data = uri
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra("mimeType", mimeType)
                }

                context.startActivity(
                    Intent.createChooser(
                        intent,
                        resources.getString(R.string.set_as_wallpaper)
                    )
                )
            },
            shape = MaterialShapes.Square.toShape(),
            modifier = Modifier
                .dropShadow(
                    shape = RoundedCornerShape(12.dp),
                    shadow = Shadow(
                        radius = 6.dp,
                        color = Color.Black,
                        spread = (-4).dp,
                        alpha = 0.5f
                    )
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more_options),
                contentDescription = stringResource(id = R.string.show_options),
                modifier = Modifier
                    .size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperSetterBottomBar(
    bitmap: Bitmap,
    offset: Offset,
    outerScale: Float,
    modifier: Modifier = Modifier,
    close: () -> Unit,
) {
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .fillMaxWidth(1f)
            .height(56.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            WallpaperTypeDialog(
                onSetWallpaperType = { wallpaperType ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        val deviceSize = Size(
                            width = windowManager.currentWindowMetrics.bounds.width().toFloat(),
                            height = windowManager.currentWindowMetrics.bounds.height().toFloat()
                        )

                        val destinationBitmap =
                            createBitmap(deviceSize.width.toInt(), deviceSize.height.toInt(), bitmap.config ?: Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(destinationBitmap)
                        val originalAspectRatio = bitmap.width.toFloat() / bitmap.height
                        val deviceAspectRatio = deviceSize.width / deviceSize.height

                        val scale =
                            if (originalAspectRatio >= deviceAspectRatio) deviceSize.height / bitmap.height
                            else deviceSize.width / bitmap.width

                        val centeringOffset = Offset(
                            x = (deviceSize.width - bitmap.width * scale * outerScale) / 2f,
                            y = (deviceSize.height - bitmap.height * scale * outerScale) / 2f
                        )

                        val matrix = Matrix().apply {
                            postScale(scale * outerScale, scale * outerScale)
                            postTranslate(centeringOffset.x + offset.x, centeringOffset.y + offset.y)
                        }
                        canvas.drawBitmap(bitmap, matrix, Paint().apply { isAntiAlias = true })

                        val wallpaperManager = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager

                        wallpaperManager.setBitmap(destinationBitmap, null, true, wallpaperType.flag)

                        delay(1000)

                        close()
                    }
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }

        FullWidthDialogButton(
            text = stringResource(id = R.string.apply),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single,
            modifier = Modifier
                .dropShadow(
                    shape = RoundedCornerShape(1000.dp),
                    shadow = Shadow(
                        radius = 8.dp,
                        color = Color.Black,
                        spread = 2.dp,
                        alpha = 0.5f
                    )
                )
        ) {
            showDialog = true
        }
    }
}

@Composable
fun ImageEditorBottomBar(
    modifications: SnapshotStateList<ImageModification>,
    originalAspectRatio: Float,
    imageEditingState: ImageEditingState,
    drawingPaintState: DrawingPaintState,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    increaseModCount: () -> Unit,
    saveEffect: (MediaColorFilters) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    BottomAppBar(
        modifier = modifier
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
                ImageEditorTabs.entries.forEach { entry ->
                    SimpleTab(text = stringResource(id = entry.title), selected = pagerState.currentPage == ImageEditorTabs.entries.indexOf(entry)) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(ImageEditorTabs.entries.indexOf(entry))
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
                        SharedEditorCropContent(
                            imageAspectRatio = originalAspectRatio,
                            croppingAspectRatio = imageEditingState.croppingAspectRatio,
                            rotation = imageEditingState.rotation,
                            setCroppingAspectRatio = imageEditingState::setCroppingAspectRatio,
                            setRotation = imageEditingState::setRotation,
                            resetCrop = {
                                imageEditingState.resetCrop(true)
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
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageEditorTopBar(
    modifications: List<ImageModification>,
    lastSavedModCount: MutableIntState,
    overwrite: Boolean,
    setOverwrite: (Boolean) -> Unit,
    saveImage: suspend () -> Unit
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

            LaunchedEffect(overwriteByDefault) {
                setOverwrite(overwriteByDefault)
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
                    setOverwrite(true)
                    showDropDown = false
                }

                SelectableDropDownMenuItem(
                    text = stringResource(id = R.string.editing_save),
                    iconResId = R.drawable.checkmark_thin,
                    isSelected = true
                ) {
                    setOverwrite(false)
                    showDropDown = false
                }
            }

            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            lastSavedModCount.intValue = modifications.size

                            // mainViewModel so it doesn't die if user exits before image is saved
                            mainViewModel.launch {
                                saveImage()
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