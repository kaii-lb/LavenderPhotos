package com.kaii.photos.models.search_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.appModule.settings

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

    private lateinit var initialSortMode: MediaItemSortMode
    private lateinit var initialFormat: DisplayDateFormat
    private lateinit var initialInfo: ImmichBasicInfo

    init {
        viewModelScope.launch {
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

    private val repo = SearchRepository(
        info = initialInfo,
        context = context,
        sortMode = initialSortMode,
        format = initialFormat
    )

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)

    fun search(query: String) {
        viewModelScope.launch {
            repo.search(query)
        }
    }

    fun changeMode(mode: SearchMode) {
        viewModelScope.launch {
            repo.update(null, null, null, mode)
        }
    }

    private fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        accessToken: String? = null,
        mode: SearchMode? = null
    ) {
        viewModelScope.launch {
            repo.update(sortMode, format, accessToken, mode)
        }
    }
}

