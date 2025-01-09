package com.kaii.photos.compose.grids

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.FolderDoesntExist
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.ShowSelectedState
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.datastore.Storage
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.getBaseInternalStorageDirectory
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.math.roundToInt

private const val TAG = "PHOTO_GRID_VIEW"

@Composable
fun PhotoGrid(
    groupedMedia: MutableState<List<MediaStoreData>>,
    path: String?,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    modifier: Modifier = Modifier,
    viewProperties: ViewProperties,
    shouldPadUp: Boolean = false,
    state: LazyGridState = rememberLazyGridState()
) {
    val hasFiles = if (path == null) {
        groupedMedia.value.isNotEmpty()
    } else {
        val basePath = getBaseInternalStorageDirectory()
        if (viewProperties == ViewProperties.Trash) {
            Path("$basePath$path").checkHasFiles(true)
        } else {
            Path("$basePath$path").checkHasFiles()
        }
    }

    if (hasFiles == null) {
        FolderDoesntExist()
        return
    }

    if (hasFiles) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f)
                .then(modifier)
        ) {
            DeviceMedia(
                groupedMedia,
                selectedItemsList,
                viewProperties,
                shouldPadUp,
                state,
                path
            )
        }
    } else {
        FolderIsEmpty(viewProperties.emptyText, viewProperties.emptyIconResId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMedia(
    groupedMedia: MutableState<List<MediaStoreData>>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    viewProperties: ViewProperties,
    shouldPadUp: Boolean,
    gridState: LazyGridState,
    path: String?
) {
    var showLoadingSpinner by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    BackHandler(
        enabled = selectedItemsList.size > 0
    ) {
        selectedItemsList.clear()
    }

    if (groupedMedia.value.isNotEmpty()) {
        showLoadingSpinner = false
    }

    LaunchedEffect(groupedMedia.value) {
        MainActivity.mainViewModel.setGroupedMedia(groupedMedia.value)
    }

    val mainViewModel = MainActivity.mainViewModel

    val spacerHeight by animateDpAsState(
        targetValue = if (selectedItemsList.size > 0 && shouldPadUp) 80.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = if (selectedItemsList.size > 0 && shouldPadUp) 350 else 0
        ),
        label = "animate spacer on bottom of photo grid"
    )

    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(maxHeight - spacerHeight)
                .align(Alignment.TopCenter)
        ) {
            val cacheThumbnails by mainViewModel.settings.Storage.getCacheThumbnails().collectAsStateWithLifecycle(initialValue = false)
            val thumbnailSize by mainViewModel.settings.Storage.getThumbnailSize().collectAsStateWithLifecycle(initialValue = 0)

            val scrollSpeed = remember { mutableFloatStateOf(0f) }
            val isDragSelecting = remember { mutableStateOf(false) }
            val localDensity = LocalDensity.current

            LaunchedEffect(scrollSpeed.floatValue) {
                if (scrollSpeed.floatValue != 0f) {
                    while (isActive) {
                        gridState.scrollBy(scrollSpeed.floatValue)
                        delay(10)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(
                    if (!isLandscape) {
                        3
                    } else {
                        6
                    }
                ),
                userScrollEnabled = !isDragSelecting.value,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .align(Alignment.TopCenter)
                    .dragSelectionHandler(
                        state = gridState,
                        selectedItemsList = selectedItemsList,
                        groupedMedia = groupedMedia.value,
                        scrollSpeed = scrollSpeed,
                        scrollThreshold = with(localDensity) {
                            40.dp.toPx()
                        },
                        isDragSelecting = isDragSelecting
                    ),
                state = gridState
            ) {
                items(
                    count = groupedMedia.value.size,
                    key = {
                        groupedMedia.value[it].uri.toString()
                    },
                    span = { index ->
                        if (index < groupedMedia.value.size) {
                            val item = groupedMedia.value[index]
                            if (item.type == MediaType.Section) {
                                GridItemSpan(maxLineSpan)
                            } else {
                                GridItemSpan(1)
                            }
                        } else {
                            GridItemSpan(1)
                        }
                    }
                ) { i ->
                    if (groupedMedia.value.isEmpty()) return@items
                    val mediaStoreItem = groupedMedia.value[i]

                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .animateItem(
                                fadeInSpec = null
                            )
                    ) {
                        val navController = LocalNavController.current

                        MediaStoreItem(
                            item = mediaStoreItem,
                            groupedMedia = groupedMedia,
                            viewProperties = viewProperties,
                            selectedItemsList = selectedItemsList,
                            thumbnailSettings = Pair(cacheThumbnails, thumbnailSize),
                            isDragSelecting = isDragSelecting
                        ) {
                            when (viewProperties.operation) {
                                ImageFunctions.LoadNormalImage -> {
                                    mainViewModel.setSinglePhotoPath(path)
                                    mainViewModel.setSelectedMediaData(mediaStoreItem)
                                    mainViewModel.setGroupedMedia(groupedMedia.value)
                                    navController.navigate(MultiScreenViewType.SinglePhotoView.name)
                                }

                                ImageFunctions.LoadTrashedImage -> {
                                    mainViewModel.setSelectedMediaData(mediaStoreItem)
                                    mainViewModel.setGroupedMedia(groupedMedia.value)
                                    navController.navigate(MultiScreenViewType.SingleTrashedPhotoView.name)
                                }

                                ImageFunctions.LoadSecuredImage -> {
                                    mainViewModel.setSelectedMediaData(mediaStoreItem)
                                    mainViewModel.setGroupedMedia(groupedMedia.value)
                                    navController.navigate(MultiScreenViewType.SingleHiddenPhotoVew.name)
                                }
                            }
                        }
                    }
                }
            }

            if (showLoadingSpinner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(48.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(1000.dp))
                            .background(CustomMaterialTheme.colorScheme.surfaceContainer),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp),
                            color = CustomMaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight(1f)
                    .width(48.dp)
            ) {
                var showHandle by remember { mutableStateOf(false) }
                var isScrollingByHandle by remember { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }

                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is DragInteraction.Start -> {
                                isScrollingByHandle = true
                            }

                            is DragInteraction.Cancel -> {
                                isScrollingByHandle = false
                            }

                            is DragInteraction.Stop -> {
                                isScrollingByHandle = false
                            }

                            else -> {}
                        }
                    }
                }

                LaunchedEffect(key1 = gridState.isScrollInProgress, key2 = isScrollingByHandle) {
                    if (gridState.isScrollInProgress || isScrollingByHandle) {
                        showHandle = true
                    } else {
                        delay(
                            if (selectedItemsList.isNotEmpty()) 1000 else 3000
                        )
                        showHandle = false
                    }
                }

                val listSize by remember {
                    derivedStateOf {
                        groupedMedia.value.size - 1
                    }
                }
                val totalLeftOverItems by remember {
                    derivedStateOf {
                        (listSize - gridState.layoutInfo.visibleItemsInfo.size).toFloat()
                    }
                }
                AnimatedVisibility(
                    visible = showHandle && !showLoadingSpinner && totalLeftOverItems > 50f,
                    modifier = Modifier.fillMaxHeight(1f),
                    enter =
                    slideInHorizontally { width -> width },
                    exit =
                    slideOutHorizontally { width -> width }
                ) {
                    val visibleItemIndex = remember { derivedStateOf { gridState.firstVisibleItemIndex } }
                    val percentScrolled by remember {
                        derivedStateOf {
                            visibleItemIndex.value / totalLeftOverItems
                        }
                    }

                    Slider(
                        value = percentScrolled,
                        interactionSource = interactionSource,
                        onValueChange = {
                            coroutineScope.launch {
                                if (isScrollingByHandle) {
                                    gridState.scrollToItem(
                                        (it * groupedMedia.value.size).roundToInt()
                                    )
                                }
                            }
                        },
                        valueRange = 0f..1f,
                        thumb = { state ->
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(96.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(0.dp, 0.dp, 1000.dp, 1000.dp))
                                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                                        .align(Alignment.Center)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.code),
                                        contentDescription = "scrollbar handle",
                                        tint = CustomMaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )

                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .rotate(-90f)
                                        .graphicsLayer {
                                            translationX = -220f
                                        }
                                ) {
                                    AnimatedVisibility(
                                        visible = isScrollingByHandle,
                                        enter =
                                        slideInHorizontally { width -> width / 4 } + fadeIn(),
                                        exit =
                                        slideOutHorizontally { width -> width / 4 } + fadeOut(),
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .height(32.dp)
                                            .wrapContentWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(32.dp)
                                                .wrapContentWidth()
                                                .clip(RoundedCornerShape(1000.dp))
                                                .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                                                .padding(8.dp, 4.dp)
                                        ) {
                                            val item = groupedMedia.value[(state.value * listSize).roundToInt()]
                                            val format = DateTimeFormatter.ofPattern("MMM yyyy")
                                            val formatted = Instant.ofEpochSecond(item.dateTaken).atZone(ZoneId.systemDefault()).toLocalDateTime().format(format)

                                            Text(
                                                text = formatted,
                                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                                textAlign = TextAlign.Center,
                                                color = CustomMaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        track = {
                            val colors = SliderDefaults.colors()
                            SliderDefaults.Track(
                                sliderState = it,
                                trackInsideCornerSize = 8.dp,
                                colors = colors.copy(
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                    disabledActiveTickColor = Color.Transparent,
                                    disabledInactiveTickColor = Color.Transparent,

                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                thumbTrackGapSize = 4.dp,
                                drawTick = { _, _ -> },
                                modifier = Modifier
                                    .height(16.dp)
                            )
                        },
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight(1f)
                            .graphicsLayer {
                                rotationZ = 90f
                                translationX = 30f
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints(
                                        minWidth = constraints.minHeight,
                                        minHeight = constraints.minWidth,
                                        maxWidth = constraints.maxHeight,
                                        maxHeight = constraints.maxWidth
                                    )
                                )

                                layout(placeable.height, placeable.width) {
                                    placeable.place(0, -placeable.height)
                                }
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaStoreItem(
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    viewProperties: ViewProperties,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    thumbnailSettings: Pair<Boolean, Int>,
    isDragSelecting: MutableState<Boolean>,
    onClick: () -> Unit
) {
    val vibratorManager = rememberVibratorManager()

    if (item.type == MediaType.Section) {
        val sectionItems by remember {
            derivedStateOf {
                groupedMedia.value.filter {
                    if (viewProperties.sortMode == MediaItemSortMode.LastModified) {
                        it.getLastModifiedDay() == item.getLastModifiedDay() && it.type != MediaType.Section
                    } else {
                        it.getDateTakenDay() == item.getDateTakenDay() && it.type != MediaType.Section
                    }
                }
            }
        }
        val isSectionSelected by remember {
            derivedStateOf {
                selectedItemsList.containsAll(sectionItems)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (isSectionSelected) {
                        selectedItemsList.removeAll(sectionItems)
                        selectedItemsList.remove(item)
                    } else {
                        if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()

                        selectedItemsList.addAll(sectionItems)
                        selectedItemsList.add(item)
                    }

                    vibratorManager.vibrateLong()
                }
                .padding(16.dp, 8.dp),
        ) {
            Text(
                text = "${viewProperties.prefix}${item.displayName}",
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = CustomMaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )

            ShowSelectedState(
                isSelected = isSectionSelected,
                showIcon = selectedItemsList.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    } else {
        val isSelected by remember/*(selectedItemsList.size)*/ { derivedStateOf { selectedItemsList.contains(item) } }

        val animatedItemCornerRadius by animateDpAsState(
            targetValue = if (isSelected) 16.dp else 0.dp,
            animationSpec = tween(
                durationMillis = 150,
            ),
            label = "animate corner radius of selected item"
        )
        val animatedItemScale by animateFloatAsState(
            targetValue = if (isSelected) 0.8f else 1f,
            animationSpec = tween(
                durationMillis = 150
            ),
            label = "animate scale of selected item"
        )

        val onSingleClick: () -> Unit = {
            vibratorManager.vibrateShort()
            if (selectedItemsList.isNotEmpty()) {
                if (isSelected) {
                    selectedItemsList.remove(item)
                } else {
                    if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()

                    selectedItemsList.add(item)
                }
            } else {
                onClick()
            }
        }

        val onLongClick: () -> Unit = {
            isDragSelecting.value = true

            vibratorManager.vibrateLong()
            if (isSelected) {
                selectedItemsList.remove(item)
            } else {
                if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()

                selectedItemsList.add(item)
            }
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(CustomMaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                .then(
                    if (selectedItemsList.isNotEmpty()) {
                        Modifier.clickable {
                            onSingleClick()
                        }
                    } else {
                        Modifier.combinedClickable(
                            onClick = onSingleClick,

                            onLongClick = onLongClick
                        )
                    }
                )
        ) {
            GlideImage(
                model = if (viewProperties == ViewProperties.SecureFolder) item.uri.path else item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .fillMaxSize(1f)
                    .align(Alignment.Center)
                    .scale(animatedItemScale)
                    .clip(RoundedCornerShape(animatedItemCornerRadius))
            ) {
                if (thumbnailSettings.second == 0) {
                    it.signature(item.signature())
                        .diskCacheStrategy(
                            if (thumbnailSettings.first) DiskCacheStrategy.ALL
                            else DiskCacheStrategy.NONE
                        )
                } else {
                    it.signature(item.signature())
                        .diskCacheStrategy(
                            if (thumbnailSettings.first) DiskCacheStrategy.ALL
                            else DiskCacheStrategy.NONE
                        )
                        .override(thumbnailSettings.second)
                }
            }

            if (item.type == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.movie_filled),
                        contentDescription = "file is video indicator",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            ShowSelectedState(
                isSelected = isSelected,
                showIcon = selectedItemsList.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
    }
}

fun Modifier.dragSelectionHandler(
    state: LazyGridState,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>,
    scrollSpeed: MutableFloatState,
    scrollThreshold: Float,
    isDragSelecting: MutableState<Boolean>
) = pointerInput(Unit) {
    var initialKey: Int? = null
    var currentKey: Int? = null

	if (groupedMedia.isEmpty()) return@pointerInput

    val numberOfHorizontalItems = state.layoutInfo.viewportSize.width / state.layoutInfo.visibleItemsInfo[0].size.width

    detectDragGestures(
        onDragStart = { offset ->
            isDragSelecting.value = true

            if (selectedItemsList.size == 1 && selectedItemsList[0] != MediaStoreData()) {
                initialKey = groupedMedia.indexOf(selectedItemsList[0])
                currentKey = initialKey
            } else {
                state.getGridItemAtOffset(offset, groupedMedia, numberOfHorizontalItems)?.let { key ->
                    val item = groupedMedia[key]

                    if (item.type != MediaType.Section) {
                        initialKey = key
                        currentKey = key
                        if (!selectedItemsList.contains(item)) selectedItemsList.add(item)
                    }
                }
            }
        },

        onDragCancel = {
            initialKey = null
            scrollSpeed.floatValue = 0f
            isDragSelecting.value = false
        },

        onDragEnd = {
            initialKey = null
            scrollSpeed.floatValue = 0f
            isDragSelecting.value = false
        },

        onDrag = { change, _ ->
            if (initialKey != null) {
                val distanceFromBottom = state.layoutInfo.viewportSize.height - change.position.y
                val distanceFromTop = change.position.y // for clarity

                scrollSpeed.floatValue = when {
                    distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
                    distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
                    else -> 0f
                }

                state.getGridItemAtOffset(change.position, groupedMedia, numberOfHorizontalItems)?.let { key ->
                    if (currentKey != key) {
                        selectedItemsList.apply {
                            val toBeRemoved =
                                if (initialKey!! <= currentKey!!) groupedMedia.subList(initialKey!!, currentKey!! + 1)
                                else groupedMedia.subList(currentKey!!, initialKey!! + 1)

                            removeAll(toBeRemoved)

                            val toBeAdded =
                                if (initialKey!! <= key) groupedMedia.subList(initialKey!!, key + 1)
                                else groupedMedia.subList(key, initialKey!! + 1)

                            addAll(toBeAdded.filter { it.type != MediaType.Section })
                        }

                        currentKey = key
                    }
                }
            }
        }
    )
}

fun LazyGridState.getGridItemAtOffset(offset: Offset, groupedMedia: List<MediaStoreData>, numberOfHorizontalItems: Int): Int? {
    var key: String? = null

    Log.d(TAG, "grid displays $numberOfHorizontalItems horizontal items")

    // scan the entire row for this item
    // if theres only one or two items on a row and user drag selects to the empty space they get selected
    for (i in 1..numberOfHorizontalItems) {
        val possibleItem = layoutInfo.visibleItemsInfo.find { item ->
            val stretched = item.size.toIntRect().let {
                IntRect(
                    top = it.top,
                    bottom = it.bottom,
                    left = it.left,
                    right = it.right * i
                )
            }
            stretched.contains(offset.round() - item.offset)
        }

        if (possibleItem != null) {
            key = possibleItem.key as String
            break
        }
    }

    val found = groupedMedia.find {
        it.uri.toString() == key
    } ?: return null

    return groupedMedia.indexOf(found)
}

