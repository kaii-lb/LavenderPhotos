package com.kaii.photos.models.traits

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.Favourite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface FavouriteImpl {
    fun <T : Favourite> T.favouriteFiles(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        album: AlbumType,
        progressChannel: Channel<FileOperationProgress<Unit>>,
        appScope: CoroutineScope
    ) {
        appScope.launch {
            val result = favouriteFile(
                files = files,
                isFavourite = isFavourite,
                albumId = album.id,
                immichId = album.immichId
            )

            progressChannel.send(
                element = FileOperationProgress.Finished(result = result)
            )
        }
    }
}