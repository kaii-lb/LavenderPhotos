package com.kaii.photos.compose

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.MainActivity
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.main_activity.MainDataSharingModel

@Composable
fun SingleAlbumView(navController: NavHostController) {
    val mainViewModel = MainActivity.mainViewModel

    val albumDir = mainViewModel.selectedAlbumDir.collectAsState(initial = null).value
	println("ALBUM DIR IS $albumDir")
    if (albumDir == null) return

	val topBarTitle = albumDir.split("/").last()
	
    Scaffold (
        topBar =  { TopBar(navController, topBarTitle) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(navController, albumDir ?: MainDataSharingModel.IGNORE_THIS_PATH)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavHostController, title: String?) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = { navController.popBackStack() },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title ?: "Album",
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
                    painter = painterResource(id = com.kaii.photos.R.drawable.settings),
                    contentDescription = "show more options for the trash bin",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}
