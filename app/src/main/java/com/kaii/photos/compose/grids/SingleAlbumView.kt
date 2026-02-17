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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SingleAlbumViewBottomBar
import com.kaii.photos.compose.app_bars.SingleAlbumViewTopBar
import com.kaii.photos.compose.dialogs.SingleAlbumDialog
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.grid_management.rememberCustomSelectionManager
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    viewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    val navController = LocalNavController.current
    val mainViewModel = LocalMainViewModel.current
    val allAlbums by mainViewModel.allAvailableAlbums.collectAsStateWithLifecycle()

    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.first { it.id == albumInfo.id }
        }
    }

    LaunchedEffect(dynamicAlbum) {
        viewModel.update(album = dynamicAlbum)
    }

    val selectionManager = rememberSelectionManager(paths = dynamicAlbum.paths)
    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = dynamicAlbum,
        selectionManager = selectionManager,
        navController = navController,
        incomingIntent = incomingIntent,
        onBackClick = {
            navController.popBackStack()
        },
        mediaCount = viewModel::getMediaCount
    )
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    viewModel: CustomAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val navController = LocalNavController.current
    val mainViewModel = LocalMainViewModel.current

    val allAlbums by mainViewModel.allAvailableAlbums.collectAsStateWithLifecycle()

    val dynamicAlbum by remember {
        derivedStateOf {
            allAlbums.first { it.id == albumInfo.id }
        }
    }

    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    val selectionManager = rememberCustomSelectionManager(albumId = albumInfo.id)

    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = dynamicAlbum,
        selectionManager = selectionManager,
        navController = navController,
        incomingIntent = incomingIntent,
        onBackClick = {
            navController.popBackStack()
        },
        mediaCount = viewModel::getMediaCount
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    albumInfo: AlbumInfo,
    selectionManager: SelectionManager,
    navController: NavHostController,
    incomingIntent: Intent?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    mediaCount: suspend () -> Int
) {
    val showDialog = remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                selectionManager = selectionManager,
                showDialog = showDialog,
                isMediaPicker = incomingIntent != null,
                onBackClick = onBackClick
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
                    incomingIntent = incomingIntent
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
                albumInfo = albumInfo,
                selectionManager = selectionManager,
                viewProperties = if (albumInfo.isCustomAlbum) ViewProperties.CustomAlbum else ViewProperties.Album,
                isMediaPicker = incomingIntent != null
            )

            SingleAlbumDialog(
                showDialog = showDialog,
                albumId = albumInfo.id,
                navController = navController,
                selectionManager = selectionManager,
                itemCount = mediaCount
            )
        }
    }
}


