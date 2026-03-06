package com.kaii.photos.compose.pages.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.widgets.albums.AlbumFolder
import com.kaii.photos.compose.widgets.albums.AlbumGridItem
import com.kaii.photos.compose.widgets.albums.CategoryList
import com.kaii.photos.compose.widgets.albums.SortModeHeader
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsGridView(
    deviceAlbums: State<List<AlbumGridState.Album>>,
    sortMode: AlbumSortMode,
    tabList: List<BottomBarTab>,
    columnSize: Int,
    immichInfo: ImmichBasicInfo,
    migrateFav: () -> Boolean,
    isMediaPicker: Boolean = false,
    setAlbumSortMode: (sortMode: AlbumSortMode) -> Unit,
    setAlbumOrder: (order: List<String>) -> Unit,
    addAlbumToGroup: (albumId: String, groupId: String) -> Unit
) {
    val navController = LocalNavController.current
    var albums by remember { mutableStateOf(deviceAlbums.value) }

    LaunchedEffect(deviceAlbums.value) {
        albums = deviceAlbums.value
    }

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp, 0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val density = LocalDensity.current
        val isLandscape by rememberDeviceOrientation()

        val lazyGridState = rememberLazyGridState()
        var itemOffset by remember { mutableStateOf(Offset.Zero) }
        var selectedItem by remember { mutableStateOf<AlbumGridState.Album?>(null) }
        var selectedIsGrouping by remember { mutableStateOf(false) }
        val addingToGroupScale by animateFloatAsState(
            targetValue = if (selectedIsGrouping) 0.8f else 1f,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        )

        val pullToRefreshState = rememberPullToRefreshState()
        var lockHeader by remember { mutableStateOf(false) }
        val headerHeight by remember {
            derivedStateOf {
                with(density) {
                    pullToRefreshState.distanceFraction * 56.dp.toPx()
                }
            }
        }

        SortModeHeader(
            sortMode = sortMode,
            tabList = tabList,
            progress = pullToRefreshState.distanceFraction.coerceAtMost(1f),
            setAlbumSortMode = setAlbumSortMode,
            modifier = Modifier
                .height(with(density) { headerHeight.toDp() })
                .zIndex(1f)
        )

        LaunchedEffect(lazyGridState.isScrollInProgress) {
            if (lazyGridState.isScrollInProgress && lazyGridState.canScrollBackward) {
                lockHeader = false
            }
        }

        val scrollSpeed = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(scrollSpeed.floatValue) {
            if (scrollSpeed.floatValue != 0f) {
                while (isActive) {
                    lazyGridState.scrollBy(scrollSpeed.floatValue)
                    delay(10)
                }
            }
        }

        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(
                if (!isLandscape) {
                    columnSize
                } else {
                    columnSize * 2
                }
            ),
            modifier = Modifier
                .fillMaxSize(1f)
                .semantics {
                    testTagsAsResourceId = true
                }
                .pullToRefresh(
                    isRefreshing = lockHeader,
                    state = pullToRefreshState,
                    onRefresh = {
                        lockHeader = true
                    }
                )
                .pointerInput(Unit) {
                    var targetItemIndex: Int? = null
                    var lastSortMode = sortMode
                    val scrollThreshold = with(density) {
                        60.dp.toPx()
                    }

                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            lastSortMode = sortMode
                            lazyGridState.layoutInfo.visibleItemsInfo
                                .find { item ->
                                    IntRect(
                                        offset = item.offset,
                                        size = item.size
                                    ).contains(offset.round()) && !item.key.toString().startsWith("FavAndTrash")
                                }?.let { item ->
                                    selectedItem = albums[item.index - 1]
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

                            val distanceFromBottom = lazyGridState.layoutInfo.viewportSize.height - change.position.y
                            val distanceFromTop = change.position.y // for clarity

                            scrollSpeed.floatValue = when {
                                distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
                                distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
                                else -> 0f
                            }

                            if (targetItem != null && currentLazyItem != null && targetItem.key in albums.map { it.id }) {
                                targetItemIndex = albums.indexOfFirst { it.id == targetItem.key }
                                val newList = albums.toMutableList()

                                if (albums[targetItemIndex] is AlbumGridState.Album.Group && selectedItem is AlbumGridState.Album.Single) {
                                    selectedIsGrouping = true
                                } else {
                                    selectedIsGrouping = false
                                    newList.removeAll { it.id == selectedItem?.id }
                                    newList.add(targetItemIndex, selectedItem!!)

                                    itemOffset =
                                        change.position - (targetItem.offset + targetItem.size.center).toOffset()

                                    albums = newList
                                }
                            }
                        },

                        onDragCancel = {
                            selectedItem = null
                            itemOffset = Offset.Zero
                            scrollSpeed.floatValue = 0f
                        },

                        onDragEnd = {
                            if (targetItemIndex != null &&
                                albums[targetItemIndex] is AlbumGridState.Album.Group
                                && selectedItem is AlbumGridState.Album.Single
                            ) {
                                val targetItem = albums[targetItemIndex]

                                addAlbumToGroup(
                                    selectedItem!!.id,
                                    targetItem.id
                                )

                                albums = albums.toMutableList().filter { it.id != selectedItem?.id }
                                setAlbumSortMode(lastSortMode)
                            } else {
                                setAlbumSortMode(AlbumSortMode.Custom)
                                setAlbumOrder(albums.map { it.id })
                            }

                            selectedItem = null
                            itemOffset = Offset.Zero
                            scrollSpeed.floatValue = 0f
                        }
                    )
                },
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "FavAndTrash"
            ) {
                CategoryList(
                    navigateToFavourites = {
                        navController.navigate(
                            if (migrateFav() && !isMediaPicker) Screens.Favourites.MigrationPage // TODO: handle media picker case
                            else Screens.Favourites.GridView
                        )
                    },
                    navigateToTrash = {
                        navController.navigate(Screens.Trash.GridView)
                    }
                )
            }

            items(
                count = albums.size,
                key = { key ->
                    albums[key].id
                },
            ) { index ->
                val album = albums[index]

                if (album is AlbumGridState.Album.Group) {
                    AlbumFolder(
                        name = album.name,
                        info = album.info,
                        isSelected = selectedItem == album,
                        immichInfo = immichInfo,
                        modifier = Modifier
                            .testTag("album_grid_group_item")
                            .zIndex(
                                if (selectedItem == album) 1f
                                else 0f
                            )
                            .graphicsLayer {
                                if (selectedItem == album) {
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
                                    if (selectedItem == album) null // if is selected don't animate so no weird snapping back and forth happens
                                    else tween(durationMillis = 250)
                            )
                    ) {

                    }
                } else {
                    AlbumGridItem(
                        album = album as AlbumGridState.Album.Single,
                        isSelected = selectedItem == album,
                        info = immichInfo,
                        modifier = Modifier
                            .testTag("album_grid_item")
                            .zIndex(
                                if (selectedItem == album) 1f
                                else 0f
                            )
                            .graphicsLayer {
                                if (selectedItem == album) {
                                    translationX = itemOffset.x
                                    translationY = itemOffset.y

                                    scaleX = addingToGroupScale
                                    scaleY = addingToGroupScale
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
                                    if (selectedItem == album) null // if is selected don't animate so no weird snapping back and forth happens
                                    else tween(durationMillis = 250)
                            )
                    ) {
                        navController.navigate(
                            route =
                                when {
                                    album.info.album is AlbumType.Cloud -> {
                                        Screens.Immich.GridView(album = album.info.album)
                                    }

                                    album.info.album is AlbumType.Custom -> {
                                        Screens.CustomAlbum.GridView(album = album.info.album)
                                    }

                                    else -> {
                                        Screens.Album.GridView(album = album.info.album as AlbumType.Folder)
                                    }
                                }
                        )
                    }
                }
            }

            // padding for floating bottom nav bar
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
}

