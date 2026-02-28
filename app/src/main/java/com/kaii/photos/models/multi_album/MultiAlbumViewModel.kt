package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.repositories.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MultiAlbumViewModel(
    context: Context,
    albumInfo: AlbumInfo
) : ViewModel() {
    private val settings = context.appModule.settings

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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

    val preserveDate = settings.permissions.getPreserveDateOnMove().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
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

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TopBarDetailsFormat.FileName
    )

    val albums = settings.albums.get().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
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

    private val repo =
        MediaRepository(
            dao = MediaDatabase.getInstance(context.applicationContext).mediaDao(),
            initialAlbumInfo = albumInfo,
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat
        )

    val mediaFlow = repo.mediaFlow.cachedIn(viewModelScope)
    val gridMediaFlow = repo.gridMediaFlow.cachedIn(viewModelScope)

    fun changePaths(
        album: AlbumInfo
    ) = repo.changePaths(new = album.paths)

    suspend fun getMediaCount() = repo.getMediaCount()
    suspend fun getMediaSize(): String {
        val bytes = repo.getMediaSize()

        if (bytes >= 1_000_000_000) {
            return ((bytes.toDouble() / 1_000_000_0).toLong() / 100.0).toString() + " GB"
        }

        return ((bytes.toDouble() / 1_000_0).toLong() / 100.0).toString() + " MB"
    }
}

