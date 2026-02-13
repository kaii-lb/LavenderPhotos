package com.kaii.photos.compose.grids

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.widgets.FloatingScrollbar
import com.kaii.photos.compose.widgets.ShowSelectedState
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.grid_management.BitmapUriShadowBuilder
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.isRawImage
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private const val TAG = "com.kaii.photos.compose.grids.PhotoGridView"

@Composable
fun PhotoGrid(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    albumInfo: AlbumInfo,
    modifier: Modifier = Modifier,
    viewProperties: ViewProperties,
    selectionManager: SelectionManager,
    isMediaPicker: Boolean = false,
    isMainPage: Boolean = false,
    state: LazyGridState = rememberLazyGridState()
) {
    if (!pagingItems.loadState.source.append.endOfPaginationReached || pagingItems.itemCount != 0) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f)
                .then(modifier)
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            DeviceMedia(
                pagingItems = pagingItems,
                viewProperties = viewProperties,
                selectionManager = selectionManager,
                gridState = state,
                albumInfo = albumInfo,
                isMediaPicker = isMediaPicker,
                isMainPage = isMainPage
            )
        }
    } else {
        val resources = LocalResources.current
        FolderIsEmpty(
            ViewProperties.getText(id = viewProperties.emptyText, resources = resources),
            viewProperties.emptyIconResId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceMedia(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    viewProperties: ViewProperties,
    selectionManager: SelectionManager,
    gridState: LazyGridState,
    albumInfo: AlbumInfo,
    isMediaPicker: Boolean,
    isMainPage: Boolean
) {
    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

    BackHandler(
        enabled = isSelecting
    ) {
        selectionManager.clear()
    }

    val mainViewModel = LocalMainViewModel.current
    val isLandscape by rememberDeviceOrientation()

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val cacheThumbnails by mainViewModel.settings.storage.getCacheThumbnails()
            .collectAsStateWithLifecycle(initialValue = false)
        val thumbnailSize by mainViewModel.settings.storage.getThumbnailSize()
            .collectAsStateWithLifecycle(initialValue = 0)

        val context = LocalContext.current
        val columnSize by mainViewModel.columnSize.collectAsStateWithLifecycle()
        val useRoundedCorners by mainViewModel.settings.lookAndFeel.getUseRoundedCorners().collectAsStateWithLifecycle(initialValue = false)
        val openVideosExternally by mainViewModel.settings.behaviour.getOpenVideosExternally().collectAsStateWithLifecycle(initialValue = false)

        // delays loading items until the "slide in" animation has finished, so that it doesn't lag or stutter
        var showItems by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(AnimationConstants.DURATION.toLong())
            showItems = true
        }

        val navController = LocalNavController.current
        val isDragSelecting = remember { mutableStateOf(false) }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(
                if (isLandscape) {
                    columnSize * 2
                } else {
                    columnSize
                }
            ),
            userScrollEnabled = !isDragSelecting.value,
            modifier = Modifier
                .testTag("mainlazycolumn")
                .fillMaxSize(1f)
                .align(Alignment.TopCenter)
                .dragSelectionHandler(
                    state = gridState,
                    selectionManager = selectionManager,
                    gridState = gridState,
                    pagingItems = pagingItems,
                    isDragSelecting = isDragSelecting,
                    context = context,
                    thumbnailSettings = Pair(cacheThumbnails, thumbnailSize)
                )
        ) {
            items(
                count = if (showItems) pagingItems.itemCount else 0,
                key = pagingItems.itemKey { it.itemKey() },
                contentType = pagingItems.itemContentType { it::class },
                span = { index ->
                    if (index < pagingItems.itemCount) {
                        val item = pagingItems[index]

                        if (item is PhotoLibraryUIModel.Section) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            GridItemSpan(1)
                        }
                    } else {
                        GridItemSpan(1)
                    }
                }
            ) { i ->
                val item = pagingItems[i]

                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateItem()
                ) {
                    if (item == null) {
                        LoadingMediaStoreItem(
                            item = PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem),
                            useRoundedCorners
                        )
                    } else {
                        MediaStoreItem(
                            item = item,
                            viewProperties = viewProperties,
                            isSelecting = { isSelecting },
                            thumbnailSettings = Pair(cacheThumbnails, thumbnailSize),
                            isDragSelecting = isDragSelecting,
                            isMediaPicker = isMediaPicker,
                            useRoundedCorners = useRoundedCorners,
                            selected = selectionManager.isSelected(item),
                            toggleSelection = {
                                selectionManager.toggle(item)
                            },
                            navigateToItem = {
                                if (!isMediaPicker && item is PhotoLibraryUIModel.MediaImpl) {
                                    val index =
                                        pagingItems.itemSnapshotList
                                            .filterIsInstance<PhotoLibraryUIModel.MediaImpl>()
                                            .indexOf(item)

                                    if (openVideosExternally && item.item.type == MediaType.Video) {
                                        val intent = Intent().apply {
                                            data = item.item.uri.toUri() // TODO: immich handling
                                            action = Intent.ACTION_VIEW
                                        }

                                        context.startActivity(intent)
                                    } else {
                                        navController.navigate(viewProperties.navigate(albumInfo, index, null))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (isSelecting && !isMainPage) {
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(80.dp)
                    )
                }
            }

            if (isMainPage) {
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(120.dp)
                    )
                }
            }
        }

        val localDensity = LocalDensity.current
        val spacerHeight = animateDpAsState(
            targetValue =
                with(localDensity) {
                    WindowInsets.navigationBars.getBottom(localDensity).toDp() +
                            if (isMainPage) 128.dp
                            else if (isSelecting) 64.dp
                            else 8.dp
                },
            animationSpec = tween(
                durationMillis = 350,
                delayMillis = if (isSelecting) 350 else 0
            ),
            label = "animate spacer on bottom of photo grid"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight(1f)
                .wrapContentWidth()
        ) {
            FloatingScrollbar(
                gridState = gridState,
                spacerHeight = spacerHeight,
                pagingItems = pagingItems
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MediaStoreItem(
    item: PhotoLibraryUIModel,
    viewProperties: ViewProperties,
    isSelecting: () -> Boolean,
    thumbnailSettings: Pair<Boolean, Int>,
    isDragSelecting: MutableState<Boolean>,
    isMediaPicker: Boolean,
    useRoundedCorners: Boolean,
    selected: Boolean,
    toggleSelection: () -> Unit,
    navigateToItem: () -> Unit
) {
    val vibratorManager = rememberVibratorManager()

    if (item is PhotoLibraryUIModel.Section) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    toggleSelection()

                    vibratorManager.vibrateLong()
                }
                .padding(16.dp, 8.dp),
        ) {
            val resources = LocalResources.current
            Text(
                text = (viewProperties.prefix?.let {
                    ViewProperties.getText(
                        id = viewProperties.prefix,
                        resources = resources
                    ) + " "
                } ?: "") + item.title,
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )

            ShowSelectedState(
                isSelected = selected,
                showIcon = isSelecting(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    } else {
        item as PhotoLibraryUIModel.MediaImpl

        val animatedItemCornerRadius by animateDpAsState(
            targetValue = if (selected) 16.dp else 0.dp,
            animationSpec = tween(
                durationMillis = 150,
            ),
            label = "animate corner radius of selected item"
        )
        val animatedItemScale by animateFloatAsState(
            targetValue = if (selected) 0.8f else 1f,
            animationSpec = tween(
                durationMillis = 150
            ),
            label = "animate scale of selected item"
        )

        val onSingleClick = {
            vibratorManager.vibrateShort()
            if (isSelecting() || isMediaPicker) {
                toggleSelection()
            } else {
                navigateToItem()
            }
        }

        val onLongClick = {
            isDragSelecting.value = true

            vibratorManager.vibrateLong()

            toggleSelection()
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(if (useRoundedCorners) 8.dp else 0.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                .clickable(
                    enabled = !isDragSelecting.value
                ) {
                    if (isMediaPicker) {
                        onLongClick()
                    } else {
                        onSingleClick()
                    }
                }
        ) {
            // TODO: possibly move to a less messy and horrible decrypting implementation
            var model by remember { mutableStateOf<Any?>(null) }
            val context = LocalContext.current
            val isSecureMedia =
                remember(viewProperties) { viewProperties == ViewProperties.SecureFolder }

            LaunchedEffect(isSecureMedia) {
                if (!isSecureMedia || model != null) return@LaunchedEffect

                item as PhotoLibraryUIModel.SecuredMedia

                model =
                    withContext(Dispatchers.IO) {
                        try {
                            val thumbnailIv =
                                item.bytes!!.getThumbnailIv() // get thumbnail iv from video

                            EncryptionManager.decryptBytes(
                                bytes = getSecuredCacheImageForFile(
                                    fileName = item.item.displayName,
                                    context = context
                                ).readBytes(),
                                iv = thumbnailIv
                            )
                        } catch (e: Throwable) {
                            Log.d(TAG, e.toString())
                            e.printStackTrace()

                            item.item.uri.toUri().path
                        }
                    }
            }

            GlideImage(
                model = when {
                    isSecureMedia -> model

                    item.item.immichUrl != null -> ImmichInfo(
                        thumbnail = item.item.immichThumbnail!!,
                        original = item.item.immichUrl!!,
                        hash = item.item.hash!!,
                        accessToken = item.accessToken!!,
                        useThumbnail = true
                    )

                    else -> item.item.uri
                },
                contentDescription = item.item.displayName,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .fillMaxSize(1f)
                    .align(Alignment.Center)
                    .scale(animatedItemScale)
                    .clip(RoundedCornerShape(animatedItemCornerRadius))
            ) {
                if (isSecureMedia) {
                    it.signature(item.signature())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                } else if (thumbnailSettings.second == 0) {
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

            if (item.item.type == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.movie_filled),
                        contentDescription = stringResource(id = R.string.file_is_a_video),
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            if (item.item.isRawImage()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.raw_on),
                        contentDescription = stringResource(id = R.string.media_is_raw),
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            ShowSelectedState(
                isSelected = selected,
                showIcon = isSelecting(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun LoadingMediaStoreItem(
    item: PhotoLibraryUIModel,
    useRoundedCorners: Boolean
) {
    var showColors by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(10) // to avoid a pop-in effect
        showColors = true
    }

    val highlightColor by animateColorAsState(
        targetValue = if (showColors) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(
            durationMillis = 0
        )
    )

    if (item is PhotoLibraryUIModel.Section) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(Color.Transparent)
                .padding(16.dp, 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(Random.nextFloat() * 0.5f + 0.5f)
                    .height(24.dp)
                    .clip(CircleShape)
                    .shimmerEffect(
                        containerColor = Color.Transparent,
                        highlightColor = highlightColor,
                        durationMillis = AnimationConstants.DURATION_EXTRA_LONG * 3,
                        delayMillis = -PhotoGridConstants.UPDATE_TIME.toInt() * 2
                    )
            )
        }
    } else {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(if (useRoundedCorners) 8.dp else 0.dp))
                .shimmerEffect(
                    containerColor = Color.Transparent,
                    highlightColor = highlightColor,
                    durationMillis = AnimationConstants.DURATION_EXTRA_LONG * 2
                )
        )
    }
}

private fun Modifier.dragSelectionHandler(
    state: LazyGridState,
    selectionManager: SelectionManager,
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    gridState: LazyGridState,
    isDragSelecting: MutableState<Boolean>,
    context: Context,
    thumbnailSettings: Pair<Boolean, Int>
) = composed {
    val localDensity = LocalDensity.current
    val resources = LocalResources.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollSpeed = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(scrollSpeed.floatValue) {
        if (scrollSpeed.floatValue != 0f) {
            while (isActive) {
                gridState.scrollBy(scrollSpeed.floatValue)
                delay(10)
            }
        }
    }

    pointerInput(pagingItems) {
        var initialKey: Int? = null
        var currentKey: Int? = null
        var isDragAndDropping = false

        val scrollThreshold = with(localDensity) {
            60.dp.toPx()
        }

        if (pagingItems.itemCount == 0) return@pointerInput

        val itemWidth = state.layoutInfo.visibleItemsInfo.firstOrNull {
            if (it.index in 0..pagingItems.itemCount) {
                pagingItems[it.index] is PhotoLibraryUIModel.MediaImpl
            } else false
        }?.size?.width

        val numberOfHorizontalItems = itemWidth?.let { state.layoutInfo.viewportSize.width / it } ?: 1

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                isDragSelecting.value = true

                state.getGridItemAtOffset(
                    offset = offset,
                    keys = (0..<pagingItems.itemCount).map { pagingItems[it]?.itemKey() },
                    numberOfHorizontalItems = numberOfHorizontalItems
                )?.let { index ->
                    val item = pagingItems[index]

                    if (item is PhotoLibraryUIModel.MediaImpl) {
                        val selected = selectionManager.isSelected(item)

                        if (selected) {
                            isDragAndDropping = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val items = selectionManager.selection.first().fastMap { it.toUri() }

                                val clipData = ClipData.newUri(
                                    context.contentResolver,
                                    resources.getString(R.string.drag_and_drop_data),
                                    items.first()
                                )

                                items.drop(1).forEach {
                                    clipData.addItem(ClipData.Item(it))
                                }

                                val bitmaps = items.take(3).map { // limit to 3 so we don't overstress the rendering/loading of bitmaps
                                    Glide.with(context)
                                        .asBitmap()
                                        .override(thumbnailSettings.second)
                                        .centerCrop()
                                        .load(it)
                                        .submit()
                                        .get()
                                }

                                val shadow = BitmapUriShadowBuilder(
                                    view = view,
                                    bitmaps = bitmaps,
                                    count = items.size,
                                    density = Density(density)
                                )

                                view.startDragAndDrop(
                                    clipData,
                                    shadow,
                                    clipData,
                                    View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE
                                )
                            }
                        } else {
                            isDragAndDropping = false
                            initialKey = index
                            currentKey = index

                            selectionManager.addMedia(item.item)
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
                isDragSelecting.value = true

                if (initialKey != null && !isDragAndDropping) {
                    val distanceFromBottom = state.layoutInfo.viewportSize.height - change.position.y
                    val distanceFromTop = change.position.y // for clarity

                    scrollSpeed.floatValue = when {
                        distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
                        distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
                        else -> 0f
                    }

                    state.getGridItemAtOffset(
                        offset = change.position,
                        keys = (0..<pagingItems.itemCount).map { pagingItems[it]?.itemKey() },
                        numberOfHorizontalItems = numberOfHorizontalItems
                    )?.let { index ->
                        if (currentKey != index) {
                            val toBeRemoved =
                                if (initialKey!! <= currentKey!!) {
                                    (initialKey!!..currentKey!!).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                } else {
                                    (currentKey!!..initialKey!!).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                }

                            val toBeAdded =
                                if (initialKey!! <= index) {
                                    (initialKey!!..index).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                } else {
                                    (index..initialKey!!).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                }

                            selectionManager.updateSelection(added = toBeAdded, removed = toBeRemoved)

                            currentKey = index
                        }
                    }
                }
            }
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> LazyGridState.getGridItemAtOffset(
    offset: Offset,
    keys: List<T>,
    numberOfHorizontalItems: Int
): Int? {
    var key: T? = null

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
            key = possibleItem.key as T
            break
        }
    }

    val found = keys.find {
        it == key
    } ?: return null

    return keys.indexOf(found)
}

