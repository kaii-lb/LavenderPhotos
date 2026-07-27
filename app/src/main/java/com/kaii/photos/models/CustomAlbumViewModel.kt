package com.kaii.photos.models

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.formatAsBytes
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.repositories.CustomRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel(assistedFactory = CustomAlbumViewModel.Factory::class)
class CustomAlbumViewModel @Inject constructor(
    @Assisted val selectionManager: SelectionManager,
    private val repo: CustomRepository,
    @param:ApplicationScope private val appScope: CoroutineScope
) : BaseViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(selectionManager: SelectionManager): CustomAlbumViewModel
    }

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    override val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    override val fileOperationProgress = progressChannel.receiveAsFlow()

    override val shareChannel = Channel<Result<Intent, FileOperationError>>()
    override val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, Any>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    private val mediaCountState = MutableStateFlow(0)
    val mediaCount = mediaCountState.asStateFlow()

    private val mediaSizeState = MutableStateFlow("")
    val mediaSize = mediaSizeState.asStateFlow()

    init {
        viewModelScope.launch {
            sortMode.collect {
                selectionManager.setSortMode(it)
            }
        }
    }

    fun editAlbum(id: String, new: AlbumType) {
        settings.albums.edit(id, new)
    }

    fun removeAlbum(id: String) {
        settings.albums.remove(id)
    }

    override fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.Copy -> repo.copyFiles(action.files, action.destination, progressChannel, appScope)
            is FileOperationAction.Move -> repo.moveFiles(action.files, action.destination, action.origin, progressChannel, appScope)
            is FileOperationAction.Trash -> repo.trashFiles(action.files, action.isTrashed, action.album, progressChannel, appScope)
            is FileOperationAction.Delete -> repo.deleteFiles(action.files, action.album, progressChannel, appScope)
            is FileOperationAction.Favourite -> repo.favouriteFiles(action.files, action.isFavourite, action.album, progressChannel, appScope)
            is FileOperationAction.RenameFile -> repo.renameFile(action.file, action.newName, progressChannel, appScope)
            is FileOperationAction.RenameAlbum -> repo.renameAlbum(action.album, action.newName, progressChannel, appScope)
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, appScope)
            is FileOperationAction.Secure -> repo.encryptFiles(action.files, progressChannel, appScope)

            is FileOperationAction.LoadExifData -> viewModelScope.launch {
                exifDataState.value = repo.getExifData(action.file)
            }

            is FileOperationAction.LoadMediaCountAndSize -> viewModelScope.launch {
                mediaCountState.value = repo.getMediaCount(action.album)
                mediaSizeState.value = repo.getMediaSize(action.album).formatAsBytes()
            }

            else -> Unit
        }
    }
}