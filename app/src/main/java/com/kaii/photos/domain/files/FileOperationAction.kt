package com.kaii.photos.domain.files

import com.kaii.photos.datastore.AlbumType

sealed interface FileOperationAction {
    data class Copy(
        val files: List<FileOperationItemMetadata>,
        val destination: AlbumType
    ) : FileOperationAction

    data class Move(
        val files: List<FileOperationItemMetadata>,
        val origin: AlbumType,
        val destination: AlbumType
    ) : FileOperationAction

    data class Trash(
        val files: List<FileOperationItemMetadata>,
        val isTrashed: Boolean,
        val album: AlbumType
    ) : FileOperationAction

    data class Delete(
        val files: List<FileOperationItemMetadata>,
        val album: AlbumType
    ) : FileOperationAction

    data class Favourite(
        val files: List<FileOperationItemMetadata>,
        val isFavourite: Boolean,
        val album: AlbumType
    ) : FileOperationAction

    data class RenameFile(
        val file: FileOperationItemMetadata,
        val newName: String
    ) : FileOperationAction

    data class RenameAlbum(
        val album: AlbumType,
        val newName: String
    ) : FileOperationAction

    data class Share(
        val files: List<FileOperationItemMetadata>
    ) : FileOperationAction

    data class PrepareSecure(
        val files: List<FileOperationItemMetadata>
    ) : FileOperationAction

    data class Secure(
        val files: List<FileOperationItemMetadata>
    ) : FileOperationAction

    data class Restore(
        val files: List<FileOperationItemMetadata>
    ) : FileOperationAction

    data class LoadExifData(
        val file: FileOperationItemMetadata
    ) : FileOperationAction

    data class LoadMediaCountAndSize(
        val album: AlbumType
    ) : FileOperationAction

    object ClearSecureFolderCaches : FileOperationAction

    enum class LongOperationType {
        Copy, Move, TrashDelete, TrashRestore,
        Delete, Share, Secure, Restore
    }
}