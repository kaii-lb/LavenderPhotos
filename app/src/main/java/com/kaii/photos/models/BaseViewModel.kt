package com.kaii.photos.models

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.repositories.BaseRepo
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

abstract class BaseViewModel(
    val settings: Settings = PhotosApplication.appModule.settings
) : ViewModel() {
    abstract val scope: CoroutineScope
    abstract val apiClient: ApiClient
    abstract val repo: BaseRepo

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val confirmToDelete = settings.permissions.getConfirmToDelete().stateIn(
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

    val columnSize = settings.lookAndFeel.getColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
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
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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

    val autoDetectAlbums = settings.albums.getAutoDetect().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val vibrateOnClick = settings.lookAndFeel.getVibrateOnMediaClick().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val albums = settings.albums.get().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
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

    val immichInfo = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
    )

    suspend fun getMediaCount() = repo.getMediaCount()
    suspend fun getMediaSize(): String {
        val bytes = repo.getMediaSize()

        if (bytes >= 1_000_000_000) {
            return ((bytes.toDouble() / 1_000_000_0).toLong() / 100.0).toString() + " GB"
        }

        return ((bytes.toDouble() / 1_000_0).toLong() / 100.0).toString() + " MB"
    }

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) = repo.getExifData(context, media)

    fun allowedAlbumTypesFor(moving: Boolean) = repo.allowedAlbumTypesFor(moving)

    abstract fun copy(context: Context, list: List<SelectionManager.SelectedItem>, destination: AlbumType)
    abstract fun move(context: Context, list: List<SelectionManager.SelectedItem>, origin: AlbumType, destination: AlbumType)
    abstract fun setTrashed(context: Context, list: List<SelectionManager.SelectedItem>, trashed: Boolean)
    abstract fun delete(context: Context, list: List<SelectionManager.SelectedItem>)
    abstract fun setFavourite(context: Context, list: List<SelectionManager.SelectedItem>, favourite: Boolean): PendingIntent?
    abstract fun renameAlbum(context: Context, newName: String)
    abstract fun share(context: Context, list: List<SelectionManager.SelectedItem>)
    abstract fun renameItem(context: Context, uri: String, newName: String): IntentSender?
    abstract fun secure(context: Context, list: List<SelectionManager.SelectedItem>)
    abstract fun restore(context: Context, list: List<SelectionManager.SelectedItem>)

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
                origin = action.origin,
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

        is GenericFileManager.Action.RenameAlbum -> {
            renameAlbum(
                context = context,
                newName = action.newName
            )
        }

        is GenericFileManager.Action.Share -> {
            share(
                context = context,
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

        is GenericFileManager.Action.Secure -> {
            secure(
                context = context,
                list = action.list
            )
        }

        is GenericFileManager.Action.Restore -> {
            restore(
                context = context,
                list = action.list
            )
        }

        else -> null
    }

    fun createSelectionManager(
        context: Context,
        sortMode: MediaItemSortMode,
        paths: Set<String>
    ) = SelectionManager(
        sortMode = sortMode,
        scope = viewModelScope,
        context = context,
        getMediaInDate = { timestamp ->
            val dao = MediaDatabase.getInstance(context).mediaDao()

            if (paths.isEmpty()) {
                // search
                dao.mediaInDateRange(timestamp = timestamp, dateModified = sortMode.isDateModified)
            } else {
                dao.mediaInDateRange(timestamp = timestamp, paths = paths, dateModified = sortMode.isDateModified)
            }
        }
    )

    fun createSelectionManager(
        context: Context,
        sortMode: MediaItemSortMode,
        albumId: String
    ) =
        SelectionManager(
            sortMode = sortMode,
            scope = viewModelScope,
            context = context,
            getMediaInDate = { timestamp ->
                val dao = MediaDatabase.getInstance(context).customDao()

                dao.mediaInDateRange(
                    timestamp = timestamp,
                    album = albumId,
                    dateModified = sortMode.isDateModified
                )
            }
        )
}