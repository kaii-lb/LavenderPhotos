package com.kaii.photos.compose.grids

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsGridView(
    gridState: AlbumGridState,
    isMediaPicker: Boolean = false
) {
    val navController = LocalNavController.current
    val mainViewModel = LocalMainViewModel.current

    val originalAlbums by gridState.albums.collectAsStateWithLifecycle()
    var albums by remember { mutableStateOf(originalAlbums) }

    LaunchedEffect(originalAlbums) {
        albums = originalAlbums
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
            progress = pullToRefreshState.distanceFraction.coerceAtMost(1f),
            modifier = Modifier
                .height(with(localDensity) { headerHeight.toDp() })
                .zIndex(1f)
        )

        val coroutineScope = rememberCoroutineScope()
        val columnSize by mainViewModel.albumColumnSize.collectAsStateWithLifecycle()

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

                            mainViewModel.settings.albums.setAlbumSortMode(AlbumSortMode.Custom)
                            mainViewModel.settings.albums.set(albums.fastMap { it.info })
                        }
                    )
                },
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
            if (!isMediaPicker) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "FavAndTrash"
                ) {
                    val migrateFav by mainViewModel.settings.versions.getUpdateFav().collectAsStateWithLifecycle(initialValue = true)

                    CategoryList(
                        navigateToFavourites = {
                            if (!migrateFav) {
                                navController.navigate(Screens.Favourites.GridView)
                            } else {
                                navController.navigate(Screens.Favourites.MigrationPage)
                            }
                        },
                        navigateToTrash = {
                            navController.navigate(Screens.Trash.GridView)
                        }
                    )
                }
            }

            items(
                count = albums.size,
                key = { key ->
                    albums[key].info.id
                },
            ) { index ->
                val album = albums[index]

                AlbumGridItem(
                    album = album,
                    isSelected = selectedItem == album,
                    modifier = Modifier
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
                    navController.navigate(
                        route =
                            if (album.info.isCustomAlbum) Screens.CustomAlbum.GridView(albumInfo = album.info)
                            else Screens.Album.GridView(albumInfo = album.info)
                    )
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AlbumGridItem(
    album: AlbumGridState.Album,
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
            AnimatedContent(
                targetState = album.thumbnail.isNotBlank(),
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = AnimationConstants.DURATION_EXTRA_LONG
                        )
                    ).togetherWith(
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = AnimationConstants.DURATION_EXTRA_LONG
                            )
                        )
                    )
                },
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) { state ->
                if (state) {
                    GlideImage(
                        model = album.thumbnail,
                        contentDescription = album.info.name,
                        contentScale = ContentScale.Crop,
                        failure = placeholder(R.drawable.broken_image),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        it.signature(album.signature)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = " ${album.info.name}",
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (album.info.isCustomAlbum) {
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
    @FloatRange(0.0, 1.0) progress: Float,
    modifier: Modifier = Modifier
) {
    val mainViewModel = LocalMainViewModel.current
    val sortMode by mainViewModel.settings.albums.getAlbumSortMode().collectAsStateWithLifecycle(initialValue = AlbumSortMode.LastModifiedDesc)
    val tabList by mainViewModel.settings.defaultTabs.getTabList()
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
            OutlinedIconButton(
                onClick = {
                    mainViewModel.settings.albums.setAlbumSortMode(sortMode = sortMode.flip())
                },
                enabled = sortMode != AlbumSortMode.Custom
            ) {
                val animatedRotation by animateFloatAsState(
                    targetValue = if (sortMode.isDescending) -90f else 90f,
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
                    mainViewModel.settings.albums.setAlbumSortMode(AlbumSortMode.LastModified.byDirection(sortMode.isDescending))
                },
                colors =
                    if (sortMode == AlbumSortMode.LastModified.byDirection(sortMode.isDescending)) ButtonDefaults.buttonColors()
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
                    mainViewModel.settings.albums.setAlbumSortMode(AlbumSortMode.Alphabetically.byDirection(sortMode.isDescending))
                },
                colors =
                    if (sortMode == AlbumSortMode.Alphabetically.byDirection(sortMode.isDescending)) ButtonDefaults.buttonColors()
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
                    mainViewModel.settings.albums.setAlbumSortMode(AlbumSortMode.Custom)
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
                val authManager = rememberSecureFolderAuthManager()
                OutlinedButton(
                    onClick = {
                        authManager.authenticate()
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
