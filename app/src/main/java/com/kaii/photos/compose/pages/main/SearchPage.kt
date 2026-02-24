package com.kaii.photos.compose.pages.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.widgets.SearchTextField
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.search_page.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchPage(
    viewModel: SearchViewModel,
    selectionManager: SelectionManager,
    isMediaPicker: Boolean
) {
    val gridState = rememberLazyGridState()

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val coroutineScope = rememberCoroutineScope()
            val query by viewModel.query.collectAsStateWithLifecycle()

            SearchTextField(
                query = query,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp, 0.dp),
                onQueryChange = { text ->
                    coroutineScope.launch {
                        viewModel.search(query = text)

                        delay(PhotoGridConstants.UPDATE_TIME)
                        gridState.scrollToItem(0)
                    }
                },
                onSearchModeChange = { mode ->
                    viewModel.update(mode = mode)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
        ) {
            val items = viewModel.gridMediaFlow.collectAsLazyPagingItems()
            PhotoGrid(
                pagingItems = items,
                albumInfo = AlbumInfo.Empty,
                selectionManager = selectionManager,
                viewProperties = ViewProperties.SearchNotFound,
                state = gridState,
                isMainPage = true,
                isMediaPicker = isMediaPicker,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}