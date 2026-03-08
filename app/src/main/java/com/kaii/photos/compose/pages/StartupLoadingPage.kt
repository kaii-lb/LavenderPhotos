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
import com.kaii.photos.compose.pages.main.MainGridView
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.rememberSelectionManager
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.main_grid.MainGridViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.permissions.StartupManager

@Composable
fun StartupLoadingPage(
    startupManager: StartupManager
) {
    val context = LocalContext.current

    val mainGridViewModel = viewModel<MainGridViewModel>(
        factory = MainGridViewModelFactory(context = context)
    )
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

    val multiAlbumViewModel = viewModel<MultiAlbumViewModel>(
        factory = MultiAlbumViewModelFactory(
            context = context,
            album = album
        )
    )

    LaunchedEffect(mainPhotosAlbums) {
        album = album.copy(paths = mainPhotosAlbums)
        multiAlbumViewModel.changePaths(album = album)
    }

    Box {
        MainGridView(
            viewModel = multiAlbumViewModel,
            album = album,
            selectionManager = rememberSelectionManager(paths = { mainPhotosAlbums }),
            isMediaPicker = false,
            columnSize = 3,
            openVideosExternally = false,
            cacheThumbnails = true,
            thumbnailSize = 256,
            useRoundedCorners = false,
            modifier = Modifier.blur(64.dp)
        )

        ProcessingPage(startupManager = startupManager)
    }
}