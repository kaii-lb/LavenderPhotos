package com.kaii.photos.compose.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.FavouritesViewBottomAppBar
import com.kaii.photos.compose.app_bars.FavouritesViewTopAppBar
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesGridView(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
) {
    val appDatabase = LocalAppDatabase.current
    val favouritesViewModel: FavouritesViewModel = viewModel(
        factory = FavouritesViewModelFactory(appDatabase)
    )

    val mediaStoreData =
        favouritesViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val mainViewModel = LocalMainViewModel.current

    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val groupedMedia = remember {
        mutableStateOf(
            groupPhotosBy(
                mediaStoreData.value,
                MediaItemSortMode.LastModified,
                displayDateFormat
            )
        )
    }

    var hasFiles by remember { mutableStateOf(true) }
    val media = remember { mutableStateOf(emptyList<MediaStoreData>())}

    LaunchedEffect(groupedMedia.value.size) {
        if (groupedMedia.value.isNotEmpty()) {
            delay(PhotoGridConstants.UPDATE_TIME)
            media.value = groupedMedia.value
        }

        delay(PhotoGridConstants.LOADING_TIME)
        hasFiles = media.value.isNotEmpty()
    }

    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = SheetValue.Hidden,
    )

    LaunchedEffect(key1 = showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

    val navController = LocalNavController.current
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        modifier = Modifier
            .fillMaxSize(1f)
            .windowInsetsPadding(
                WindowInsets.navigationBars
            ),
        topBar = {
            FavouritesViewTopAppBar(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            ) {
                navController.popBackStack()
            }
        },
        sheetContent = {
            FavouritesViewBottomAppBar(
                selectedItemsList = selectedItemsList,
                groupedMedia = groupedMedia
            )
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = media,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Favourites,
                shouldPadUp = true,
                hasFiles = hasFiles
            )
        }
    }
}

