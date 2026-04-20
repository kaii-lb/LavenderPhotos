package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.GenericFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.repositories.HybridRepository
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
    context: Context,
    private val scope: CoroutineScope = context.appModule.scope
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

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

    val columnSize = settings.lookAndFeel.getColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val albumColumnSize = settings.lookAndFeel.getAlbumColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val immichInfo = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ImmichBasicInfo.Empty
    )

    val albumSortMode = settings.albums.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlbumSortMode.LastModifiedDesc
    )

    val confirmToDelete = settings.permissions.getConfirmToDelete().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val doNotTrash = settings.permissions.getDoNotTrash().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val preserveDate = settings.permissions.getPreserveDateOnMove().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
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

    val displayDateFormat = settings.lookAndFeel.getDisplayDateFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DisplayDateFormat.Default
    )

    val sortMode = settings.photoGrid.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MediaItemSortMode.DateTaken
    )

    val vibrateOnClick = settings.lookAndFeel.getVibrateOnMediaClick().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TopBarDetailsFormat.FileName
    )

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val useCache = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    fun changeAlbum(album: AlbumType.Folder) = repo.changeAlbum(album)

    private val db = MediaDatabase.getInstance(context.applicationContext)
    private val repo =
        HybridRepository(
            mediaDao = db.mediaDao(),
            customDao = db.customDao(),
            syncTaskDao = db.taskDao(),
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

    private val loginState = LoginStateManager()

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    init {
        val apiClient = context.appModule.apiClient

        viewModelScope.launch {
            immichInfo.collectLatest {
                loginState.setBaseUrl(
                    baseUrl = it.endpoint,
                    apiClient = apiClient
                )

                val state = loginState.refresh(accessToken = it.accessToken)
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

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) = repo.getExifData(context, media)

    fun allowedAlbumTypesFor(moving: Boolean) = repo.allowedAlbumTypesFor(moving)

    fun runAction(
        context: Context,
        action: GenericFileManager.Action
    ) = when (action) {
        is GenericFileManager.Action.Copy -> {
            copy(
                context = context,
                list = action.list,
                destination = action.destination
            )
        }

        is GenericFileManager.Action.Move -> {
            move(
                context = context,
                list = action.list,
                destination = action.destination
            )
        }

        is GenericFileManager.Action.Trash -> {
            setTrashed(
                context = context,
                list = action.list,
                trashed = action.trashed
            )
        }

        is GenericFileManager.Action.Delete -> {
            delete(
                context = context,
                list = action.list
            )
        }

        is GenericFileManager.Action.Favourite -> {
            setFavourite(
                context = context,
                favourite = action.favourite,
                list = action.list
            )
        }

        is GenericFileManager.Action.RenameItem -> {
            renameItem(
                context = context,
                uri = action.uri,
                newName = action.newName
            )
        }

        is GenericFileManager.Action.Share -> {
            share(
                context = context,
                list = action.list
            )
        }

        else -> null
    }

    private fun copy(
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

    private fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
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

    private fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = repo.renameItem(context, uri, newName)

    private fun setTrashed(
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

    private fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            repo.delete(context, list)
        }
    }

    private fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
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

    private fun share(
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
}