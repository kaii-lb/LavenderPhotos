package com.kaii.photos.models.secure_folder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.di.appModule
import com.kaii.photos.repositories.SecureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SecureFolderViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.appModule.settings

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

    private val repo = SecureRepository(
        context = context,
        scope = viewModelScope,
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat(),
        info = settings.immich.getImmichBasicInfo()
    )

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)

    init {
        repo.attachFileObserver()
    }

    override fun onCleared() {
        super.onCleared()
        repo.detachFileObserver()
    }
}