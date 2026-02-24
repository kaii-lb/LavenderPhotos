package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import kotlinx.coroutines.launch

class SearchViewModel(
    context: Context,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = SearchRepository(
        info = info,
        context = context,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)

    val query = repo.query

    fun search(query: String) {
        viewModelScope.launch {
            repo.search(query)
        }
    }

    fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        accessToken: String? = null,
        mode: SearchMode? = null
    ) {
        viewModelScope.launch {
            repo.update(sortMode, format, accessToken, mode)
            repo.search(query.value, true)
        }
    }
}

