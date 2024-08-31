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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.MainActivity
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SinglePhotoView(navController: NavHostController) {
    val mainViewModel = MainActivity.mainViewModel
    
    val mediaItem = mainViewModel.selectedMediaUri.collectAsState(initial = null).value

    Scaffold (
        topBar =  { TopBar(mediaItem, navController) },
        bottomBar = { BottomBar() },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { scaffoldPadding ->

        Column (
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        	// val windowInsetsController = WindowCompat.getInsetsController
            GlideImage(
                model = mediaItem?.uri ?: Uri.parse(""),
                contentDescription = "selected image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .clickable {
                    	
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(mediaItem: MediaStoreData?, navController: NavHostController) {
    TopAppBar(
    	colors = TopAppBarDefaults.topAppBarColors(
    		containerColor = MaterialTheme.colorScheme.surfaceContainer
    	),
    	navigationIcon = {
    		IconButton(
                onClick = { navController.popBackStack() },
                colors = IconButtonDefaults.iconButtonColors(
                	containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
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
            val mediaTitle = if (mediaItem != null) {
                mediaItem.displayName ?: mediaItem.type.name
            } else {
                "Media"
            }

            Spacer (modifier = Modifier.width(8.dp))

            Text(
                text = mediaTitle,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                	.width(160.dp)
            )
        }
    )
}

@Composable
fun BottomBar() {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp),
        actions = {
            Row (

            ) {

            }
        }
    )
}
