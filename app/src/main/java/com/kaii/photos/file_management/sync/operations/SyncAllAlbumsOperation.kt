package com.kaii.photos.file_management.sync.operations

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.sync.handlers.CustomSyncHandler
import com.kaii.photos.file_management.sync.handlers.LocalSyncHandler
import kotlinx.coroutines.flow.first

class SyncAllAlbumsOperation(
    private val albums: SettingsAlbumsListImpl,
    private val syncLocal: LocalSyncHandler,
    private val syncCustom: CustomSyncHandler
) {
    suspend fun execute() {
        albums.get().first()
            .filter {
                it.immichId?.isNotBlank() == true && (it is AlbumType.Folder || it is AlbumType.Custom)
            }
            .forEach { album ->
                when (album) {
                    is AlbumType.Folder -> syncLocal.sync(album)
                    is AlbumType.Custom -> syncCustom.sync(album)
                    else -> Unit
                }
            }
    }
}