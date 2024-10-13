package com.kaii.photos.compose.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.TrashedPhotoGridViewTopBar
import com.kaii.photos.compose.TrashedPhotoViewBottomBar
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getAppTrashBinDirectory
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import kotlinx.coroutines.Dispatchers

@Composable
fun TrashedPhotoGridView(
	navController: NavHostController,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
    val mainViewModel = MainActivity.mainViewModel
    
	val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(LocalContext.current, getAppTrashBinDirectory().replace("/storage/emulated/0/", ""), MediaItemSortMode.LastModified)
	)
//	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

	val mediaStoreData = galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

	var groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
	mainViewModel.setGroupedMedia(groupedMedia.value)

	LaunchedEffect(mediaStoreData.value) {
		groupedMedia.value = mediaStoreData.value
		mainViewModel.setGroupedMedia(mediaStoreData.value)
	}
	
    Scaffold (
        topBar =  { TrashedPhotoGridViewTopBar(navController = navController, selectedItemsList = selectedItemsList) },
        bottomBar = {
            TrashedPhotoViewBottomBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        },
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f),
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
                emptyText = "Deleted items show up here",
                prefix = "Deleted On "
            )
        }
    }
}
