package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.search.SearchManager
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import com.kaii.photos.repositories.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SearchViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val confirmToDelete = settings.permissions.getConfirmToDelete().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val doNotTrash = settings.permissions.getDoNotTrash().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val displayDateFormat = settings.lookAndFeel.getDisplayDateFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DisplayDateFormat.Default
    )

    val sortMode = settings.photoGrid.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MediaItemSortMode.DateTaken
    )

    val immichInfo = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
    )

    val columnSize = settings.lookAndFeel.getColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val openVideosExternally = settings.behaviour.getOpenVideosExternally().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val cacheThumbnails = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val thumbnailSize = settings.storage.getThumbnailSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 256
    )

    val useRoundedCorners = settings.lookAndFeel.getUseRoundedCorners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private var initialSortMode: MediaItemSortMode
    private var initialFormat: DisplayDateFormat
    private var initialInfo: ImmichBasicInfo

    init {
        runBlocking {
            initialSortMode = sortMode.first()
            initialFormat = displayDateFormat.first()
            initialInfo = immichInfo.first()
        }

        viewModelScope.launch {
            displayDateFormat.collect { update(format = it) }
        }

        viewModelScope.launch {
            sortMode.collect { update(sortMode = it) }
        }

        viewModelScope.launch {
            immichInfo.collect { update(accessToken = it.accessToken) }
        }
    }

    private val searchManager = SearchManager(
        searchRepo = SearchRepository(
            searchDao = MediaDatabase.getInstance(context.applicationContext).searchDao(),
            taggedItemsDao = MediaDatabase.getInstance(context.applicationContext).taggedItemsDao(),
            scope = viewModelScope,
            info = initialInfo,
            sortMode = initialSortMode,
            format = initialFormat
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

    private fun update(
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

