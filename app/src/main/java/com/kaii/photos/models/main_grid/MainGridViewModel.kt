package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MainGridViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

    val mainPhotosAlbums =
        getMainPhotosAlbums().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = runBlocking(Dispatchers.IO) {
                getMainPhotosAlbums().first()
            }
        )

    val defaultTab = settings.defaultTabs.getDefaultTab().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = runBlocking(Dispatchers.IO) {
            settings.defaultTabs.getDefaultTab().first()
        }
    )

    val tabList = settings.defaultTabs.getTabList().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = runBlocking(Dispatchers.IO) {
            settings.defaultTabs.getTabList().first()
        }
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

    val albumSortMode = settings.albums.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AlbumSortMode.LastModifiedDesc
    )

    val alwaysShowImmichInfo = settings.immich.getAlwaysShowUserInfo().stateIn(
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
}