package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl

class RenameAlbumOperation(
    private val settings: SettingsAlbumsListImpl
) {
    fun execute(
        album: AlbumType,
        newName: String
    ) {
        settings.edit(
            id = album.id,
            newInfo = when (album) {
                is AlbumType.Cloud -> album.copy(name = newName)
                is AlbumType.Custom -> album.copy(name = newName)
                is AlbumType.Folder -> album.copy(name = newName)
                AlbumType.PlaceHolder -> throw IllegalArgumentException("Physically cannot rename ${AlbumType.PlaceHolder::class.qualifiedName}")
            }
        )
    }
}