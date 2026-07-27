package com.kaii.photos.models.trash_bin

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.Settings
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
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
    context: Context,
    private val repo: TrashRepository,
    settings: Settings = PhotosApplication.appModule.settings,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ViewModel(), TrashImpl, DeleteImpl, ShareImpl, RenameFileImpl {
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
        initialValue = true
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

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TopBarDetailsFormat.FileName
    )

    val useCache = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val vibrateOnClick = settings.lookAndFeel.getVibrateOnMediaClick().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val useTapToNav = settings.behaviour.getTapToNav().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    private val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    val fileOperationProgress = progressChannel.receiveAsFlow()

    private val shareChannel = Channel<Result<Intent, FileOperationError>>()
    val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, Any>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    val selectionManager = SelectionManager(
        sortMode = MediaItemSortMode.DateModified,
        scope = viewModelScope,
        context = context,
        getMediaInDate = { timestamp ->
            repo.getItemsForDate(timestamp, MediaItemSortMode.DateModified)
        }
    )

    override fun onCleared() {
        repo.cancel()
    }

    fun cancel() = repo.cancel()

    fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.Trash -> repo.trashFiles(action.files, action.isTrashed, action.album, progressChannel, appScope)
            is FileOperationAction.Delete -> repo.deleteFiles(action.files, action.album, progressChannel, appScope)
            is FileOperationAction.RenameFile -> repo.renameFile(action.file, action.newName, progressChannel, appScope)
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, appScope)

            is FileOperationAction.LoadExifData -> viewModelScope.launch {
                exifDataState.value = repo.getExifData(action.file)
            }

            else -> Unit
        }
    }

    fun deleteAll() {
        appScope.launch {
            repo.deleteAll()
        }
    }
}
