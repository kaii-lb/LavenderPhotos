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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.kaii.photos.R
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers

@Composable
fun SearchPage(navController: NavHostController, selectedItemsList: SnapshotStateList<MediaStoreData>) {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .padding(8.dp)
    ) {
        var searchedForText by remember { mutableStateOf("") }
        var searchNow by remember { mutableStateOf(false) }
        var showLoadingSpinner by remember { mutableStateOf(true) }
        
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

		val searchViewModel: GalleryViewModel = viewModel(
			factory = GalleryViewModelFactory(LocalContext.current, "", MediaItemSortMode.DateTaken)
		)
       	val mediaStoreDataHolder = searchViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
		
		var originalGroupedMedia by remember { mutableStateOf(mediaStoreDataHolder.value) }

		var groupedMedia = remember { mutableStateOf(originalGroupedMedia) }

		LaunchedEffect(key1 = mediaStoreDataHolder.value) {
			originalGroupedMedia = mediaStoreDataHolder.value

			if (searchedForText == "") {
				groupedMedia.value = originalGroupedMedia
			}
		}
  	        
        LaunchedEffect(key1 = searchNow) {
        	if(!searchNow) return@LaunchedEffect

			delay(500)
        	
        	val groupedMediaLocal = originalGroupedMedia.filter {
        		val isMedia = it.type != MediaType.Section
        		val matchesFilter = it.displayName?.contains(searchedForText.trim(), true) == true
				isMedia && matchesFilter
			}
   	        groupedMedia.value = groupPhotosBy(groupedMediaLocal, MediaItemSortMode.DateTaken)

			searchNow = false
        }

        val gridState = rememberLazyGridState()
	        	
		PhotoGrid(
			groupedMedia = groupedMedia,
			navController = navController,
			operation = ImageFunctions.LoadNormalImage, 
			path = null,
			selectedItemsList = selectedItemsList,
			emptyText = "Search for some photos!",
			emptyIconResId = R.drawable.search
		)
    }
}
