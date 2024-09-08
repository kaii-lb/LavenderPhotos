package com.kaii.photos.compose

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.Window
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.MainActivity
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.helpers.single_image_functions.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.launch

private const val TAG = "SINGLE_PHOTO_VIEW"

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoView(navController: NavHostController, window: Window) {
    val mainViewModel = MainActivity.mainViewModel
    
    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

    val holderGroupedMedia = mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

	val groupedMedia = holderGroupedMedia.filter { item ->
        !(item.mimeType == null && item.type == MediaType.Section)
   	}
	
    var systemBarsShown by remember { mutableStateOf(true) }
    var appBarAlpha by remember { mutableFloatStateOf(1f) }
    var currentMediaItem by remember { mutableStateOf(mediaItem) }

    Scaffold (
        topBar =  { TopBar(navController, currentMediaItem, appBarAlpha) },
        bottomBar = { BottomBar(navController, appBarAlpha, currentMediaItem) },
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) {  _ ->
        val state = rememberLazyListState()
        Column (
            modifier = Modifier
                .padding(0.dp)
                .background(CustomMaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val requestBuilderTransform =
                { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
                    requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
                }

            val preloadingData =
                rememberGlidePreloadingData(
                    groupedMedia,
                    Size(75f, 75f),
                    requestBuilderTransform = requestBuilderTransform,
                )

            LazyRow (
                modifier = Modifier
                    .fillMaxHeight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                state = state,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
            ) {
                items(
                    count = preloadingData.size,
                    key = {
       	                groupedMedia[it].uri.toString()
       	            },
                ) { i ->
                    val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]

                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

					if (mediaStoreItem.type != MediaType.Section && mediaStoreItem.mimeType != null && mediaStoreItem.id != 0L) {
						currentMediaItem = mediaStoreItem
						
	                    GlideImage(
	                        model = mediaStoreItem.uri,
	                        contentDescription = "selected image",
	                        contentScale = ContentScale.Fit,
	                        modifier = Modifier
	                            .fillParentMaxSize(1f)
	                            .clickable {
	                                if (systemBarsShown) {
	                                    windowInsetsController.apply {
	                                        hide(WindowInsetsCompat.Type.systemBars())
	                                        systemBarsBehavior =
	                                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
	                    ) {
	                        it.thumbnail(preloadRequestBuilder).signature(mediaStoreItem.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
	                    }
					} else {
						Column (
							modifier = Modifier.fillParentMaxSize(1f).background(CustomMaterialTheme.colorScheme.primary)
						) {
							
						}
					}
                }
            }

            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(key1 = mediaItem) {
                coroutineScope.launch {
                    state.scrollToItem(
                        if (groupedMedia.indexOf(mediaItem) >= 0) groupedMedia.indexOf(mediaItem) else 0
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, mediaItem: MediaStoreData?, alpha: Float) {
    TopAppBar(
    	modifier = Modifier.alpha(alpha),
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
                    tint = CustomMaterialTheme.colorScheme.onBackground,
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
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Composable
private fun BottomBar(navController: NavHostController, alpha: Float, item: MediaStoreData) {
    val context = LocalContext.current

    BottomAppBar(
        containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
        contentColor = CustomMaterialTheme.colorScheme.onBackground,
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
                    val operation = ImageFunctions.entries[index] // WARNING: ORDER IS VERY IMPORTANT!!!
		    		Button(
		                onClick = {
                            operateOnImage(item.absolutePath, item.id, operation, context)
                            navController.popBackStack()
                        },
		                colors = ButtonDefaults.buttonColors(
		                	containerColor = Color.Transparent,
		                	contentColor = CustomMaterialTheme.colorScheme.onBackground
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
			                    tint = CustomMaterialTheme.colorScheme.onBackground,
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
