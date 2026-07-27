package com.kaii.photos.models

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.search.SearchManager
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
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
class SearchViewModel @Inject constructor(
    private val repo: SearchRepository,
    private val searchManager: SearchManager,
    @param:ApplicationScope private val appScope: CoroutineScope
) : BaseViewModel() {
    init {
        viewModelScope.launch {
            displayDateFormat.collect { searchManager.update(format = it) }
        }

        viewModelScope.launch {
            sortMode.collect { searchManager.update(sortMode = it) }
        }

        viewModelScope.launch {
            immichInfo.collect { searchManager.update(info = it) }
        }
    }

    val mediaFlow = searchManager.mediaFlow
    val gridMediaFlow = searchManager.gridMediaFlow

    override val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    override val fileOperationProgress = progressChannel.receiveAsFlow()

    override val shareChannel = Channel<Result<Intent, FileOperationError>>()
    override val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, String>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    val tags = searchManager.tags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    val searchQuery = searchManager.searchQuery.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ""
    )

    val searchMode = searchManager.searchMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SearchMode.Name
    )

    val searchingForTags = searchManager.searchingForTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val selectedTags = searchManager.selectedTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun search(
        query: String
    ) = searchManager.search(query)

    fun setSearchMode(mode: SearchMode) = searchManager.setSearchMode(mode)
    fun setSearchingForTags(value: Boolean) = searchManager.setSearchingForTags(value)
    fun toggleTagSelected(tag: Tag) = searchManager.toggleTagSelected(tag)
    fun clearSelectedTags() = searchManager.clearSelectedTags()

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            searchManager.deleteTag(tag)
        }
    }

    fun clear() = searchManager.clear()

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

