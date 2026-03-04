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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.repositories.SearchMode
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

            val columnSize by viewModel.columnSize.collectAsStateWithLifecycle()
            val openVideosExternally by viewModel.openVideosExternally.collectAsStateWithLifecycle()
            val cacheThumbnails by viewModel.cacheThumbnails.collectAsStateWithLifecycle()
            val thumbnailSize by viewModel.thumbnailSize.collectAsStateWithLifecycle()
            val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()

            PhotoGrid(
                pagingItems = items,
                album = AlbumType.PlaceHolder,
                selectionManager = selectionManager,
                viewProperties = ViewProperties.SearchNotFound,
                state = gridState,
                isMainPage = true,
                isMediaPicker = isMediaPicker,
                columnSize = columnSize,
                openVideosExternally = openVideosExternally,
                cacheThumbnails = cacheThumbnails,
                thumbnailSize = thumbnailSize,
                useRoundedCorners = useRoundedCorners,
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
            dialogTitle = stringResource(id = R.string.tags_delete, currentTag?.name ?: ""),
            confirmButtonLabel = stringResource(id = R.string.media_delete)
        ) {
            viewModel.deleteTag(currentTag!!)
        }

        val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val searchMode by viewModel.searchMode.collectAsStateWithLifecycle()
        val searchingForTags by viewModel.searchingForTags.collectAsStateWithLifecycle()

        SearchTextField(
            searchQuery = searchQuery,
            searchMode = searchMode,
            searchingForTags = searchingForTags,
            tags = tags,
            selectedTags = selectedTags,
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .padding(horizontal = 8.dp),
            onQueryChange = { text ->
                coroutineScope.launch {
                    viewModel.search(query = text)

                    if (searchMode != SearchMode.Tag) {
                        delay(PhotoGridConstants.UPDATE_TIME)
                        gridState.scrollToItem(0)
                    }
                }
            },
            onSearchModeChange = { mode ->
                viewModel.setSearchMode(mode)
            },
            onToggleTag = { tag ->
                viewModel.toggleTagSelected(tag)
            },
            onTagRemove = {
                currentTag = it
                showDialog.value = true
            },
            setSearchingForTags = viewModel::setSearchingForTags,
            onClearTags = viewModel::clearSelectedTags
        )
    }
}