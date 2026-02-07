package com.kaii.photos.compose.immich

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SingleAlbumViewTopBar
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ImmichAlbumPage(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    modifier: Modifier = Modifier,
    viewModel: ImmichAlbumViewModel
) {
    val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            val navController = LocalNavController.current
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                pagingItems = items,
                selectedItemsList = selectedItemsList,
                showDialog = remember { mutableStateOf(false) },
                isMediaPicker = false, // TODO:
                onBackClick = {
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            // TODO:
            // AnimatedVisibility(
            //     visible = selectedItemsList.isNotEmpty(),
            //     enter = fadeIn() + slideInHorizontally(
            //         animationSpec = AnimationConstants.expressiveSpring()
            //     ),
            //     exit = fadeOut() + slideOutHorizontally(
            //         animationSpec = AnimationConstants.expressiveTween()
            //     )
            // ) {
            //
            // }
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
                pagingItems = items,
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Immich,
                isMediaPicker = false, // TODO:
            )
        }
    }
}