package com.kaii.photos.compose.grids

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.search_page.SearchViewModel
import kotlinx.coroutines.delay

@Composable
fun SearchPage(navController: NavHostController, searchViewModel: SearchViewModel) {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .padding(8.dp)
    ) {
        var searchedForText by remember { mutableStateOf("") }
        var searchNow by remember { mutableStateOf(false) }
        var showLoadingSpinner by remember { mutableStateOf(true) }
        val selectedItemsList = remember { SnapshotStateList<String>() } // has the absolute paths of all the selected items
        
        Column (
        	modifier = Modifier
        		.fillMaxWidth(1f)
        		.fillMaxHeight(0.1f),
        	verticalArrangement = Arrangement.Center,
        	horizontalAlignment = Alignment.CenterHorizontally
        ) {
	        TextField(
	            value = searchedForText,
	            onValueChange = {
	                searchedForText = it
	            },
	            maxLines = 1,
	            singleLine = true,
	            placeholder = {
	                Text(
	                    text = "Search for an image's name",
	                    fontSize = TextUnit(16f, TextUnitType.Sp)
	                )
	            },
	            prefix = {
	                Icon(
	                    painter = painterResource(id = com.kaii.photos.R.drawable.search),
	                    contentDescription = "Search Icon"
	                )
	            },
	            colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
				keyboardOptions = KeyboardOptions(
					autoCorrectEnabled = false,
					keyboardType = KeyboardType.Text,
					imeAction = ImeAction.Search
				),
				keyboardActions = KeyboardActions(
					onSearch = {
						if (!showLoadingSpinner) {
							searchNow = true
							showLoadingSpinner = true
						}
					}
				),
	            shape = RoundedCornerShape(1000.dp),
	            modifier = Modifier
	                .fillMaxWidth(1f)
	        )
        }

        Spacer (modifier = Modifier.height(8.dp))

       	val mediaStoreDataHolder = searchViewModel.mediaStoreData.collectAsState()
      	val mediaStoreData by remember { mutableStateOf(mediaStoreDataHolder) }
		
		var originalGroupedMedia by remember { mutableStateOf(groupPhotosBy(mediaStoreData.value, MediaItemSortMode.DateTaken)) }

		var groupedMedia by remember { mutableStateOf(originalGroupedMedia) }

		// println("THE CURRENT STATE IS ${mediaStoreData.value}")

		LaunchedEffect(key1 = mediaStoreData.value) {
			originalGroupedMedia = groupPhotosBy(mediaStoreData.value, MediaItemSortMode.DateTaken)

			if (searchedForText == "") {
				groupedMedia = originalGroupedMedia
			}
		}
  	        
        LaunchedEffect(key1 = searchNow) {
        	if(!searchNow) return@LaunchedEffect

			delay(500)
        	
        	val groupedMediaLocal = originalGroupedMedia.filter {
        		val isMedia = it.type == MediaType.Image || it.type == MediaType.Video
        		val matchesFilter = it.displayName?.contains(searchedForText.trim(), true) == true
				isMedia && matchesFilter
			}
   	        groupedMedia = groupPhotosBy(groupedMediaLocal, MediaItemSortMode.DateTaken)

			searchNow = false
        }


        val requestBuilderTransform =
            { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
                requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
            }

        val preloadingData =
            rememberGlidePreloadingData(
                groupedMedia,
                Size(50f, 50f),
                requestBuilderTransform = requestBuilderTransform,
            )

        val gridState = rememberLazyGridState()

		Box(
			modifier = Modifier
				.fillMaxSize(1f)	
		) {
	        LazyVerticalGrid(
				columns = GridCells.Fixed(
					if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
						3
					} else {
						6
					}
				),
	            modifier = Modifier
	                .fillMaxSize(1f)
	                .align(Alignment.Center),
	            state = gridState
	        ) {
	            items(
	                count = preloadingData.size,
	                span = { index ->
	                	val neededIndex = if (index < groupedMedia.size) index else index - 1
	                    val item = groupedMedia[neededIndex]
	                    if (item.type == MediaType.Section) {
	                        GridItemSpan(maxLineSpan)
	                    } else {
	                        GridItemSpan(1)
	                    }
	                }
	            ) { i ->
	                val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]

	                MediaStoreItem(
                        navController,
                        mediaStoreItem,
                        preloadRequestBuilder,
                        ImageFunctions.LoadNormalImage,
                        MainActivity.mainViewModel,
                        groupedMedia,
                        "",
                        selectedItemsList,
						gridState
                    )

	                if (i >= 0) {
	                    val handler = Handler(Looper.getMainLooper())
	                    val runnable = Runnable {
	                        showLoadingSpinner = false
	                    }
	                    handler.removeCallbacks(runnable)
	                    handler.postDelayed(runnable, 500)
	                }
	            }
	        }

	        if (showLoadingSpinner) {
	            // println("SPINNING STARTED AT ${LocalTime.now()}")
	            Row (
	                modifier = Modifier
	                    .fillMaxWidth(1f)
	                    .height(48.dp)
	                    .align(Alignment.TopCenter),
	                verticalAlignment = Alignment.CenterVertically,
	                horizontalArrangement = Arrangement.Center
	            ) {
	                Row (
	                    modifier = Modifier
	                        .size(40.dp)
	                        .clip(RoundedCornerShape(1000.dp))
	                        .background(CustomMaterialTheme.colorScheme.surfaceContainer),
	                    verticalAlignment = Alignment.CenterVertically,
	                    horizontalArrangement = Arrangement.Center
	                ) {
	                    CircularProgressIndicator(
	                        modifier = Modifier
	                            .size(22.dp),
	                        color = CustomMaterialTheme.colorScheme.primary,
	                        strokeWidth = 4.dp,
	                        strokeCap = StrokeCap.Round
	                    )
	                }
	            }
	        }
        }
    }
}
