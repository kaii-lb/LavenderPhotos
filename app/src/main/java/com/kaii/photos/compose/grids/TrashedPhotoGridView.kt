package com.kaii.photos.compose.grids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.IsSelectingBottomAppBar
import com.kaii.photos.compose.TrashedPhotoGridViewTopBar
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getAppTrashBinDirectory
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridView(
	navController: NavHostController,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
    val mainViewModel = MainActivity.mainViewModel

    val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(
            LocalContext.current,
            getAppTrashBinDirectory().replace("/storage/emulated/0/", ""),
            MediaItemSortMode.LastModified
        )
	)
//	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

	val mediaStoreData = galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

	val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
	mainViewModel.setGroupedMedia(groupedMedia.value)

	LaunchedEffect(mediaStoreData.value) {
		groupedMedia.value = mediaStoreData.value
		mainViewModel.setGroupedMedia(mediaStoreData.value)
	}

    BackHandler (
        enabled = selectedItemsList.size > 0
    ) {
        selectedItemsList.clear()
    }

    val showBottomSheet by remember { derivedStateOf {
        selectedItemsList.size > 0
    }}

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = SheetValue.Hidden,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
    LaunchedEffect(key1 = showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }
    

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            TrashedPhotoGridViewTopBar(navController = navController, selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        },
        sheetContent = {
            IsSelectingBottomAppBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f)
                .windowInsetsPadding(
                	WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        	// TODO: check for items older than 30 days and delete them
            PhotoGrid(
            	groupedMedia = groupedMedia,
                navController = navController,
                operation = ImageFunctions.LoadTrashedImage,
                path = getAppTrashBinDirectory().replace("/storage/emulated/0/", ""),
                selectedItemsList = selectedItemsList,
                emptyText = "Trashed items show up here",
                prefix = "Trashed On ",
                shouldPadUp = true
            )
        }
    }
}
