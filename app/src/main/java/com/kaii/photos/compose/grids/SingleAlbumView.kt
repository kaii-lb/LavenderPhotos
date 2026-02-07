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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.delay

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    viewModel: MultiAlbumViewModel,
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

    LaunchedEffect(dynamicAlbum) {
        viewModel.update(album = dynamicAlbum)
    }

    val pagingItems = viewModel.gridMediaFlow.collectAsLazyPagingItems()
    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = dynamicAlbum,
        selectedItemsList = selectedItemsList,
        navController = navController,
        incomingIntent = incomingIntent,
        onBackClick = {
            navController.popBackStack()
        }
    )
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
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
    SingleAlbumViewCommon(
        pagingItems = pagingItems,
        albumInfo = dynamicAlbum,
        selectedItemsList = selectedItemsList,
        navController = navController,
        incomingIntent = incomingIntent,
        onBackClick = {
            navController.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    navController: NavHostController,
    incomingIntent: Intent?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    var hasFiles by remember { mutableStateOf(true) }
    LaunchedEffect(pagingItems.loadState) {
        delay(PhotoGridConstants.LOADING_TIME)
        hasFiles = !pagingItems.loadState.source.append.endOfPaginationReached || pagingItems.itemCount != 0
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                pagingItems = pagingItems,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog,
                isMediaPicker = incomingIntent != null,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedItemsList.isNotEmpty(),
                enter = fadeIn() + slideInHorizontally(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = fadeOut() + slideOutHorizontally(
                    animationSpec = AnimationConstants.expressiveTween()
                )
            ) {
                SingleAlbumViewBottomBar(
                    albumInfo = albumInfo,
                    selectedItemsList = selectedItemsList,
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
                selectedItemsList = selectedItemsList,
                viewProperties = if (albumInfo.isCustomAlbum) ViewProperties.CustomAlbum else ViewProperties.Album,
                isMediaPicker = incomingIntent != null,
                hasFiles = hasFiles
            )

            SingleAlbumDialog(
                showDialog = showDialog,
                albumId = albumInfo.id,
                navController = navController,
                selectedItemsList = selectedItemsList,
                itemCount = pagingItems.itemCount // TODO
            )
        }
    }
}


