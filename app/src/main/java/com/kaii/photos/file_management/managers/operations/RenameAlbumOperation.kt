package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import javax.inject.Inject

class RenameAlbumOperation @Inject constructor(
    private val settings: SettingsAlbumsListImpl
) {
    fun execute(
        album: AlbumType,
        newName: String
    ) {
        settings.edit(
            id = album.id,
            newInfo = album.modify(name = newName)
        )
    }
}