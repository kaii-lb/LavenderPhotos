package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.BaseViewModel
import com.kaii.photos.repositories.HybridRepository
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginStateManager
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MainGridViewModel(
    context: Context
) : BaseViewModel(context) {
    override val scope: CoroutineScope = context.appModule.scope
    override val apiClient: ApiClient = context.appModule.apiClient

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

    val extraSecureFolderNavEntry = settings.lookAndFeel.getShowExtraSecureNav().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
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

    fun changeAlbum(album: AlbumType.Folder) = repo.changeAlbum(album)

    private val db = MediaDatabase.getInstance(context.applicationContext)
    override val repo =
        HybridRepository(
            db = db,
            client = context.applicationContext.appModule.apiClient,
            scope = viewModelScope,
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat,
            initialAlbum = AlbumType.Folder(
                id = "",
                name = "",
                pinned = false,
                immichId = null,
                paths = mainPhotosAlbums.value
            )
        )

    private val loginState = LoginStateManager(context.appModule.apiClient)

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    init {
        viewModelScope.launch {
            immichInfo.collectLatest {
                loginState.setEndpoint(it.endpoint)
                loginState.setAuth(it.auth)

                val state = loginState.refresh()
                if (state is LoginState.LoggedIn) {
                    settings.immich.setUsername(state.user.name)
                    settings.immich.setUpdatedAt(state.user.updatedAt)
                }
            }
        }
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
                if (!success) {
                    delay(1000)
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
                if (!success) {
                    delay(1000)
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

            repo.setTrashed(context, list, trashed) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                if (!success) {
                    delay(1000)
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