package com.kaii.photos.models

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.Settings
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.formatAsBytes
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.repositories.SAFRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SAFAlbumViewModel.Factory::class)
class SAFAlbumViewModel @AssistedInject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    @Assisted album: AlbumType.SAFFolder,
    repoFactory: SAFRepository.Factory,
    settings: Settings
) : BaseViewModel(settings) {
    @AssistedFactory
    interface Factory {
        fun create(album: AlbumType.SAFFolder): SAFAlbumViewModel
    }

    private val repo = repoFactory.create(
        scope = viewModelScope,
        album = album
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    val selectionManager = SelectionManager(
        sortMode = MediaItemSortMode.DateTaken,
        scope = viewModelScope,
        getMediaInDate = { timestamp, sortMode ->
            repo.mediaInDateRange(timestamp, album.base64TreeUri, sortMode.isDateModified)
        }
    )

    override val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    override val fileOperationProgress = progressChannel.receiveAsFlow()

    override val shareChannel = Channel<Result<Intent, FileOperationError>>()
    override val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, String>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    private val mediaCountState = MutableStateFlow(0)
    val mediaCount = mediaCountState.asStateFlow()

    private val mediaSizeState = MutableStateFlow("")
    val mediaSize = mediaSizeState.asStateFlow()

    fun removeAlbum(id: String) {
        viewModelScope.launch {
            repo.deleteAlbum()
            settings.albums.remove(id)
        }
    }

    fun editAlbum(id: String, newInfo: AlbumType) {
        settings.albums.edit(id, newInfo)
    }

    fun updateShowNested(value: Boolean) {
        repo.updateShowNested(value)
    }

    override fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, progressChannel, appScope)

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