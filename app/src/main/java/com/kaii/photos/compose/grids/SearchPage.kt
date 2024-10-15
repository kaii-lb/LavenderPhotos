package com.kaii.photos.compose.grids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@Composable
fun SearchPage(navController: NavHostController, selectedItemsList: SnapshotStateList<MediaStoreData>) {
	val searchViewModel: SearchViewModel = viewModel(
		factory = SearchViewModelFactory(LocalContext.current, "", MediaItemSortMode.DateTaken)
	)
   	val mediaStoreDataHolder = searchViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

	var originalGroupedMedia = remember { mutableStateOf(mediaStoreDataHolder.value) }

	val groupedMedia = remember { mutableStateOf(originalGroupedMedia.value) }

    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background)
    ) {
        var searchedForText by remember { mutableStateOf("") }
        var searchNow by remember { mutableStateOf(false) }
        val showLoadingSpinner by remember { derivedStateOf {
        	groupedMedia.value.size == 0
        }}
        
        Column (
        	modifier = Modifier
        		.fillMaxWidth(1f)
        		.fillMaxHeight(0.1f)
				.padding(8.dp, 0.dp)
				.background(CustomMaterialTheme.colorScheme.background),
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
	                    painter = painterResource(id = R.drawable.search),
	                    contentDescription = "Search Icon"
	                )
	            },
	            colors = TextFieldDefaults.colors(
                    focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                    cursorColor = CustomMaterialTheme.colorScheme.primary,
                    focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
						}
					}
				),
	            shape = RoundedCornerShape(1000.dp),
	            modifier = Modifier
	                .fillMaxWidth(1f)
	        )
        }

        Spacer (modifier = Modifier.height(8.dp))

		LaunchedEffect(key1 = mediaStoreDataHolder.value) {
			originalGroupedMedia.value = mediaStoreDataHolder.value

			if (searchedForText == "") {
				groupedMedia.value = originalGroupedMedia.value
			}
		}
  	        
        LaunchedEffect(key1 = searchNow) {
        	if(!searchNow) return@LaunchedEffect

			groupedMedia.value = emptyList()
        	
        	val groupedMediaLocal = originalGroupedMedia.value.filter {
        		val isMedia = it.type != MediaType.Section
        		val matchesFilter = it.displayName?.contains(searchedForText.trim(), true) == true
				isMedia && matchesFilter
			}
   	        groupedMedia.value = groupPhotosBy(groupedMediaLocal, MediaItemSortMode.DateTaken)

			searchNow = false
        }

	    Box (
	    	modifier = Modifier
	    		.fillMaxHeight(1f)	
	    ) {
			PhotoGrid(
				groupedMedia = groupedMedia,
				navController = navController,
				operation = ImageFunctions.LoadNormalImage, 
				path = null,
				selectedItemsList = selectedItemsList,
				emptyText = if (searchedForText != "") "Unable to find anything with that name" else "Search for some photos!",
				emptyIconResId = R.drawable.search,
				modifier = Modifier
					.align(Alignment.Center)
			)

			if (showLoadingSpinner) {
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
