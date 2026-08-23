package com.kaii.photos.models

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.TaggedItemsDao
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.traits.ClearExifImpl
import com.kaii.photos.models.traits.CopyImpl
import com.kaii.photos.models.traits.DeleteImpl
import com.kaii.photos.models.traits.FavouriteImpl
import com.kaii.photos.models.traits.MoveImpl
import com.kaii.photos.models.traits.PrepareFileForWriteImpl
import com.kaii.photos.models.traits.RenameAlbumImpl
import com.kaii.photos.models.traits.RenameFileImpl
import com.kaii.photos.models.traits.SecureImpl
import com.kaii.photos.models.traits.ShareImpl
import com.kaii.photos.models.traits.TrashImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

abstract class BaseViewModel(
    protected val settings: Settings
) : ViewModel(), CopyImpl, MoveImpl, TrashImpl, DeleteImpl, FavouriteImpl, RenameFileImpl,
    RenameAlbumImpl, SecureImpl, ShareImpl, PrepareFileForWriteImpl, ClearExifImpl
{
    val useBlackBackground by lazy {
        settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val confirmToDelete by lazy {
        settings.permissions.getConfirmToDelete().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val doNotTrash by lazy {
        settings.permissions.getDoNotTrash().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val columnSize by lazy {
        settings.lookAndFeel.getColumnSize().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 3
        )
    }

    val openVideosExternally by lazy {
        settings.behaviour.getOpenVideosExternally().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val cacheThumbnails by lazy {
        settings.storage.getCacheThumbnails().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = true
        )
    }

    val thumbnailSize by lazy {
        settings.storage.getThumbnailSize().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = 256
        )
    }

    val useRoundedCorners by lazy {
        settings.lookAndFeel.getUseRoundedCorners().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val topBarDetailsFormat by lazy {
        settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TopBarDetailsFormat.FileName
        )
    }

    val blurViews by lazy {
        settings.lookAndFeel.getBlurViews().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val useCache by lazy {
        settings.storage.getCacheThumbnails().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val autoDetectAlbums by lazy {
        settings.albums.getAutoDetect().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    val vibrateOnClick by lazy {
        settings.lookAndFeel.getVibrateOnMediaClick().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = true
        )
    }

    val allAlbums by lazy {
        settings.albums.get().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    }

    val sortMode by lazy {
        settings.photoGrid.getSortMode().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MediaItemSortMode.DateTaken
        )
    }

    val immichInfo by lazy {
        settings.immich.getImmichBasicInfo().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = ImmichBasicInfo.Empty
        )
    }

    val useTapToNav by lazy {
        settings.behaviour.getTapToNav().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = false
        )
    }

    protected abstract val progressChannel: Channel<FileOperationProgress<Unit>>
    abstract val fileOperationProgress: Flow<FileOperationProgress<Unit>>

    protected abstract val shareChannel: Channel<Result<Intent, FileOperationError>>
    abstract val fileShareIntent: Flow<Result<Intent, FileOperationError>>

    abstract fun runAction(action: FileOperationAction)

    fun createSelectionManager(
        mediaDao: MediaDao,
        sortMode: MediaItemSortMode,
        paths: Set<String>
    ) = SelectionManager(
        sortMode = sortMode,
        scope = viewModelScope,
        getMediaInDate = { timestamp, sortMode ->
            mediaDao.mediaInDateRange(timestamp = timestamp, paths = paths, dateModified = sortMode.isDateModified)
        }
    )

    fun createSelectionManager(
        taggedItemsDao: TaggedItemsDao,
        sortMode: MediaItemSortMode,
        tagIds: List<Int>
    ) = SelectionManager(
        sortMode = sortMode,
        scope = viewModelScope,
        getMediaInDate = { timestamp, sortMode ->
            if (tagIds.isEmpty()) {
                taggedItemsDao.mediaInDateRangeWithAnyTag(timestamp = timestamp, dateModified = sortMode.isDateModified)
            } else {
                taggedItemsDao.mediaInDateRangeWithLotsOfTags(timestamp = timestamp, dateModified = sortMode.isDateModified, tagIds = tagIds)
            }
        }
    )

    fun createSelectionManager(
        mediaDao: MediaDao,
        sortMode: MediaItemSortMode
    ) = SelectionManager(
        sortMode = sortMode,
        scope = viewModelScope,
        getMediaInDate = { timestamp, sortMode ->
            mediaDao.mediaInDateRange(timestamp = timestamp, dateModified = sortMode.isDateModified)
        }
    )

    fun createSelectionManager(
        customDao: CustomEntityDao,
        sortMode: MediaItemSortMode,
        albumId: String
    ) = SelectionManager(
        sortMode = sortMode,
        scope = viewModelScope,
        getMediaInDate = { timestamp, sortMode ->
            customDao.mediaInDateRange(
                timestamp = timestamp,
                album = albumId,
                dateModified = sortMode.isDateModified
            )
        }
    )
}