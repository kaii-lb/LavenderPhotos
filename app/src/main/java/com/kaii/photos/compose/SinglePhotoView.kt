package com.kaii.photos.compose

import android.annotation.SuppressLint
import android.net.Uri
import android.view.Window
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.MainActivity
import com.kaii.photos.mediastore.MediaStoreData

@OptIn(ExperimentalGlideComposeApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoView(navController: NavHostController, window: Window) {
    val mainViewModel = MainActivity.mainViewModel
    
    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value

	if (mediaItem == null) return

    var systemBarsShown by remember { mutableStateOf(true) }
    var appBarAlpha by remember { mutableFloatStateOf(1f) }

    Scaffold (
        topBar =  { TopBar(mediaItem, navController, appBarAlpha) },
        bottomBar = { BottomBar(appBarAlpha) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {  _ ->
        Column (
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

            GlideImage(
                model = mediaItem?.uri ?: Uri.parse(""),
                contentDescription = "selected image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .clickable {
                    	if (systemBarsShown) {
                            windowInsetsController.apply {
                            	hide(WindowInsetsCompat.Type.systemBars())	
                            	systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            } 
							window.setDecorFitsSystemWindows(false)
                            systemBarsShown = false
                            appBarAlpha = 0f
                        } else {
                            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())                            
                            window.setDecorFitsSystemWindows(false)
                            systemBarsShown = true
                            appBarAlpha = 1f
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(mediaItem: MediaStoreData?, navController: NavHostController, alpha: Float) {
    TopAppBar(
    	modifier = Modifier.alpha(alpha),
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
            val mediaTitle = if (mediaItem != null) {
                mediaItem.displayName ?: mediaItem.type.name
            } else {
                "Media"
            }

            Spacer (modifier = Modifier.width(8.dp))

            Text(
                text = mediaTitle,
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
                    painter = painterResource(id = com.kaii.photos.R.drawable.favorite),
                    contentDescription = "favorite this media item",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(0.dp, 1.dp, 0.dp, 0.dp)
                )
            }

       		IconButton(
                onClick = { /* TODO */ },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.more_options),
                    contentDescription = "show more options",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Composable
fun BottomBar(alpha: Float) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentPadding = PaddingValues(0.dp),
        actions = {
            Row (
				modifier = Modifier
					.fillMaxWidth(1f)
           			.padding(12.dp, 0.dp),
				verticalAlignment = Alignment.CenterVertically,
               	horizontalArrangement = Arrangement.SpaceEvenly	
            ) {	
            	val listOfResources = listOf(
            		com.kaii.photos.R.drawable.share,
           			com.kaii.photos.R.drawable.paintbrush,
            		com.kaii.photos.R.drawable.trash,	
            		com.kaii.photos.R.drawable.locked_folder		
           		)

           		val listOfStrings = listOf(
           			"Share",
           			"Edit",
           			"Delete",
           			"Hide"
           		)
            	
            	repeat(4) { index ->
		    		Button(
		                onClick = { /* TODO */ },
		                colors = ButtonDefaults.buttonColors(
		                	containerColor = Color.Transparent,
		                	contentColor = MaterialTheme.colorScheme.onBackground
		                ),
		                contentPadding = PaddingValues(0.dp, 4.dp),
		                modifier = Modifier
		                	.wrapContentHeight()
		                	.weight(1f)
		            ) {
		            	Column (
		            		verticalArrangement = Arrangement.Center,
		            		horizontalAlignment = Alignment.CenterHorizontally
		            	) {
			                Icon(
			                    painter = painterResource(id = listOfResources[index]),
			                    contentDescription = listOfStrings[index],
			                    tint = MaterialTheme.colorScheme.onBackground,
			                    modifier = Modifier
			                        .size(26.dp)
			                )
				            Text(
				                text = listOfStrings[index],
				                fontSize = TextUnit(15f, TextUnitType.Sp),
				                maxLines = 1,
				                modifier = Modifier
				                	.padding(0.dp, 2.dp, 0.dp, 0.dp)
				            )
		            	}
		            }					            
            	}
            }
        },
        modifier = Modifier.alpha(alpha)
    )
}
