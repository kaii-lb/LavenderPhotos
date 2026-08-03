package com.kaii.photos.file_management.managers.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError

interface RenameAlbum {
    suspend fun renameAlbum(
        album: AlbumType,
        newName: String
    ): Result<Unit, FileOperationError>
}