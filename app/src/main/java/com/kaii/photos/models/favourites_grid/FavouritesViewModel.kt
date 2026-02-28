package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.di.appModule
import com.kaii.photos.repositories.FavouritesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class FavouritesViewModel(
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

    private val repo = FavouritesRepository(
        dao = MediaDatabase.getInstance(context.applicationContext).mediaDao(),
        info = settings.immich.getImmichBasicInfo(),
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat()
    )

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)
}
