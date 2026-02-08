package com.kaii.photos.compose.grids

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.search_page.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

@Composable
fun SearchPage(
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    searchViewModel: SearchViewModel,
    isMediaPicker: Boolean
) {
    val mainViewModel = LocalMainViewModel.current
    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

    val items = searchViewModel.gridMediaFlow.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val coroutineScope = rememberCoroutineScope()
        val scrollBackToTop = {
            coroutineScope.launch {
                gridState.animateScrollToItem(0)
            }
        }

        val searchedForText = rememberSaveable { mutableStateOf("") }

        var hideLoadingSpinner by remember { mutableStateOf(false) }
        val showLoadingSpinner by remember {
            derivedStateOf {
                if (items.loadState.source == LoadState.Loading) true else !hideLoadingSpinner
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val resources = LocalResources.current
            val placeholdersList = remember {
                val month = Month.entries.random().name.toPascalCase()
                val day = DayOfWeek.entries.random().name.toPascalCase()
                val date = (1..31).random()
                val year = (2016..2024).random()

                listOf(
                    resources.getString(R.string.search_photo_name),
                    resources.getString(R.string.search_photo_date),
                    "$month $year",
                    resources.getString(R.string.search_photo_day),
                    "$day $month $year",
                    "$date $month $year"
                )
            }
            val placeholder = remember { placeholdersList.random() }

            ClearableTextField(
                text = searchedForText,
                placeholder = placeholder,
                icon = R.drawable.search,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp, 0.dp),
                onConfirm = {
                    if (!showLoadingSpinner) {
                        scrollBackToTop()
                    }
                },
                onClear = {
                    searchedForText.value = ""
                    scrollBackToTop()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(hideLoadingSpinner) {
            if (!hideLoadingSpinner) {
                delay(10000)
                hideLoadingSpinner = true
            }
        }

        LaunchedEffect(searchedForText.value, sortMode) {
            if (searchedForText.value.isEmpty()) {
                searchViewModel.search(query = "")
                hideLoadingSpinner = true
                return@LaunchedEffect
            }

            delay(PhotoGridConstants.UPDATE_TIME)
            hideLoadingSpinner = false

            searchViewModel.search(query = searchedForText.value)

            hideLoadingSpinner = true

            delay(PhotoGridConstants.LOADING_TIME)
            gridState.requestScrollToItem(0)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
        ) {
            PhotoGrid(
                pagingItems = items,
                albumInfo = AlbumInfo.Empty,
                selectedItemsList = selectedItemsList,
                viewProperties = if (searchedForText.value == "") ViewProperties.SearchLoading else ViewProperties.SearchNotFound,
                state = gridState,
                isMainPage = true,
                isMediaPicker = isMediaPicker,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}

fun String.toPascalCase(): String {
    return this.lowercase().split(Regex("[\\s_-]+")) // Split by spaces, underscores, or hyphens
        .joinToString("") { word ->
            word.replaceFirstChar {
                it.uppercase()
            }
        }
}