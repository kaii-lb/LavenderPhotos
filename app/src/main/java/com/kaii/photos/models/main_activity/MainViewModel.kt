package com.kaii.photos.models.main_activity

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainViewModel(
    context: Context,
    var albumInfo: List<AlbumInfo>
) : ViewModel() {
    private var initialMainPhotosPaths = emptySet<String>()

    val settings = Settings(context.applicationContext, viewModelScope)

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

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TopBarDetailsFormat.FileName
    )

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
                }.flatten().toSet() - pair.second
            } else {
                pair.second
            }
        }

    // TODO: should probably move to a background service/worker/something
    /** launch tasks on the mainViewModel scope */
    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        block: suspend () -> Unit
    ) = viewModelScope.launch(dispatcher) {
        block()
    }
}
