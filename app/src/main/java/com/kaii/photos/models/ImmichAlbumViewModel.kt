package com.kaii.photos.models

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.Settings
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.formatAsBytes
import com.kaii.photos.repositories.ImmichRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(assistedFactory = ImmichAlbumViewModel.Factory::class)
class ImmichAlbumViewModel @AssistedInject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    @Assisted album: AlbumType.Cloud,
    customDao: CustomEntityDao,
    repoFactory: ImmichRepository.Factory,
    settings: Settings
) : BaseViewModel(settings) {
    @AssistedFactory
    interface Factory {
        fun create(album: AlbumType.Cloud): ImmichAlbumViewModel
    }

    val selectionManager = createSelectionManager(customDao, sortMode.value, album.id)

    private val repo = repoFactory.create(
        scope = viewModelScope,
        album = album
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

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

    init {
        viewModelScope.launch {
            launch {
                while (true) {
                    refresh()
                    delay(5000.milliseconds)
                }
            }

            launch {
                sortMode.collect {
                    selectionManager.setSortMode(it)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repo.refresh()
        }
    }

    fun editAlbum(id: String, newInfo: AlbumType) {
        settings.albums.edit(id, newInfo)
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
            is FileOperationAction.RenameAlbum -> repo.renameAlbum(action.album, action.newName, progressChannel, appScope)
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, appScope)

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