package com.kaii.photos.models

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.traits.RestoreImpl
import com.kaii.photos.repositories.SecureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecureFolderViewModel @Inject constructor(
    context: Context,
    private val repo: SecureRepository,
    @param:ApplicationScope private val appScope: CoroutineScope
) : BaseViewModel(), RestoreImpl {
    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    val selectionManager = SelectionManager(
        sortMode = MediaItemSortMode.DateModified,
        scope = viewModelScope,
        context = context,
        getMediaInDate = { timestamp ->
            repo.getItemsForDate(timestamp, sortMode.value)
        }
    )

    init {
        repo.attachFileObserver()

        viewModelScope.launch {
            sortMode.collect {
                selectionManager.setSortMode(it)
            }
        }
    }

    override fun onCleared() {
        repo.detachFileObserver()
    }

    override val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    override val fileOperationProgress = progressChannel.receiveAsFlow()

    override val shareChannel = Channel<Result<Intent, FileOperationError>>()
    override val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, Any>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    override fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, appScope)
            is FileOperationAction.Delete -> repo.deleteFiles(action.files, action.album, progressChannel, appScope)
            is FileOperationAction.Restore -> repo.decryptFiles(action.files, progressChannel, appScope)

            is FileOperationAction.LoadExifData -> viewModelScope.launch {
                exifDataState.value = repo.getExifData(action.file)
            }

            else -> Unit
        }
    }
}