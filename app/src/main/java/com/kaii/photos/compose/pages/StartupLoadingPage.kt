package com.kaii.photos.compose.pages

import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.compose.pages.main.MainPages
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.rememberAlbumGridState
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
    val mainViewModel = LocalMainViewModel.current
    val immichInfo by mainViewModel.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)
    val mainPhotosPaths by mainViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()
    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

    val multiAlbumViewModel = viewModel<MultiAlbumViewModel>(
        factory = MultiAlbumViewModelFactory(
            context = context,
            albumInfo = AlbumInfo.createPathOnlyAlbum(mainPhotosPaths),
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat
        )
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            context = context,
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat
        )
    )

    Box {
        val deviceAlbums = rememberAlbumGridState().albums.collectAsStateWithLifecycle()

        MainPages(
            mainPhotosPaths = mainPhotosPaths,
            multiAlbumViewModel = multiAlbumViewModel,
            searchViewModel = searchViewModel,
            deviceAlbums = deviceAlbums,
            window = window,
            incomingIntent = null,
            blur = true
        )

        ProcessingPage(startupManager = startupManager)
    }
}