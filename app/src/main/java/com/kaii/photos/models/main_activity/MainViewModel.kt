package com.kaii.photos.models.main_activity

import android.content.Context
import android.os.CancellationSignal
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.Updater
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.mediastore.content_provider.CustomAlbumDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainViewModel(context: Context, var albumInfo: List<AlbumInfo>) : ViewModel() {
    private var initialMainPhotosPaths = emptySet<String>()

    val apiClient = ApiClient()

    val settings = Settings(context.applicationContext, viewModelScope)

    val updater = Updater(context = context.applicationContext, coroutineScope = viewModelScope)

    val displayDateFormat = settings.lookAndFeel.getDisplayDateFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DisplayDateFormat.Default
    )

    val sortMode = settings.photoGrid.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = MediaItemSortMode.DateTaken
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

    val useBlackViewBackgroundColor = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TopBarDetailsFormat.FileName
    )

    val albumsThumbnailsMap = mutableStateMapOf<Int, MediaStoreData>()

    val allAvailableAlbums = settings.albums.get().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val defaultTab = settings.defaultTabs.getDefaultTab().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = settings.defaultTabs.defaultTabItem
    )

    val tabList = settings.defaultTabs.getTabList().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = settings.defaultTabs.defaultTabList
    )

    private val _mainPhotosAlbums = settings.mainPhotosView.getAlbums()

    init {
        runBlocking {
            initialMainPhotosPaths = getMainPhotosAlbums().first()
        }

        refreshAlbums(
            context = context.applicationContext,
            albums = albumInfo,
            sortMode = MediaItemSortMode.DateTaken
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mainPhotosAlbums =
        getMainPhotosAlbums().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialMainPhotosPaths
        )

    private fun getMainPhotosAlbums() =
        allAvailableAlbums.combine(
            combine(
                settings.mainPhotosView.getShowEverything(),
                _mainPhotosAlbums
            ) { showAll, mainPaths -> Pair(showAll, mainPaths) }
        ) { albums, pair ->
            if (pair.first) {
                albums.fastMap { albumInfo ->
                    albumInfo.paths.map { it.removeSuffix("/") }
                }.flatMap { it }.toSet() - pair.second
            } else {
                pair.second
            }
        }

    // TODO: move to a state holder class
    fun refreshAlbums(
        context: Context,
        albums: List<AlbumInfo>,
        sortMode: MediaItemSortMode
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (albums.toSet() == albumInfo.toSet()) return@launch

        val dao = MediaDatabase.getInstance(context).mediaDao()

        albums.forEach { album ->
            val cancellationSignal = CancellationSignal()

            val media = if (!album.isCustomAlbum) {
                if (sortMode.isDateModified) {
                    dao.getThumbnailForAlbumDateModified(paths = album.paths)
                } else {
                    dao.getThumbnailForAlbumDateTaken(paths = album.paths)
                } ?: MediaStoreData.dummyItem
            } else {
                val datasource = CustomAlbumDataSource(
                    context = context,
                    parentId = album.id,
                    sortMode = sortMode,
                    cancellationSignal = cancellationSignal
                )

                datasource.query().getOrElse(0) { MediaStoreData.dummyItem }
            }

            cancellationSignal.cancel()
            albumsThumbnailsMap[album.id] = media
        }
    }

    /** launch tasks on the mainViewModel scope */
    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        block: suspend () -> Unit
    ) = viewModelScope.launch(dispatcher) {
        block()
    }
}
