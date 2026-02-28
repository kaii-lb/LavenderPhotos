package com.kaii.photos.models.custom_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.repositories.CustomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomAlbumViewModel(
    context: Context,
    private val albumInfo: AlbumInfo
) : ViewModel() {
    private val settings = context.appModule.settings

    private val repo = CustomRepository(
        dao = MediaDatabase.getInstance(context.applicationContext).customDao(),
        albumInfo = albumInfo,
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat(),
        info = settings.immich.getImmichBasicInfo()
    )

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)

    val useBlackBackground = context.appModule.settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val confirmToDelete = context.appModule.settings.permissions.getConfirmToDelete().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val doNotTrash = context.appModule.settings.permissions.getDoNotTrash().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val preserveDate = settings.permissions.getPreserveDateOnMove().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
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

    val albums = settings.albums.get().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun remove(items: Set<MediaStoreData>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.remove(items, albumInfo.id)
        }
    }

    suspend fun getMediaCount() = repo.getMediaCount()
    suspend fun getMediaSize(): String {
        val bytes = repo.getMediaSize()

        if (bytes >= 1_000_000_000) {
            return ((bytes.toDouble() / 1_000_000_0).toLong() / 100.0).toString() + " GB"
        }

        return ((bytes.toDouble() / 1_000_0).toLong() / 100.0).toString() + " MB"
    }
}