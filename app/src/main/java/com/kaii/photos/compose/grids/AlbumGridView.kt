package com.kaii.photos.compose.grids

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.FloatRange
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ALBUMS_GRID_VIEW"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsGridView(
    currentView: MutableState<BottomBarTab>
) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect().collectAsStateWithLifecycle(initialValue = true)
    val listOfDirs by if (autoDetectAlbums) {
        mainViewModel.settings.AlbumsList.getAutoDetectedAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        mainViewModel.settings.AlbumsList.getNormalAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    }

    val sortMode by mainViewModel.settings.AlbumsList
        .getAlbumSortMode()
        .collectAsStateWithLifecycle(initialValue = AlbumSortMode.Custom)

    val sortByDescending by mainViewModel.settings.AlbumsList
        .getSortByDescending()
        .collectAsStateWithLifecycle(initialValue = true)

    val albums = remember { mutableStateOf(listOfDirs) }

    val albumsViewModel: AlbumsViewModel = viewModel(
        factory = AlbumsViewModelFactory(
            context = context,
            albums = albums.value
        )
    )

    val albumToThumbnailMapping by albumsViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val cachedAlbumToThumbnailMapping =
        remember { mutableStateListOf<Pair<AlbumInfo, MediaStoreData>>() }

    LaunchedEffect(listOfDirs, sortMode, sortByDescending, albumToThumbnailMapping) {
        withContext(Dispatchers.IO) {
            val newList = mutableListOf<AlbumInfo>()

            // if the albums actually changed, not just the order then refresh
            if (albumsViewModel.albumInfo.toSet() != listOfDirs.toSet()) {
                albumsViewModel.refresh(
                    context = context,
                    albums = listOfDirs
                )
            }

            if (albumToThumbnailMapping.toSet() != cachedAlbumToThumbnailMapping.toSet() && albumToThumbnailMapping.isNotEmpty()) {
                cachedAlbumToThumbnailMapping.addAll(albumToThumbnailMapping.toSet())
                cachedAlbumToThumbnailMapping.retainAll(albumToThumbnailMapping.toSet().toSet())
            }

            val copy = listOfDirs
            when (sortMode) {
                AlbumSortMode.LastModified -> {
                    newList.addAll(
                        if (sortByDescending) {
                            copy.sortedByDescending { album ->
                                cachedAlbumToThumbnailMapping.find {
                                    it.first.id == album.id
                                }?.second?.dateModified
                            }.toMutableList().apply {
                                find { item -> item.mainPath == "DCIM/Camera" }?.let { cameraItem ->
                                    remove(cameraItem)
                                    add(0, cameraItem)
                                }
                            }
                        } else {
                            copy.sortedBy { album ->
                                cachedAlbumToThumbnailMapping.find {
                                    it.first.id == album.id
                                }?.second?.dateModified
                            }.toMutableList().apply {
                                find { item -> item.mainPath == "DCIM/Camera" }?.let { cameraItem ->
                                    remove(cameraItem)
                                    add(0, cameraItem)
                                }
                            }
                        }
                    )
                }

                AlbumSortMode.Alphabetically -> {
                    newList.addAll(
                        if (!sortByDescending) {
                            copy.sortedBy {
                                it.name
                            }
                        } else {
                            copy.sortedByDescending {
                                it.name
                            }
                        }
                    )
                }

                AlbumSortMode.Custom -> {
                    newList.addAll(
                        copy
                    )
                }
            }

            albums.value = newList.distinctBy { it.id }

            Log.d(TAG, "Mapping: $albumToThumbnailMapping")
            Log.d(TAG, "Albums $albums")
            Log.d(TAG, "Dirs: $listOfDirs")

            Log.d(TAG, "sort mode: $sortMode and new list: $newList")
        }
    }

    // update the list to reflect custom order (doesn't matter for other sorting modes)
    LaunchedEffect(albums.value) {
        if (albums.value.isNotEmpty()) mainViewModel.settings.AlbumsList.setAlbumsList(albums.value)
    }

    BackHandler(
        enabled = currentView.value == DefaultTabs.TabTypes.albums && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
    ) {
        currentView.value = DefaultTabs.TabTypes.photos
    }

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp, 0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val localConfig = LocalConfiguration.current
        val localDensity = LocalDensity.current
        var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        LaunchedEffect(localConfig) {
            isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        val lazyGridState = rememberLazyGridState()
        var itemOffset by remember { mutableStateOf(Offset.Zero) }
        var selectedItem: AlbumInfo? by remember { mutableStateOf(null) }

        val pullToRefreshState = rememberPullToRefreshState()
        var lockHeader by remember { mutableStateOf(false) }
        val headerHeight by remember {
            derivedStateOf {
                with(localDensity) {
                    pullToRefreshState.distanceFraction * 56.dp.toPx()
                }
            }
        }

        LaunchedEffect(lazyGridState.isScrollInProgress) {
            if (lazyGridState.isScrollInProgress && lazyGridState.canScrollBackward) lockHeader =
                false
        }

        SortModeHeader(
            sortMode = sortMode,
            currentView = currentView,
            progress = pullToRefreshState.distanceFraction.coerceAtMost(1f),
            modifier = Modifier
                .height(with(localDensity) { headerHeight.toDp() })
                .zIndex(1f)
        )

        val coroutineScope = rememberCoroutineScope()
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(
                if (!isLandscape) {
                    2
                } else {
                    4
                }
            ),
            modifier = Modifier
                .fillMaxSize(1f)
                .pullToRefresh(
                    isRefreshing = lockHeader,
                    state = pullToRefreshState,
                    onRefresh = {
                        lockHeader = true
                    }
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lazyGridState.layoutInfo.visibleItemsInfo
                                .find { item ->
                                    IntRect(
                                        offset = item.offset,
                                        size = item.size
                                    ).contains(offset.round())
                                }?.let { item ->
                                    selectedItem = albums.value[item.index - 1]
                                } ?: run { selectedItem = null }
                        },

                        onDrag = { change, offset ->
                            change.consume()
                            itemOffset += offset

                            val targetItem = lazyGridState.layoutInfo.visibleItemsInfo
                                .find { item ->
                                    IntRect(
                                        offset = item.offset,
                                        size = item.size
                                    ).contains(change.position.round())
                                }

                            val currentLazyItem =
                                lazyGridState.layoutInfo.visibleItemsInfo.find {
                                    it.key == selectedItem?.id
                                }

                            if (targetItem != null && currentLazyItem != null) {
                                val targetItemIndex = albums.value.indexOfFirst { it.id == targetItem.key }
                                val newList = albums.value.toMutableList()
                                newList.remove(selectedItem)
                                newList.add(targetItemIndex, selectedItem!!)

                                itemOffset =
                                    change.position - (targetItem.offset + targetItem.size.center).toOffset()

                                albums.value = newList.distinctBy { it.id }
                                if (sortMode != AlbumSortMode.Custom) mainViewModel.settings.AlbumsList.setAlbumSortMode(
                                    AlbumSortMode.Custom
                                )
                            } else if (currentLazyItem != null) {
                                val startOffset = currentLazyItem.offset.y + itemOffset.y
                                val endOffset =
                                    currentLazyItem.offset.y + currentLazyItem.size.height + itemOffset.y

                                val offsetToTop =
                                    startOffset - lazyGridState.layoutInfo.viewportStartOffset
                                val offsetToBottom =
                                    endOffset - lazyGridState.layoutInfo.viewportEndOffset

                                val scroll = when {
                                    offsetToTop < 0 -> offsetToTop.coerceAtMost(0f)
                                    offsetToBottom > 0 -> offsetToBottom.coerceAtLeast(0f)
                                    else -> 0f
                                }

                                if (scroll != 0f && (lazyGridState.canScrollBackward || lazyGridState.canScrollForward)) coroutineScope.launch {
                                    lazyGridState.scrollBy(scroll)
                                }
                            }
                        },

                        onDragCancel = {
                            selectedItem = null
                            itemOffset = Offset.Zero
                        },

                        onDragEnd = {
                            selectedItem = null
                            itemOffset = Offset.Zero
                        }
                    )
                },
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                CategoryList(
                    navigateToFavourites = {
                        navController.navigate(MultiScreenViewType.FavouritesGridView.name)
                    },
                    navigateToTrash = {
                        navController.navigate(MultiScreenViewType.TrashedPhotoView.name)
                    }
                )
            }

            items(
                count = albums.value.size,
                key = { key ->
                    albums.value[key].id
                },
            ) { index ->
                val neededDir = albums.value[index]
                val mediaItem = cachedAlbumToThumbnailMapping.find {
                    it.first.id == neededDir.id
                }?.second ?: MediaStoreData.dummyItem

                AlbumGridItem(
                    album = neededDir,
                    item = mediaItem,
                    isSelected = selectedItem == neededDir,
                    modifier = Modifier
                        .zIndex(
                            if (selectedItem == neededDir) 1f
                            else 0f
                        )
                        .graphicsLayer {
                            if (selectedItem == neededDir) {
                                translationX = itemOffset.x
                                translationY = itemOffset.y
                            }
                        }
                        .wrapContentSize()
                        .animateItem(
                            fadeInSpec = tween(
                                durationMillis = 250
                            ),
                            fadeOutSpec = tween(
                                durationMillis = 250
                            ),
                            placementSpec =
                                if (selectedItem == neededDir) null // if is selected don't animate so no weird snapping back and forth happens
                                else tween(durationMillis = 250)
                        )
                ) {
                    navController.navigate(
                        Screens.SingleAlbumView(
                            albumInfo = neededDir
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AlbumGridItem(
    album: AlbumInfo,
    item: MediaStoreData,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animate album grid item scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(
            durationMillis = 200
        ),
        label = "animate selected album grid item background color"
    )

    Column(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
            .scale(animatedScale)
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable {
                if (!isSelected) onClick()
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp, 8.dp, 8.dp, 4.dp)
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlideImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brightenColor(
                            MaterialTheme.colorScheme.surfaceContainer,
                            0.1f
                        )
                    ),
            ) {
                it.signature(item.signature())
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = " ${album.name}",
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (album.isCustomAlbum) {
                    Icon(
                        painter = painterResource(id = R.drawable.art_track),
                        contentDescription = stringResource(id = R.string.albums_is_custom),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryList(
    navigateToTrash: () -> Unit,
    navigateToFavourites: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = {
                navigateToFavourites()
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.favourite),
                    contentDescription = stringResource(id = R.string.favourites),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(0.dp, 2.dp, 0.dp, 0.dp)
                )

                Spacer(
                    modifier = Modifier
                        .width(8.dp)
                )

                Text(
                    text = stringResource(id = R.string.favourites),
                    fontSize = TextUnit(16f, TextUnitType.Sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(
            onClick = {
                navigateToTrash()
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.trash),
                    contentDescription = stringResource(id = R.string.trash_bin),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                )

                Text(
                    text = stringResource(id = R.string.trash_bin_short) + " ",
                    fontSize = TextUnit(16f, TextUnitType.Sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                )
            }
        }
    }
}

@Composable
private fun SortModeHeader(
    sortMode: AlbumSortMode,
    currentView: MutableState<BottomBarTab>,
    @FloatRange(0.0, 1.0) progress: Float,
    modifier: Modifier = Modifier
) {
    val tabList by mainViewModel.settings.DefaultTabs.getTabList()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    LazyRow(
        modifier = modifier
            .fillMaxWidth(1f)
            .padding(4.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Start
        )
    ) {
        item {
            val sortByDescending by mainViewModel.settings.AlbumsList.getSortByDescending()
                .collectAsStateWithLifecycle(initialValue = true)

            OutlinedIconButton(
                onClick = {
                    mainViewModel.settings.AlbumsList.setSortByDescending(!sortByDescending)
                },
                enabled = sortMode != AlbumSortMode.Custom
            ) {
                val animatedRotation by animateFloatAsState(
                    targetValue = if (sortByDescending) -90f else 90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "Animate sort order arrow"
                )

                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.sort_indicator),
                    modifier = Modifier
                        .rotate(animatedRotation)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    mainViewModel.settings.AlbumsList.setAlbumSortMode(AlbumSortMode.LastModified)
                },
                colors =
                    if (sortMode == AlbumSortMode.LastModified) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(id = R.string.sort_date),
                    modifier = Modifier
                        .scale(progress)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    mainViewModel.settings.AlbumsList.setAlbumSortMode(AlbumSortMode.Alphabetically)
                },
                colors =
                    if (sortMode == AlbumSortMode.Alphabetically) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(id = R.string.sort_name),
                    modifier = Modifier
                        .scale(progress)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    mainViewModel.settings.AlbumsList.setAlbumSortMode(AlbumSortMode.Custom)
                },
                colors =
                    if (sortMode == AlbumSortMode.Custom) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(id = R.string.sort_custom),
                    modifier = Modifier
                        .scale(progress)
                )
            }
        }

        if (!tabList.contains(DefaultTabs.TabTypes.secure)) {
            item {
                OutlinedButton(
                    onClick = {
                        currentView.value = DefaultTabs.TabTypes.secure
                    },
                    colors =
                        if (sortMode == AlbumSortMode.Custom) ButtonDefaults.buttonColors()
                        else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(
                        text = stringResource(id = R.string.secure_folder),
                        modifier = Modifier
                            .scale(progress)
                    )
                }
            }
        }
    }
}
