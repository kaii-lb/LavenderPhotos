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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.getAppTrashBinDirectory
import com.kaii.photos.helpers.single_image_functions.ImageFunctions

@Composable
fun TrashedPhotoGridView(navController: NavHostController) {
    Scaffold (
        topBar =  { TopBar(navController) },
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
                navController,
                ImageFunctions.LoadTrashedImage,
                LocalContext.current.getAppTrashBinDirectory().replace("/storage/emulated/0/", ""),
                MediaItemSortMode.LastModified,
                "Deleted items show up here",
                "Deleted On "
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController) {
    val title = "Trash Bin"
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = { navController.popBackStack() },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(160.dp)
            )
        },
        actions = {
            IconButton(
                onClick = { /* TODO */ },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.trash),
                    contentDescription = "empty out the trash bin",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            IconButton(
                onClick = { /* TODO */ },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.settings),
                    contentDescription = "show more options for the trash bin",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}
