package com.kaii.photos.compose.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.compose.pages.main.MainGridView
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.main_grid.MainGridViewModelFactory
import com.kaii.photos.permissions.StartupManager

@Composable
fun StartupLoadingPage(
    startupManager: StartupManager
) {
    val context = LocalContext.current

    val mainGridViewModel = viewModel<MainGridViewModel>(factory = MainGridViewModelFactory(context = context))
    val mainPhotosAlbums by mainGridViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()

    var album by remember {
        mutableStateOf(
            AlbumType.Folder(
                id = "",
                name = "",
                paths = mainPhotosAlbums,
                pinned = false,
                immichId = null
            )
        )
    }

    LaunchedEffect(mainPhotosAlbums) {
        album = album.copy(paths = mainPhotosAlbums)
    }

    Box {
        Box(
            modifier = Modifier.blur(64.dp)
        ) {
            val items = mainGridViewModel.gridMediaFlow.collectAsLazyPagingItems()

            MainGridView(
                items = items,
                album = { album },
                selectionManager = rememberSelectionManager(paths = { mainPhotosAlbums }),
                isMediaPicker = false,
                columnSize = { 3 },
                openVideosExternally = { false },
                cacheThumbnails = { true },
                thumbnailSize = { 256 },
                useRoundedCorners = { false },
                vibrateOnClick = { false }
            )
        }

        ProcessingPage(startupManager = startupManager)
    }
}