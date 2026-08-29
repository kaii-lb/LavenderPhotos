package com.kaii.photos.models

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsBehaviourImpl
import com.kaii.photos.datastore.preferences.SettingsLookAndFeelImpl
import com.kaii.photos.datastore.preferences.SettingsStorageImpl
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.traits.ClearExifImpl
import com.kaii.photos.models.traits.DeleteImpl
import com.kaii.photos.models.traits.RenameFileImpl
import com.kaii.photos.models.traits.ShareImpl
import com.kaii.photos.models.traits.TrashImpl
import com.kaii.photos.repositories.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    repoFactory: TrashRepository.Factory,
    lookAndFeel: SettingsLookAndFeelImpl,
    behaviour: SettingsBehaviourImpl,
    storage: SettingsStorageImpl
) : ViewModel(), TrashImpl, DeleteImpl, ShareImpl, RenameFileImpl, ClearExifImpl {
    val columnSize = lookAndFeel.getColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val openVideosExternally = behaviour.getOpenVideosExternally().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val cacheThumbnails = storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val thumbnailSize = storage.getThumbnailSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = 256
    )

    val useRoundedCorners = lookAndFeel.getUseRoundedCorners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val useBlackBackground = lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val blurViews = lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val topBarDetailsFormat = lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TopBarDetailsFormat.FileName
    )

    val useCache = storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val vibrateOnClick = lookAndFeel.getVibrateOnMediaClick().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val useTapToNav = behaviour.getTapToNav().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    private val repo = repoFactory.create(scope = viewModelScope)

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    private val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    val fileOperationProgress = progressChannel.receiveAsFlow()

    private val shareChannel = Channel<Result<Intent, FileOperationError>>()
    val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, String>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    val selectionManager = SelectionManager(
        sortMode = MediaItemSortMode.DateModified,
        scope = viewModelScope,
        getMediaInDate = { timestamp, sortMode ->
            repo.getItemsForDate(timestamp, sortMode)
        }
    )

    override fun onCleared() {
        repo.cancel()
    }

    fun start() = repo.start()
    fun cancel() = repo.cancel()

    fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.Trash -> repo.trashFiles(action.files, action.isTrashed, action.album, progressChannel, appScope)
            is FileOperationAction.Delete -> repo.deleteFiles(action.files, action.album, progressChannel, appScope)
            is FileOperationAction.RenameFile -> repo.renameFile(action.file, action.newName, progressChannel, appScope)
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, progressChannel, appScope)
            is FileOperationAction.ClearExifData -> repo.clearExifData(action.absolutePath, progressChannel, appScope)

            is FileOperationAction.LoadExifData -> viewModelScope.launch {
                exifDataState.value = repo.getExifData(action.file)
            }

            else -> Unit
        }
    }

    fun deleteAll() {
        appScope.launch {
            val files = repo.getAllFiles()
            repo.deleteFiles(
                files = files,
                album = AlbumType.PlaceHolder,
                progressChannel = progressChannel,
                appScope = appScope
            )
        }
    }
}
