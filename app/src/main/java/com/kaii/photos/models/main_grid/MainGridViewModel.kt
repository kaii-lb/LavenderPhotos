package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.domain.immich.ImmichLoginState
import com.kaii.photos.domain.news.UpdateState
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.BaseViewModel
import com.kaii.photos.repositories.HybridRepository
import com.kaii.photos.repositories.LatestNewsRepository
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MainGridViewModel(
    context: Context,
    override val scope: CoroutineScope = PhotosApplication.appModule.scope,
    override val apiClient: ApiClient = PhotosApplication.appModule.apiClient,
    private val db: MediaDatabase = PhotosApplication.appModule.db,
    private val latestNewsRepository: LatestNewsRepository
) : BaseViewModel() {
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

    override val repo = HybridRepository(
        db = db,
        client = apiClient,
        scope = viewModelScope,
        info = settings.immich.getImmichBasicInfo(),
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat(),
        initialAlbum = AlbumType.Folder(
            id = "",
            name = "",
            pinned = false,
            immichId = null,
            paths = mainPhotosAlbums.value
        )
    )

    var selectionManager by mutableStateOf(createSelectionManager(context.applicationContext, sortMode.value, mainPhotosAlbums.value))
        private set

    private val loginClient = LoginClient(
        client = apiClient,
        endpoint = "",
        auth = Auth.None
    )

    private val userClient = UserClient(
        client = apiClient,
        endpoint = "",
        auth = Auth.None
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaFlow = repo.mediaFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val gridMediaFlow = repo.gridMediaFlow

    private val _updateStateChannel = Channel<UpdateState>(1)
    val updateStateChannel = _updateStateChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            launch {
                immichInfo.collectLatest {
                    loginClient.setEndpoint(it.endpoint)
                    loginClient.setAuth(it.auth)

                    userClient.setEndpoint(it.endpoint)
                    userClient.setAuth(it.auth)

                    if (it.endpoint.isNotBlank() && it.auth.isValid()) {
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
        if (!loginClient.ping()) {
            return@withContext ImmichLoginState.ServerUnreachable
        }

        val validated = loginClient.validate()

        if (!validated) {
            return@withContext ImmichLoginState.LoggedOut
        }

        userClient.getMe()?.let {
            ImmichLoginState.LoggedIn(user = it)
        } ?: ImmichLoginState.LoggedOut
    }

    fun changeAlbum(
        context: Context,
        paths: Set<String>
    ) {
        selectionManager.clear()
        selectionManager = createSelectionManager(context.applicationContext, sortMode.value, paths)

        if (paths.isEmpty()) return

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
                    newInfo = when (album.info.album) {
                        is AlbumType.Folder -> album.info.album.copy(pinned = !album.pinned)
                        is AlbumType.Custom -> album.info.album.copy(pinned = !album.pinned)
                        is AlbumType.Cloud -> album.info.album.copy(pinned = !album.pinned)
                        else -> AlbumType.PlaceHolder
                    }
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

    override fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType
    ) {
        scope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_copy_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_copy_snackbar_title),
                    body = body,
                    icon = R.drawable.content_paste,
                    percentage = percentage
                )
            )

            repo.copy(context, list, destination, preserveDate.value, null) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_copy_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                delay(1000.milliseconds)
                if (success) {
                    percentage.floatValue = 1f
                    body.value = context.resources.getString(
                        R.string.media_copy_snackbar_body,
                        list.size, list.size
                    )
                } else {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.media_snackbar_operation_failed),
                            icon = R.drawable.delete,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }

    override fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
        destination: AlbumType
    ) {
        scope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_move_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_move_snackbar_title),
                    body = body,
                    icon = R.drawable.cut,
                    percentage = percentage
                )
            )

            repo.move(context, list, null, destination, preserveDate.value) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_move_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                delay(1000.milliseconds)
                if (success) {
                    percentage.floatValue = 1f
                    body.value = context.resources.getString(
                        R.string.media_copy_snackbar_body,
                        list.size, list.size
                    )
                } else {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.media_snackbar_operation_failed),
                            icon = R.drawable.delete,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = repo.renameItem(context, uri, newName)

    override fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean
    ) {
        scope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_delete_snackbar_title),
                    body = body,
                    icon = R.drawable.delete,
                    percentage = percentage
                )
            )

            repo.setTrashed(context, list, trashed, null, null) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                delay(1000.milliseconds)
                if (success) {
                    percentage.floatValue = 1f
                    body.value = context.resources.getString(
                        R.string.media_copy_snackbar_body,
                        list.size, list.size
                    )
                } else {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.media_snackbar_operation_failed),
                            icon = R.drawable.delete,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }

    override fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            repo.delete(context, list)
        }
    }

    override fun setFavourite(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        favourite: Boolean
    ) = if (list.first().isCloud) {
        scope.launch {
            repo.setFavourite(context, favourite, list)
        }
        null
    } else {
        // this is okay since local media's setFavourite is not a blocking function
        runBlocking {
            repo.setFavourite(context, favourite, list)
        }
    }

    override fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            val isLoading = mutableStateOf(true)

            if (list.any { it.isCloud }) {
                LavenderSnackbarController.pushEvent(
                    event = LavenderSnackbarEvent.LoadingEvent(
                        message = context.resources.getString(R.string.media_sharing),
                        icon = R.drawable.share,
                        isLoading = isLoading
                    )
                )
            }

            repo.share(context, list)
            isLoading.value = false
        }
    }

    override fun renameAlbum(context: Context, newName: String) {
        throw IllegalAccessError("Cannot rename album in a main view!")
    }

    override fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.secure_encrypting),
                    icon = R.drawable.secure_folder,
                    isLoading = isLoading
                )
            )

            repo.secure(context, list)

            isLoading.value = false
        }
    }

    override fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.secure_decrypting),
                    icon = R.drawable.unlock,
                    isLoading = isLoading
                )
            )

            repo.restore(context, list)

            isLoading.value = false
        }
    }
}