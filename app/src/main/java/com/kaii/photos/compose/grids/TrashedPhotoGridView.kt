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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.TrashedPhotoGridViewTopBar
import com.kaii.photos.compose.TrashedPhotoViewBottomBar
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getAppTrashBinDirectory
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.mediastore.MediaStoreData

@Composable
fun TrashedPhotoGridView(
	navController: NavHostController,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
    Scaffold (
        topBar =  { TrashedPhotoGridViewTopBar(navController = navController, selectedItemsList = selectedItemsList) },
        bottomBar = {
            TrashedPhotoViewBottomBar(selectedItemsList = selectedItemsList)
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
                navController = navController,
                operation = ImageFunctions.LoadTrashedImage,
                path = getAppTrashBinDirectory().replace("/storage/emulated/0/", ""),
                sortBy = MediaItemSortMode.LastModified,
                selectedItemsList = selectedItemsList,
                emptyText = "Deleted items show up here",
                prefix = "Deleted On "
            )
        }
    }
}
