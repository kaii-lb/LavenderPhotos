package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

class MainGridViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.appModule.settings
    private var initialMainPhotosPaths = emptySet<String>()


    init {
        runBlocking {
            initialMainPhotosPaths = getMainPhotosAlbums().first()
        }
    }

    val mainPhotosAlbums =
        getMainPhotosAlbums().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialMainPhotosPaths
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

    val exitImmediately = settings.behaviour.getExitImmediately().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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

    val albumSortMode = settings.albums.getAlbumSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlbumSortMode.LastModifiedDesc
    )

    val alwaysShowPfp = settings.immich.getAlwaysShowUserInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val confirmToDelete = settings.permissions.getConfirmToDelete().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
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

    fun setAlbumSortMode(sortMode: AlbumSortMode) = settings.albums.setAlbumSortMode(sortMode)
    fun setAlbums(list: List<AlbumInfo>) = settings.albums.set(list)

    private fun getMainPhotosAlbums() =
        combine(
            settings.albums.get(),
            settings.mainPhotosView.getShowEverything(),
            settings.mainPhotosView.getAlbums()
        ) { albums, showEverything, mainAlbums ->
            if (showEverything) {
                albums.fastMap { albumInfo ->
                    albumInfo.paths.map { it.removeSuffix("/") }
                }.flatten().toSet() - mainAlbums
            } else {
                mainAlbums
            }
        }
}