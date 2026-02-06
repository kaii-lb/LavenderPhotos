package com.kaii.photos.models.main_activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.datastore.Settings
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.Updater
import com.kaii.photos.mediastore.MediaDataSource
import com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.models.MainViewModel"

class MainViewModel(context: Context, var albumInfo: List<AlbumInfo>) : ViewModel() {
    init {
        refreshAlbums(
            context = context,
            albums = albumInfo,
            sortMode = MediaItemSortMode.DateTaken
        )
    }

    // READ_MEDIA_VIDEO isn't necessary as its bundled with READ_MEDIA_IMAGES
    private val permList =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.MANAGE_MEDIA
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_MEDIA
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

    val permissionQueue = mutableStateListOf<String>()

    val settings = Settings(context, viewModelScope)

    val updater = Updater(context = context, coroutineScope = viewModelScope)

    val displayDateFormat = settings.LookAndFeel.getDisplayDateFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DisplayDateFormat.Default
    )

    val sortMode = settings.PhotoGrid.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = MediaItemSortMode.DateTaken
    )

    val columnSize = settings.LookAndFeel.getColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val albumColumnSize = settings.LookAndFeel.getAlbumColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val useBlackViewBackgroundColor = settings.LookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val topBarDetailsFormat = settings.LookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TopBarDetailsFormat.FileName
    )

    val albumsThumbnailsMap = mutableStateMapOf<Int, MediaStoreData>()

    val allAvailableAlbums = settings.AlbumsList.get().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _mainPhotosAlbums = settings.MainPhotosView.getAlbums()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mainPhotosAlbums =
        getMainPhotosAlbums().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private fun getMainPhotosAlbums() =
        allAvailableAlbums.combine(
            combine(
                settings.MainPhotosView.getShowEverything(),
                _mainPhotosAlbums
            ) { showAll, mainPaths -> Pair(showAll, mainPaths) }
        ) { albums, pair ->
            if (pair.first) {
                albums.fastMap { albumInfo ->
                    albumInfo.paths.fastMap { it.removeSuffix("/") }
                }.flatMap { it } - pair.second.toSet()
            } else {
                pair.second
            }
        }

    fun refreshAlbums(
        context: Context,
        albums: List<AlbumInfo>,
        sortMode: MediaItemSortMode
    ) {
        if (albums.toSet() == albumInfo.toSet()) return

        albums.forEach { album ->
            val cancellationSignal = CancellationSignal()

            val media = if (!album.isCustomAlbum) {
                val datasource = MediaDataSource(
                    context = context,
                    cancellationSignal = cancellationSignal
                )

                datasource.query().getOrElse(1) { MediaStoreData.dummyItem }
            } else {
                val datasource = CustomAlbumDataSource(
                    context = context,
                    parentId = album.id,
                    sortMode = sortMode,
                    cancellationSignal = cancellationSignal
                )

                datasource.query().getOrElse(1) { MediaStoreData.dummyItem }
            }

            cancellationSignal.cancel()
            albumsThumbnailsMap[album.id] = media
        }
    }

    fun startupPermissionCheck(context: Context) {
        permList.forEach { perm ->
            val granted = when (perm) {
                Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                    Environment.isExternalStorageManager()
                }

                Manifest.permission.MANAGE_MEDIA -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaStore.canManageMedia(
                        context
                    )
                    else false
                }

                else -> {
                    context.checkSelfPermission(
                        perm
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }

            if (!granted && !permissionQueue.contains(perm)) permissionQueue.add(perm)
            else permissionQueue.remove(perm)

            Log.d(TAG, "Permission $perm has been granted $granted")
        }
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if (!isGranted && !permissionQueue.contains(permission)) permissionQueue.add(permission)
        else if (isGranted) permissionQueue.remove(permission)

        permissionQueue.forEach { Log.d(TAG, "PERMISSION DENIED $it") }
    }

    fun checkCanPass(): Boolean {
        val manageMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionQueue.all { it == Manifest.permission.MANAGE_MEDIA }
        } else {
            false
        }

        return permissionQueue.isEmpty() || manageMedia
    }

    /** launch tasks on the mainViewModel scope */
    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        block: suspend () -> Unit
    ) = viewModelScope.launch(dispatcher) {
        block()
    }
}
