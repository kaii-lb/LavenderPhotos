package com.kaii.photos.models.multi_album

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumType
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
    album: AlbumType.Folder
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

    val preserveDate = settings.permissions.getPreserveDateOnMove().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
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

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
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
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val cacheThumbnails = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val thumbnailSize = settings.storage.getThumbnailSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = 256
    )

    val useRoundedCorners = settings.lookAndFeel.getUseRoundedCorners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val useCache = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val autoDetectAlbums = settings.albums.getAutoDetect().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    private val repo =
        MediaRepository(
            dao = MediaDatabase.getInstance(context.applicationContext).mediaDao(),
            scope = viewModelScope,
            initialAlbum = album,
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat
        )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun changePaths(
        album: AlbumType.Folder
    ) = repo.changePaths(new = album.paths)

    suspend fun getMediaCount() = repo.getMediaCount()
    suspend fun getMediaSize(): String {
        val bytes = repo.getMediaSize()

        if (bytes >= 1_000_000_000) {
            return ((bytes.toDouble() / 1_000_000_0).toLong() / 100.0).toString() + " GB"
        }

        return ((bytes.toDouble() / 1_000_0).toLong() / 100.0).toString() + " MB"
    }

    fun editAlbum(id: String, newInfo: AlbumType.Folder) {
        settings.albums.edit(id, newInfo)
    }

    fun removeAlbum(id: String) {
        settings.albums.remove(id)
    }
}

