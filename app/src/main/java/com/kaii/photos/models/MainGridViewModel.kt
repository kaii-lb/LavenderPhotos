package com.kaii.photos.models

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.kaii.photos.data.immich.ImmichSessionManager
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.TaggedItemsDao
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.domain.immich.ImmichLoginState
import com.kaii.photos.domain.news.UpdateState
import com.kaii.photos.file_management.managers.impl.LocalFileManager
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.repositories.HybridRepository
import com.kaii.photos.repositories.LatestNewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@HiltViewModel
class MainGridViewModel @Inject constructor(
    private val mediaDao: MediaDao,
    private val taggedItemsDao: TaggedItemsDao,
    private val latestNewsRepository: LatestNewsRepository,
    private val immichSessionManager: ImmichSessionManager,
    @param:ApplicationScope private val appScope: CoroutineScope,
    repoFactory: HybridRepository.Factory,
    other: LocalFileManager,
    settings: Settings
) : BaseViewModel(settings) {
    val mainPhotosAlbums =
        getMainPhotosAlbums().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    val defaultTab = settings.defaultTabs.getDefaultTab().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = settings.defaultTabs.defaultTabItem
    )

    val tabList = settings.defaultTabs.getTabList().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val exitImmediately = settings.behaviour.getExitImmediately().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val albumColumnSize = settings.lookAndFeel.getAlbumColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val albumSortMode = settings.albums.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlbumSortMode.LastModifiedDesc
    )

    val migrateFav = settings.versions.getMigrateFav().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val groups = settings.albums.getGroups().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    val autoDetect = settings.albums.getAutoDetect().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    var selectionManager by mutableStateOf(createSelectionManager(mediaDao, sortMode.value, mainPhotosAlbums.value))
        private set

    private val repo = repoFactory.create(
        scope = viewModelScope,
        album = AlbumType.Folder(
            id = "",
            name = "",
            pinned = false,
            immichId = null,
            paths = mainPhotosAlbums.value
        ),
        other = other
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    private val _updateStateChannel = Channel<UpdateState>(1)
    val updateStateChannel = _updateStateChannel.receiveAsFlow()

    override val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    override val fileOperationProgress = progressChannel.receiveAsFlow()

    override val shareChannel = Channel<Result<Intent, FileOperationError>>()
    override val fileShareIntent = shareChannel.receiveAsFlow()

    private val exifDataState = MutableStateFlow<Result<Map<MediaData, String>, FileOperationError>>(Result.Error(FileOperationError.Failed))
    val exifData = exifDataState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                immichSessionManager.infoUpdates.collectLatest { info ->
                    if (info.endpoint.isNotBlank() && info.auth.isValid()) {
                        val state = getLoginState()
                        if (state is ImmichLoginState.LoggedIn) {
                            settings.immich.setUsername(state.user.name)
                            settings.immich.setUpdatedAt(state.user.updatedAt)
                        }
                    }
                }
            }

            launch {
                sortMode.collect {
                    selectionManager.setSortMode(it)
                }
            }

            launch {
                settings.versions.getCheckUpdatesOnStartup().collectLatest {
                    if (!it) return@collectLatest

                    _updateStateChannel.send(UpdateState.Loading)

                    if (latestNewsRepository.hasUpdate()) {
                        _updateStateChannel.send(UpdateState.Available)
                    } else {
                        _updateStateChannel.send(UpdateState.NotAvailable)
                    }
                }
            }
        }
    }

    private suspend fun getLoginState() = withContext(Dispatchers.IO) {
        if (!immichSessionManager.loginClient.ping()) {
            return@withContext ImmichLoginState.ServerUnreachable
        }

        val validated = immichSessionManager.loginClient.validate()

        if (!validated) {
            return@withContext ImmichLoginState.LoggedOut
        }

        immichSessionManager.userClient.getMe()?.let {
            ImmichLoginState.LoggedIn(user = it)
        } ?: ImmichLoginState.LoggedOut
    }

    fun changeAlbum(paths: Set<String>) {
        selectionManager.clear()

        selectionManager = createSelectionManager(mediaDao, sortMode.value, paths)

        repo.changeAlbum(
            album = AlbumType.Folder(
                id = "",
                name = "",
                pinned = false,
                immichId = null,
                paths = paths
            )
        )
    }

    fun changeAlbumSearchWithoutTags() {
        selectionManager.clear()
        selectionManager = createSelectionManager(mediaDao, sortMode.value)
    }

    fun changeAlbumSearchWithTags(tagIds: List<Int>) {
        selectionManager.clear()
        selectionManager = createSelectionManager(taggedItemsDao, sortMode.value, tagIds)
    }

    fun setAlbumSortMode(sortMode: AlbumSortMode) = settings.albums.setSortMode(sortMode)
    fun setAlbumOrder(list: List<String>) = settings.albums.setOrder(list)

    fun addAlbum(album: AlbumType) = settings.albums.add(listOf(album))

    @OptIn(ExperimentalUuidApi::class)
    fun addGroup(name: String) = settings.albums.addGroup(
        AlbumGroup(
            id = Uuid.random().toString(),
            name = name,
            pinned = true,
            albumIds = emptyList()
        )
    )

    fun addAlbumToGroup(albumId: String, groupId: String) = viewModelScope.launch {
        val groups = settings.albums.getGroups().first()
        settings.albums.editGroup(
            id = groupId,
            albumIds = groups.first {
                it.id == groupId
            }.albumIds.toMutableList().apply {
                add(albumId)
            }
        )
    }

    fun toggleAlbumPin(
        album: AlbumGridState.Album
    ) {
        viewModelScope.launch {
            if (album is AlbumGridState.Album.Group) {
                settings.albums.editGroup(
                    id = album.id,
                    pinned = !album.pinned
                )
            } else {
                album as AlbumGridState.Album.Single

                settings.albums.edit(
                    id = album.id,
                    newInfo = album.info.album.modify(pinned = !album.pinned)
                )
            }
        }
    }

    fun deleteAlbum(album: AlbumGridState.Album) {
        viewModelScope.launch {
            if (album is AlbumGridState.Album.Group) {
                settings.albums.removeGroup(id = album.id)
            } else {
                settings.albums.remove(albumId = album.id)
            }
        }
    }

    private fun getMainPhotosAlbums() =
        combine(
            settings.albums.get(),
            settings.mainPhotosView.getShowEverything(),
            settings.mainPhotosView.getAlbums()
        ) { albums, showEverything, mainAlbums ->
            if (showEverything) {
                albums.filterIsInstance<AlbumType.Folder>().fastMap { albumInfo ->
                    albumInfo.paths.map { it.removeSuffix("/") }
                }.flatten().toSet() - mainAlbums
            } else {
                mainAlbums
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
            is FileOperationAction.Share -> repo.shareFiles(action.files, shareChannel, progressChannel, appScope)
            is FileOperationAction.PrepareSecure -> repo.prepareEncryptFiles(action.files, progressChannel, appScope)
            is FileOperationAction.Secure -> repo.encryptFiles(action.files, progressChannel, appScope)

            is FileOperationAction.LoadExifData -> viewModelScope.launch {
                exifDataState.value = repo.getExifData(action.file)
            }

            else -> Unit
        }
    }
}