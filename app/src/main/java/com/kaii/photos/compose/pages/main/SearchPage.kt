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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.search_page.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

@Composable
fun SearchPage(
    searchViewModel: SearchViewModel,
    selectionManager: SelectionManager,
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
        val searchedForText = rememberSaveable { mutableStateOf("") }

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
                    "$day $month $year",
                    "$date $month $year",
                    "#Year: $year",
                    "#Month: $month",
                    "#Day: $day"
                )
            }

            ClearableTextField(
                text = searchedForText,
                placeholder = placeholdersList.random(),
                icon = R.drawable.search,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp, 0.dp),
                onConfirm = {},
                onClear = {
                    searchedForText.value = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(searchedForText.value, sortMode) {
            searchViewModel.search(query = searchedForText.value)

            delay(PhotoGridConstants.UPDATE_TIME)
            gridState.scrollToItem(0)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
        ) {
            PhotoGrid(
                pagingItems = items,
                albumInfo = AlbumInfo.Empty,
                selectionManager = selectionManager,
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