package com.kaii.photos.compose.grids

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.FavouritesViewBottomAppBar
import com.kaii.photos.compose.app_bars.FavouritesViewTopAppBar
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

private const val TAG = "FAVOURITES_GRID_VIEW"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesGridView(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val appDatabase = LocalAppDatabase.current
    val favouritesViewModel: FavouritesViewModel = viewModel(
        factory = FavouritesViewModelFactory(appDatabase)
    )

    val mediaStoreData by favouritesViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val mainViewModel = LocalMainViewModel.current

    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val groupedMedia = remember {
        mutableStateOf(
            groupPhotosBy(
                mediaStoreData,
                MediaItemSortMode.LastModified,
                displayDateFormat,
                context
            )
        )
    }

    var hasFiles by remember { mutableStateOf(true) }

    LaunchedEffect(mediaStoreData) {
        if (mediaStoreData.isNotEmpty()) {
            delay(PhotoGridConstants.UPDATE_TIME)
            groupedMedia.value =
                groupPhotosBy(
                    mediaStoreData,
                    MediaItemSortMode.LastModified,
                    displayDateFormat,
                    context
                )
        }

        delay(PhotoGridConstants.LOADING_TIME)
        hasFiles = mediaStoreData.isNotEmpty()

        Log.d(TAG, "Grouped media size: ${groupedMedia.value.size}")
    }

    val navController = LocalNavController.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize(1f)
            .windowInsetsPadding(
                WindowInsets.navigationBars
            ),
        topBar = {
            FavouritesViewTopAppBar(
                selectedItemsList = selectedItemsList
            ) {
                navController.popBackStack()
            }
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
                FavouritesViewBottomAppBar(
                    selectedItemsList = selectedItemsList,
                    groupedMedia = groupedMedia
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Favourites,
                hasFiles = hasFiles
            )
        }
    }
}

