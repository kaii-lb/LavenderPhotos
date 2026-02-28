package com.kaii.photos.compose.grids

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SingleAlbumViewBottomBar
import com.kaii.photos.compose.app_bars.SingleAlbumViewTopBar
import com.kaii.photos.compose.dialogs.AlbumInfoDialog
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.rememberCustomSelectionManager
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    viewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    val allAlbums by viewModel.albums.collectAsStateWithLifecycle()

    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.first { it.id == albumInfo.id }
        }
    }

    LaunchedEffect(dynamicAlbum) {
        viewModel.changePaths(album = dynamicAlbum)
    }

    val selectionManager = rememberSelectionManager(paths = dynamicAlbum.paths)

    val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
    val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
    val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val preserveDate by viewModel.preserveDate.collectAsStateWithLifecycle()

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = { dynamicAlbum },
        selectionManager = selectionManager,
        incomingIntent = incomingIntent,
        viewProperties = ViewProperties.Album,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        preserveDate = preserveDate,
        mediaCount = viewModel::getMediaCount,
        albumSize = viewModel::getMediaSize
    )
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    viewModel: CustomAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val allAlbums by viewModel.albums.collectAsStateWithLifecycle()

    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.first { it.id == albumInfo.id }
        }
    }

    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberCustomSelectionManager(albumId = albumInfo.id)

    val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
    val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
    val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val preserveDate by viewModel.preserveDate.collectAsStateWithLifecycle()

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = { dynamicAlbum },
        selectionManager = selectionManager,
        incomingIntent = incomingIntent,
        viewProperties = ViewProperties.CustomAlbum,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        preserveDate = preserveDate,
        mediaCount = viewModel::getMediaCount,
        albumSize = viewModel::getMediaSize
    )
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    viewModel: ImmichAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val allAlbums by viewModel.albums.collectAsStateWithLifecycle()

    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.first { it.id == albumInfo.id }
        }
    }

    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberCustomSelectionManager(albumId = albumInfo.id)

    val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
    val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
    val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
    val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val confirmToDelete by viewModel.confirmToDelete.collectAsStateWithLifecycle()
    val doNotTrash by viewModel.doNotTrash.collectAsStateWithLifecycle()
    val preserveDate by viewModel.preserveDate.collectAsStateWithLifecycle()

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = { dynamicAlbum },
        selectionManager = selectionManager,
        incomingIntent = incomingIntent,
        viewProperties = ViewProperties.Immich,
        columnSize = columnSize,
        openVideosExternally = openVideosExternally,
        cacheThumbnails = cacheThumbnails,
        thumbnailSize = thumbnailSize,
        useRoundedCorners = useRoundedCorners,
        confirmToDelete = confirmToDelete,
        doNotTrash = doNotTrash,
        preserveDate = preserveDate,
        mediaCount = viewModel::getMediaCount,
        albumSize = viewModel::getMediaSize
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    albumInfo: () -> AlbumInfo,
    selectionManager: SelectionManager,
    incomingIntent: Intent?,
    viewProperties: ViewProperties,
    columnSize: Int,
    openVideosExternally: Boolean,
    cacheThumbnails: Boolean,
    thumbnailSize: Int,
    useRoundedCorners: Boolean,
    confirmToDelete: Boolean,
    doNotTrash: Boolean,
    preserveDate: Boolean,
    modifier: Modifier = Modifier,
    mediaCount: suspend () -> Int,
    albumSize: suspend () -> String
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        val vibratorManager = rememberVibratorManager()
        AlbumInfoDialog(
            albumInfo = albumInfo,
            sheetState = sheetState,
            itemCount = mediaCount,
            albumSize = albumSize,
            toggleSelectionMode = {
                vibratorManager.vibrateShort()
                selectionManager.enterSelectMode()
                coroutineScope.launch {
                    sheetState.hide()
                    showInfoDialog = false
                }
            },
            dismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showInfoDialog = false
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                selectionManager = selectionManager,
                isMediaPicker = incomingIntent != null,
                showDialog = {
                    coroutineScope.launch {
                        showInfoDialog = true
                        delay(50)
                        sheetState.expand()
                    }
                }
            )
        },
        bottomBar = {
            val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

            AnimatedVisibility(
                visible = isSelecting,
                enter = fadeIn() + slideInHorizontally(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = fadeOut() + slideOutHorizontally(
                    animationSpec = AnimationConstants.expressiveTween()
                )
            ) {
                SingleAlbumViewBottomBar(
                    albumInfo = albumInfo,
                    selectionManager = selectionManager,
                    incomingIntent = incomingIntent,
                    confirmToDelete = confirmToDelete,
                    doNotTrash = doNotTrash,
                    preserveDate = preserveDate
                )
            }
        }
    ) { padding ->
        val isLandscape by rememberDeviceOrientation()
        val safeDrawingPadding = if (isLandscape) {
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

            val layoutDirection = LocalLayoutDirection.current
            val left = safeDrawing.calculateStartPadding(layoutDirection)
            val right = safeDrawing.calculateEndPadding(layoutDirection)

            Pair(left, right)
        } else {
            Pair(0.dp, 0.dp)
        }

        Column(
            modifier = Modifier
                .padding(
                    start = safeDrawingPadding.first,
                    top = padding.calculateTopPadding(),
                    end = safeDrawingPadding.second,
                    bottom = 0.dp
                )
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                pagingItems = pagingItems,
                albumInfo = albumInfo(),
                selectionManager = selectionManager,
                viewProperties = viewProperties,
                isMediaPicker = incomingIntent != null,
                columnSize = columnSize,
                openVideosExternally = openVideosExternally,
                cacheThumbnails = cacheThumbnails,
                thumbnailSize = thumbnailSize,
                useRoundedCorners = useRoundedCorners,
            )
        }
    }
}


