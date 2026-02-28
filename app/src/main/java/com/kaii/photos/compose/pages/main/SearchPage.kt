package com.kaii.photos.compose.pages.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.R
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.widgets.SearchTextField
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.search_page.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchPage(
    viewModel: SearchViewModel,
    selectionManager: SelectionManager,
    isMediaPicker: Boolean
) {
    val gridState = rememberLazyGridState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 64.dp)
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

        // counter intuitively below the grid since it needs to display *above* it
        val coroutineScope = rememberCoroutineScope()
        val tags by viewModel.tags.collectAsStateWithLifecycle()

        val showDialog = remember { mutableStateOf(false) }
        var currentTag by remember { mutableStateOf<Tag?>(null) }
        ConfirmationDialog(
            showDialog = showDialog,
            dialogTitle = stringResource(id = R.string.tags_delete),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            viewModel.deleteTag(currentTag!!)
        }

        val selectedTags = retain { mutableStateListOf<Tag>() }

        SearchTextField(
            tags = tags,
            selectedTags = selectedTags,
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .padding(horizontal = 8.dp),
            onQueryChange = { text, tags ->
                coroutineScope.launch {
                    viewModel.search(query = text, tags)

                    delay(PhotoGridConstants.UPDATE_TIME)
                    gridState.scrollToItem(0)
                }
            },
            onSearchModeChange = { mode ->
                viewModel.update(mode = mode)
            },
            onTagAdd = { query, tags ->
                viewModel.search(query, tags)
            },
            onTagRemove = { tag ->
                currentTag = tag
                showDialog.value = true
            }
        )
    }
}