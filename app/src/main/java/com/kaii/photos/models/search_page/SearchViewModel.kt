package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.search.SearchManager
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import com.kaii.photos.repositories.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    context: Context,
    info: ImmichBasicInfo,
    sortMode: MediaItemSortMode,
    format: DisplayDateFormat
) : ViewModel() {
    private val searchManager = SearchManager(
        searchRepo = SearchRepository(
            searchDao = MediaDatabase.getInstance(context.applicationContext).searchDao(),
            taggedItemsDao = MediaDatabase.getInstance(context.applicationContext).taggedItemsDao(),
            scope = viewModelScope,
            info = info,
            sortMode = sortMode,
            format = format
        ),
        tagRepo = TagRepository(
            dao = MediaDatabase.getInstance(context.applicationContext).tagDao()
        )
    )

    val mediaFlow = searchManager.mediaFlow
    val gridMediaFlow = searchManager.gridMediaFlow

    val tags = searchManager.tags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    val searchQuery = searchManager.searchQuery.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ""
    )

    val searchMode = searchManager.searchMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SearchMode.Name
    )

    val searchingForTags = searchManager.searchingForTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val selectedTags = searchManager.selectedTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun search(
        query: String
    ) = searchManager.search(query)

    fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        accessToken: String? = null
    ) = searchManager.update(sortMode, format, accessToken)

    fun setSearchMode(mode: SearchMode) = searchManager.setSearchMode(mode)
    fun setSearchingForTags(value: Boolean) = searchManager.setSearchingForTags(value)
    fun toggleTagSelected(tag: Tag) = searchManager.toggleTagSelected(tag)
    fun clearSelectedTags() = searchManager.clearSelectedTags()

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            searchManager.deleteTag(tag)
        }
    }

    fun clear() = searchManager.clear()
}

