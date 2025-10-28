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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.widgets.ClearableTextField
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

@Composable
fun SearchPage(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val mainViewModel = LocalMainViewModel.current
    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(
            context = context,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    )

    val mediaStoreDataHolder =
        searchViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val originalGroupedMedia = remember { derivedStateOf { mediaStoreDataHolder.value } }

    val groupedMedia = remember { mutableStateOf(originalGroupedMedia.value) }

    val actualGroupedMedia by searchViewModel.groupedMedia.collectAsStateWithLifecycle()
    LaunchedEffect(actualGroupedMedia) {
        groupedMedia.value = actualGroupedMedia
    }

    LaunchedEffect(groupedMedia.value) {
        mainViewModel.setGroupedMedia(groupedMedia.value)
    }

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
                if (groupedMedia.value.isEmpty()) true else !hideLoadingSpinner
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

        var hasFiles by remember { mutableStateOf(true) }

        LaunchedEffect(searchedForText.value, originalGroupedMedia.value, sortMode) {
            println("ORIGINAL CHANGED REFRESHING")
            if (searchedForText.value == "") {
                groupedMedia.value = originalGroupedMedia.value
                hideLoadingSpinner = true
                return@LaunchedEffect
            }

            delay(PhotoGridConstants.UPDATE_TIME)
            hideLoadingSpinner = false
            hasFiles = true

            coroutineScope.launch(Dispatchers.IO) {
                val query = searchedForText.value.trim()
                var final = searchViewModel.searchByDateFormat(query = query)

                if (final.isEmpty()) {
                    final = searchViewModel.searchByDateNames(query = query)
                }

                if (final.isEmpty()) {
                    final = searchViewModel.searchByName(name = query)
                }

                searchViewModel.setMedia(
                    context = context,
                    media = final,
                    sortMode = sortMode,
                    displayDateFormat = displayDateFormat
                )
                hideLoadingSpinner = true

                delay(PhotoGridConstants.LOADING_TIME)
                hasFiles = groupedMedia.value.isNotEmpty()
                gridState.requestScrollToItem(0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(1f)
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                selectedItemsList = selectedItemsList,
                viewProperties = if (searchedForText.value == "") ViewProperties.SearchLoading else ViewProperties.SearchNotFound,
                state = gridState,
                hasFiles = hasFiles,
                isMainPage = true,
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