package com.kaii.photos.compose.pages

import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.compose.pages.main.MainPages
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.state.rememberAlbumGridState
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.main_grid.MainGridViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.permissions.StartupManager

@Composable
fun StartupLoadingPage(
    startupManager: StartupManager,
    window: Window
) {
    val context = LocalContext.current

    val mainGridViewModel = viewModel<MainGridViewModel>(
        factory = MainGridViewModelFactory(context = context)
    )
    val mainPhotosAlbums by mainGridViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()

    val multiAlbumViewModel = viewModel<MultiAlbumViewModel>(
        factory = MultiAlbumViewModelFactory(
            context = context,
            albumInfo = AlbumInfo.createPathOnlyAlbum(mainPhotosAlbums)
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(context = context)
    )

    LaunchedEffect(mainPhotosAlbums) {
        multiAlbumViewModel.changePaths(AlbumInfo.createPathOnlyAlbum(mainPhotosAlbums))
    }

    Box {
        val albumGridState = rememberAlbumGridState()
        val deviceAlbums = albumGridState.albums.collectAsStateWithLifecycle()

        MainPages(
            multiAlbumViewModel = multiAlbumViewModel,
            searchViewModel = searchViewModel,
            mainGridViewModel = mainGridViewModel,
            deviceAlbums = deviceAlbums,
            window = window,
            incomingIntent = null,
            blur = true,
            refreshAlbums = albumGridState::refresh
        )

        ProcessingPage(startupManager = startupManager)
    }
}