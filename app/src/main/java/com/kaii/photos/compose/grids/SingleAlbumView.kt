package com.kaii.photos.compose.grids

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.SingleAlbumViewBottomBar
import com.kaii.photos.compose.SingleAlbumViewTopBar
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.dialogs.SingleAlbumDialog
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumView(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    viewModel: MultiAlbumViewModel
) {
    val navController = LocalNavController.current
    BackHandler(
        enabled = selectedItemsList.size == 0
    ) {
        viewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val mediaStoreData by viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val albums by mainViewModel.settings.AlbumsList.get().collectAsStateWithLifecycle(initialValue = emptyList())

    val groupedMedia = remember { mutableStateOf(mediaStoreData) }

    LaunchedEffect(mediaStoreData) {
        groupedMedia.value = mediaStoreData
    }

    val showDialog = remember { mutableStateOf(false) }
    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.size > 0
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

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

	val albumInfo by remember { derivedStateOf {
		albums.find {
        	it.mainPath == viewModel.albums.first()
       	}
	}}

	val context = LocalContext.current
	LaunchedEffect(albumInfo) {
		if (albumInfo == null) return@LaunchedEffect

		if (viewModel.albums.toSet() != albumInfo!!.paths.toSet()) {
			viewModel.reinitDataSource(
				context = context,
				albumsList = albumInfo!!.paths
			)
		}
	}

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                // dir = viewModel.albums.first(), // TODO: make it handle multiple dirs
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog,
                currentView = currentView
            ) {
                navController.popBackStack()
            }
        },
        sheetContent = {
            SingleAlbumViewBottomBar(
                selectedItemsList = selectedItemsList
            )
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albums = albumInfo?.paths ?: viewModel.albums,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Album,
                shouldPadUp = true
            )

            SingleAlbumDialog(showDialog, viewModel.albums.first(), navController, selectedItemsList) // TODO: make it handle multiple albums
        }
    }
}


