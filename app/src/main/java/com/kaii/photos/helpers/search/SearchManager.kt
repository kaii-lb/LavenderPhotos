package com.kaii.photos.helpers.search

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import com.kaii.photos.repositories.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class SearchManager(
    private val searchRepo: SearchRepository,
    private val tagRepo: TagRepository
) {
    private val _searchQuery = MutableStateFlow("")
    private val _searchMode = MutableStateFlow(SearchMode.Name)
    private val _searchingForTags = MutableStateFlow(false)
    private val _searchTagQuery = MutableStateFlow("")
    private val _selectedTags = mutableStateListOf<Tag>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchQuery = _searchingForTags.flatMapLatest { tagSearch ->
        if (tagSearch) _searchTagQuery.asStateFlow()
        else _searchQuery.asStateFlow()
    }

    val searchMode = _searchMode.asStateFlow()
    val searchingForTags = _searchingForTags.asStateFlow()
    val tags = tagRepo.allTags
    val mediaFlow = searchRepo.mediaFlow
    val gridMediaFlow = searchRepo.gridMediaFlow
    val selectedTags = snapshotFlow { _selectedTags.toList() }

    fun search(query: String) {
        if (_searchingForTags.value) {
            _searchTagQuery.value = query
        } else {
            _searchQuery.value = query
            searchRepo.search(query, _selectedTags.toSet())
        }
    }

    fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        accessToken: String? = null,
        mode: SearchMode? = null
    ) = searchRepo.update(sortMode, format, accessToken, mode)

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        update(mode = mode)
    }

    fun setSearchingForTags(value: Boolean) {
        _searchingForTags.value = value
    }

    fun toggleTagSelected(tag: Tag) {
        if (tag in _selectedTags) _selectedTags.remove(tag)
        else _selectedTags.add(tag)

        searchRepo.search(_searchQuery.value, _selectedTags.toSet())
    }

    fun clearSelectedTags() {
        _selectedTags.clear()
    }

    fun clear() {
        setSearchMode(SearchMode.Name)
        _searchQuery.value = ""
        _searchingForTags.value = false
        _searchTagQuery.value = ""
        _selectedTags.clear()
        search("")
    }

    suspend fun deleteTag(tag: Tag) {
        tagRepo.deleteTag(tag)
    }
}