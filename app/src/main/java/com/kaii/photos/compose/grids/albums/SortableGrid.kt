package com.kaii.photos.compose.grids.albums

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.albums.AlbumFolder
import com.kaii.photos.compose.widgets.albums.AlbumGridItem
import com.kaii.photos.compose.widgets.albums.SortModeHeader
import com.kaii.photos.compose.widgets.albums.pinDeleteHeader
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.Screens
import com.kaii.photos.reorderable_lists.rememberSortableGridState

@Preview
@Composable
private fun SortableGridPreview() {
    SortableGrid(
        albumList = {
            (0..10).map { index ->
                AlbumGridState.Album.Single(
                    info = AlbumGridState.Info(
                        album = AlbumType.PlaceHolder,
                        thumbnail = AlbumGridState.Info.Thumbnail(
                            uri = "",
                            signature = ObjectKey(0),
                            albumId = index.toString(),
                            date = 0L,
                            isGif = false
                        )
                    ),
                    id = index.toString(),
                    name = "Test",
                    summary = null,
                    date = 0L,
                    pinned = false
                )
            }
        },
        sortMode = { AlbumSortMode.LastModifiedDesc },
        tabList = { emptyList() },
        columnSize = 2,
        immichInfo = { ImmichBasicInfo.Empty },
        navController = rememberNavController(),
        autoDetect = { false },
        setAlbumSortMode = {},
        setAlbumOrder = {},
        addAlbumToGroup = { _, _ -> },
        authenticateSecureFolder = {},
        toggleAlbumPin = {},
        deleteAlbum = {},
    )
}

@Composable
fun SortableGrid(
    albumList: () -> List<AlbumGridState.Album>,
    sortMode: () -> AlbumSortMode,
    tabList: () -> List<BottomBarTab>,
    columnSize: Int,
    immichInfo: () -> ImmichBasicInfo,
    navController: NavController,
    autoDetect: () -> Boolean,
    modifier: Modifier = Modifier,
    isAlbumGroup: Boolean = false,
    removeAlbumIcon: Int = R.drawable.delete,
    prefix: (LazyGridScope.() -> Unit)? = null,
    suffix: (LazyGridScope.() -> Unit)? = null,
    setAlbumSortMode: (sortMode: AlbumSortMode) -> Unit,
    setAlbumOrder: (order: List<String>) -> Unit,
    addAlbumToGroup: (albumId: String, groupId: String) -> Unit,
    authenticateSecureFolder: () -> Unit,
    toggleAlbumPin: (album: AlbumGridState.Album) -> Unit,
    deleteAlbum: (album: AlbumGridState.Album) -> Unit
) {
    var albums by remember { mutableStateOf(albumList()) }
    LaunchedEffect(albumList()) {
        albums = albumList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val pullToRefreshState = rememberPullToRefreshState()
        var lockHeader by remember { mutableStateOf(false) }

        if (!isAlbumGroup) {
            val density = LocalDensity.current
            val headerHeight by remember {
                derivedStateOf {
                    with(density) {
                        pullToRefreshState.distanceFraction * 56.dp.toPx()
                    }
                }
            }

            SortModeHeader(
                sortMode = sortMode,
                showHiddenSecureEntry = {
                    !tabList().contains(DefaultTabs.TabTypes.secure)
                },
                progress = {
                    pullToRefreshState.distanceFraction.coerceAtMost(1f)
                },
                modifier = Modifier
                    .height(with(density) { headerHeight.toDp() })
                    .zIndex(1f),
                setAlbumSortMode = setAlbumSortMode,
                authenticateSecureFolder = authenticateSecureFolder
            )
        }

        val lazyGridState = rememberLazyGridState()
        LaunchedEffect(lazyGridState.isScrollInProgress) {
            if (lazyGridState.isScrollInProgress && lazyGridState.canScrollBackward) {
                lockHeader = false
            }
        }

        val sortableGridState = rememberSortableGridState(
            gridState = lazyGridState,
            albums = { albums },
            hasPrefix = { prefix != null },
            isAlbumGroup = isAlbumGroup,
            sortMode = sortMode,
            autoDetect = autoDetect,
            setAlbums = { albums = it },
            setAlbumSortMode = setAlbumSortMode,
            setAlbumOrder = setAlbumOrder,
            addAlbumToGroup = addAlbumToGroup,
            toggleAlbumPin = toggleAlbumPin,
            deleteAlbum = deleteAlbum
        )

        val itemScale by animateFloatAsState(
            targetValue = sortableGridState.itemScale,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        )

        val isLandscape by rememberDeviceOrientation()
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
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId = true
                }
                .pullToRefresh(
                    isRefreshing = lockHeader,
                    state = pullToRefreshState,
                    onRefresh = {
                        if (!isAlbumGroup) lockHeader = true
                    },
                    enabled = !isAlbumGroup
                )
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = sortableGridState::onDragStart,

                        onDrag = sortableGridState::onDrag,

                        onDragCancel = sortableGridState::onDragCancel,

                        onDragEnd = sortableGridState::onDragEnd
                    )
                },
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
            if (prefix != null) {
                prefix()
            }

            if (!isAlbumGroup) {
                pinDeleteHeader(
                    sortableGridState = sortableGridState,
                    removeAlbumIcon = removeAlbumIcon
                )
            }

            items(
                count = albums.size,
                key = { key ->
                    albums[key].id
                },
                contentType = { key ->
                    albums[key]::class
                }
            ) { index ->
                val album = albums[index]
                val itemSelected by remember {
                    derivedStateOf {
                        sortableGridState.selectedItem?.id == album.id
                    }
                }

                if (album is AlbumGridState.Album.Group) {
                    AlbumFolder(
                        name = album.name,
                        info = album.info,
                        isSelected = { itemSelected },
                        immichInfo = immichInfo,
                        modifier = Modifier
                            .testTag("album_grid_group_item")
                            .zIndex(
                                if (itemSelected) 1f
                                else 0f
                            )
                            .graphicsLayer {
                                if (itemSelected) {
                                    translationX = sortableGridState.itemOffset.x
                                    translationY = sortableGridState.itemOffset.y

                                    scaleX = itemScale
                                    scaleY = itemScale
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
                                    if (itemSelected) null // if is selected don't animate so no weird snapping back and forth happens
                                    else tween(durationMillis = 250)
                            )
                    ) {
                        navController.navigate(
                            route = Screens.AlbumGroup(
                                id = album.id,
                                name = album.name
                            )
                        )
                    }
                } else {
                    AlbumGridItem(
                        album = album as AlbumGridState.Album.Single,
                        isSelected = { itemSelected },
                        info = immichInfo,
                        modifier = Modifier
                            .testTag("album_grid_item")
                            .zIndex(
                                if (itemSelected) 1f
                                else 0f
                            )
                            .graphicsLayer {
                                if (itemSelected) {
                                    translationX = sortableGridState.itemOffset.x
                                    translationY = sortableGridState.itemOffset.y

                                    scaleX = itemScale
                                    scaleY = itemScale
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
                                    if (itemSelected) null // if is selected don't animate so no weird snapping back and forth happens
                                    else tween(durationMillis = 250)
                            )
                    ) {
                        navController.navigate(
                            route =
                                when (val album = album.info.album) {
                                    is AlbumType.Cloud -> {
                                        Screens.Immich.GridView(album = album)
                                    }

                                    is AlbumType.Custom -> {
                                        Screens.CustomAlbum.GridView(album = album)
                                    }

                                    else -> {
                                        Screens.Album.GridView(album = album as AlbumType.Folder)
                                    }
                                }
                        )
                    }
                }
            }

            if (suffix != null) {
                suffix()
            }
        }
    }
}