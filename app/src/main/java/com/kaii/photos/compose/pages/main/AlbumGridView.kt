package com.kaii.photos.compose.pages.main

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.util.fastMap
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
import kotlinx.coroutines.launch

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
    setAlbums: (albums: List<AlbumType>) -> Unit
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
        val localDensity = LocalDensity.current
        val isLandscape by rememberDeviceOrientation()

        val lazyGridState = rememberLazyGridState()
        var itemOffset by remember { mutableStateOf(Offset.Zero) }
        var selectedItem by remember { mutableStateOf<AlbumGridState.Album?>(null) }

        val pullToRefreshState = rememberPullToRefreshState()
        var lockHeader by remember { mutableStateOf(false) }
        val headerHeight by remember {
            derivedStateOf {
                with(localDensity) {
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
                .height(with(localDensity) { headerHeight.toDp() })
                .zIndex(1f)
        )

        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(lazyGridState.isScrollInProgress) {
            if (lazyGridState.isScrollInProgress && lazyGridState.canScrollBackward) {
                lockHeader = false
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
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
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
                                    it.key == selectedItem?.info?.id
                                }

                            if (targetItem != null && currentLazyItem != null && targetItem.key in albums.map { it.info.id }) {
                                val targetItemIndex =
                                    albums.indexOfFirst { it.info.id == targetItem.key }
                                val newList = albums.toMutableList()
                                newList.removeAll { it.info.id == selectedItem?.info?.id }
                                newList.add(targetItemIndex, selectedItem!!)

                                itemOffset =
                                    change.position - (targetItem.offset + targetItem.size.center).toOffset()

                                albums = newList
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

                            setAlbumSortMode(AlbumSortMode.Custom)
                            setAlbums(albums.fastMap { it.info })
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
                    albums[key].info.id
                },
            ) { index ->
                val album = albums[index]

                if (album.info is AlbumType.AlbumGroup) {
                    AlbumFolder(
                        name = album.info.name,
                        albums = album.info.albums.fastMap { info ->
                            AlbumGridState.Album(
                                info = info,
                                thumbnails = album.thumbnails.filter { it.id == info.id },
                                date = album.date
                            )
                        },
                        isSelected = selectedItem == album,
                        info = immichInfo
                    ) {

                    }
                } else {
                    AlbumGridItem(
                        album = album,
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
                        album.info as AlbumType.Album

                        navController.navigate(
                            route =
                                when {
                                    album.info.custom && album.info.immichId.isNotBlank() -> {
                                        Screens.Immich.GridView(album = album.info)
                                    }

                                    album.info.custom -> {
                                        Screens.CustomAlbum.GridView(album = album.info)
                                    }

                                    else -> {
                                        Screens.Album.GridView(album = album.info)
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

