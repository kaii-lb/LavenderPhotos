package com.kaii.photos.compose.grids.media

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.widgets.FloatingScrollbar
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaType

@Composable
fun PhotoGrid(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    album: () -> AlbumType,
    modifier: Modifier = Modifier,
    viewProperties: ViewProperties,
    selectionManager: SelectionManager,
    columnSize: () -> Int,
    openVideosExternally: () -> Boolean,
    cacheThumbnails: () -> Boolean,
    thumbnailSize: () -> Int,
    useRoundedCorners: () -> Boolean,
    vibrateOnClick: () -> Boolean,
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
                album = album,
                isMediaPicker = isMediaPicker,
                isMainPage = isMainPage,
                columnSize = columnSize,
                openVideosExternally = openVideosExternally,
                cacheThumbnails = cacheThumbnails,
                thumbnailSize = thumbnailSize,
                useRoundedCorners = useRoundedCorners,
                vibrateOnClick = vibrateOnClick,
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
    album: () -> AlbumType,
    isMediaPicker: Boolean,
    isMainPage: Boolean,
    columnSize: () -> Int,
    openVideosExternally: () -> Boolean,
    cacheThumbnails: () -> Boolean,
    thumbnailSize: () -> Int,
    useRoundedCorners: () -> Boolean,
    vibrateOnClick: () -> Boolean
) {
    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

    BackHandler(
        enabled = isSelecting
    ) {
        selectionManager.clear()
    }

    val isLandscape by rememberDeviceOrientation()

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val context = LocalContext.current
        val resources = LocalResources.current
        val navController = LocalNavController.current
        val vibratorManager = rememberVibratorManager()
        val isDragSelecting = remember { mutableStateOf(false) }

        val prefix = remember(resources) {
            viewProperties.prefix?.let {
                ViewProperties.getText(
                    id = viewProperties.prefix,
                    resources = resources
                ) + " "
            } ?: ""
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(
                if (isLandscape) {
                    columnSize() * 2
                } else {
                    columnSize()
                }
            ),
            userScrollEnabled = !isDragSelecting.value,
            modifier = Modifier
                .semantics {
                    testTagsAsResourceId = true
                }
                .testTag("main_lazy_column")
                .fillMaxSize(1f)
                .align(Alignment.TopCenter)
                .dragSelectionHandler(
                    state = gridState,
                    selectionManager = selectionManager,
                    vibratorManager = vibratorManager.takeIf { vibrateOnClick() },
                    gridState = gridState,
                    pagingItems = pagingItems,
                    isDragSelecting = isDragSelecting,
                    context = context,
                    thumbnailSettings = Pair(cacheThumbnails(), thumbnailSize())
                )
        ) {
            items(
                count = pagingItems.itemCount,
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
                    when (item) {
                        null -> {
                            LoadingItem(
                                item = PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem),
                                useRoundedCorners = useRoundedCorners()
                            )
                        }

                        is PhotoLibraryUIModel.Section -> {
                            SectionItem(
                                item = item,
                                selected = { selectionManager.isSelected(item) },
                                prefix = prefix,
                                isSelecting = { isSelecting },
                                toggleSelection = {
                                    vibratorManager
                                        .takeIf { vibrateOnClick() }
                                        ?.vibrateLong()

                                    selectionManager.toggle(item)
                                }
                            )
                        }

                        is PhotoLibraryUIModel.MediaImpl -> {
                            MediaItem(
                                item = item,
                                isSecureMedia = viewProperties == ViewProperties.SecureFolder,
                                isSelecting = { isSelecting },
                                thumbnailSettings = {
                                    Pair(cacheThumbnails(), thumbnailSize())
                                },
                                isDragSelecting = isDragSelecting,
                                isMediaPicker = isMediaPicker,
                                useRoundedCorners = useRoundedCorners,
                                selected = { selectionManager.isSelected(item) },
                                toggleSelection = {
                                    vibratorManager
                                        .takeIf { vibrateOnClick() }
                                        ?.vibrateShort()

                                    selectionManager.toggle(item)
                                },
                                navigateToItem = {
                                    if (!isMediaPicker) {
                                        vibratorManager
                                            .takeIf { vibrateOnClick() }
                                            ?.vibrateShort()

                                        val index =
                                            pagingItems.itemSnapshotList
                                                .filterIsInstance<PhotoLibraryUIModel.MediaImpl>()
                                                .indexOf(item)

                                        if (openVideosExternally() && item.item.type == MediaType.Video) {
                                            val intent = Intent().apply {
                                                data = item.item.uri.toUri() // TODO: immich handling
                                                action = Intent.ACTION_VIEW
                                            }

                                            context.startActivity(intent)
                                        } else {
                                            navController.navigate(viewProperties.navigate(album(), index))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (isSelecting || isMainPage) {
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(128.dp)
                    )
                }
            }
        }

        val localDensity = LocalDensity.current
        val spacerHeight = animateDpAsState(
            targetValue =
                with(localDensity) {
                    WindowInsets.navigationBars.getBottom(localDensity).toDp() +
                            if (isMainPage || isSelecting) 128.dp
                            else 64.dp
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