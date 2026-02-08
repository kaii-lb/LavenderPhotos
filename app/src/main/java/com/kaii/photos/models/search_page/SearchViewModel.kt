package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.repositories.SearchRepository

class SearchViewModel(
    context: Context,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val repo = SearchRepository(
        scope = viewModelScope,
        info = info,
        context = context,
        sortMode = sortMode,
        format = format
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun search(query: String) = repo.search(query)

    fun update(
        sortMode: MediaItemSortMode?,
        format: DisplayDateFormat?,
        accessToken: String?
    ) = repo.update(sortMode, format, accessToken)
}

