package com.kaii.photos.models

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.repositories.FavouritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repo: FavouritesRepository,
    private val mediaDao: MediaDao,
    @param:ApplicationScope private val appScope: CoroutineScope
) : BaseViewModel() {
    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    var selectionManager = SelectionManager(
        sortMode = sortMode.value,
        scope = viewModelScope,
        context = context,
        getMediaInDate = { timestamp ->
            mediaDao.favMediaInDateRange(timestamp = timestamp, dateModified = sortMode.value.isDateModified)
        }
    )
        private set

    override val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    override val fileOperationProgress = progressChannel.receiveAsFlow()

    override val shareChannel = Channel<Result<Intent, FileOperationError>>()
    override val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, Any>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    init {
        viewModelScope.launch {
            sortMode.collect {
                selectionManager = SelectionManager(
                    sortMode = it,
                    scope = viewModelScope,
                    context = context,
                    getMediaInDate = { timestamp ->
                        mediaDao.favMediaInDateRange(timestamp = timestamp, dateModified = it.isDateModified)
                    }
                )
            }
        }
    }

    override fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.Copy -> repo.copyFiles(action.files, action.destination, progressChannel, appScope)
            is FileOperationAction.Move -> repo.moveFiles(action.files, action.destination, action.origin, progressChannel, appScope)
            is FileOperationAction.Trash -> repo.trashFiles(action.files, action.isTrashed, action.album, progressChannel, appScope)
            is FileOperationAction.Delete -> repo.deleteFiles(action.files, action.album, progressChannel, appScope)
            is FileOperationAction.Favourite -> repo.favouriteFiles(action.files, action.isFavourite, action.album, progressChannel, appScope)
            is FileOperationAction.RenameFile -> repo.renameFile(action.file, action.newName, progressChannel, appScope)
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, appScope)
            is FileOperationAction.Secure -> repo.encryptFiles(action.files, progressChannel, appScope)

            is FileOperationAction.LoadExifData -> viewModelScope.launch {
                exifDataState.value = repo.getExifData(action.file)
            }

            else -> Unit
        }
    }
}
