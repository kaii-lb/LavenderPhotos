package com.kaii.photos.compose.grids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import kotlinx.coroutines.Dispatchers

@Composable
fun SearchPage(
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(LocalContext.current, "", MediaItemSortMode.DateTaken)
    )
    val mediaStoreDataHolder =
        searchViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val originalGroupedMedia = remember { mutableStateOf(mediaStoreDataHolder.value) }

    val groupedMedia = remember { mutableStateOf(originalGroupedMedia.value) }

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background)
    ) {
        val searchedForText = rememberSaveable { mutableStateOf("") }
        var searchNow by rememberSaveable { mutableStateOf(false) }
        val showLoadingSpinner by remember {
            derivedStateOf {
                groupedMedia.value.isEmpty() && searchedForText.value == ""
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(CustomMaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SearchTextField(
            	searchedForText = searchedForText, 
            	placeholder = "Search for a photo's name",
            	modifier = Modifier
   		            .fillMaxWidth(1f)
  		            .height(56.dp)
  		            .padding(8.dp, 0.dp),
  		        onSearch = {	        	
	                if (!showLoadingSpinner) {
	                    searchNow = true
	                }
  		        },
  		        onClear = {
  		        	searchedForText.value = ""
  		        	searchNow = true
  		        }
           	)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(key1 = mediaStoreDataHolder.value) {
            originalGroupedMedia.value = mediaStoreDataHolder.value

            if (searchedForText.value == "") {
                groupedMedia.value = originalGroupedMedia.value
            }
        }

        LaunchedEffect(key1 = searchedForText.value) {
            // if (!searchNow) return@LaunchedEffect

            groupedMedia.value = emptyList()

            if (searchedForText.value == "") {
                groupedMedia.value = originalGroupedMedia.value
            } else {
                val groupedMediaLocal = originalGroupedMedia.value.filter {
                    val isMedia = it.type != MediaType.Section
                    val matchesFilter = it.displayName?.contains(searchedForText.value.trim(), true) == true
                    isMedia && matchesFilter
                }
                groupedMedia.value = groupPhotosBy(groupedMediaLocal, MediaItemSortMode.DateTaken)
            }

            searchNow = false
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                navController = navController,
                path = null,
                selectedItemsList = selectedItemsList,
                viewProperties = if (searchedForText.value == "") ViewProperties.SearchLoading else ViewProperties.SearchNotFound,
                modifier = Modifier
                    .align(Alignment.Center)
            )

            if (showLoadingSpinner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(48.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
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
