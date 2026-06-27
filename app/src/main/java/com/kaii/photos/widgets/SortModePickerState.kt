package com.kaii.photos.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kaii.photos.di.appModule
import com.kaii.photos.domain.settings.SortModeItem
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SortModePickerState(
    sortModeFlow: Flow<MediaItemSortMode>,
    coroutineScope: CoroutineScope,
    private val setMode: (mode: MediaItemSortMode) -> Unit
) {
    var currentMode by mutableStateOf(SortModeItem.defaultItems.first())
        private set

    init {
        coroutineScope.launch {
            sortModeFlow.collect {
                currentMode = SortModeItem.getForMode(it)
            }
        }
    }

    fun setMode(mode: SortModeItem) {
        currentMode = mode
        setMode(mode.sortMode)
    }
}

@Composable
fun rememberSortModePickerState(): SortModePickerState {
    val settings = LocalContext.current.appModule.settings.photoGrid
    val coroutineScope = rememberCoroutineScope()

    return remember {
        SortModePickerState(
            sortModeFlow = settings.getSortMode(),
            coroutineScope = coroutineScope,
            setMode = settings::setSortMode
        )
    }
}