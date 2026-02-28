package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
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
    private val searchRepo = SearchRepository(
        searchDao = MediaDatabase.getInstance(context.applicationContext).searchDao(),
        taggedItemsDao = MediaDatabase.getInstance(context.applicationContext).taggedItemsDao(),
        info = info,
        sortMode = sortMode,
        format = format
    )

    private val tagRepo = TagRepository(
        dao = MediaDatabase.getInstance(context.applicationContext).tagDao()
    )

    val mediaFlow = searchRepo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = searchRepo.gridMediaFlow.cachedIn(viewModelScope)

    val tags = tagRepo.allTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    fun search(
        query: String,
        tags: List<Tag>
    ) {
        viewModelScope.launch {
            searchRepo.search(query, tags.toSet())
        }
    }

    fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        accessToken: String? = null,
        mode: SearchMode? = null
    ) {
        viewModelScope.launch {
            searchRepo.update(sortMode, format, accessToken, mode)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepo.deleteTag(tag)
        }
    }
}

