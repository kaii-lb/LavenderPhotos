package com.kaii.photos.data.immich

import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.ExifDataDao
import com.kaii.photos.database.entities.toExifData
import com.kaii.photos.database.transactions.TransactionRunner
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlin.uuid.Uuid

class RefreshCloudAlbumOperation(
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    private val exifDataDao: ExifDataDao,
    private val transactionRunner: TransactionRunner,
    private val albumsClient: AlbumsClient,
    private val assetsClient: AssetsClient
) {
    suspend fun execute(
        albumId: String,
        immichId: String
    ): Result<Unit, FileOperationError> {
        val cloudAlbum = albumsClient.get(
            id = Uuid.parse(immichId),
            withoutAssets = false
        ) ?: return Result.Error(FileOperationError.Failed)

        val cloudAssets = assetsClient.getForAlbum(
            albumId = cloudAlbum.id
        ) ?: return Result.Error(FileOperationError.Failed)

        val items = cloudAssets.map { it.toMediaStoreData() }
        val existingIds = customDao.getAllIdsIn(album = albumId).toSet()
        val incomingIds = items.map { it.id }.toSet()
        val added = incomingIds - existingIds
        val deleted = existingIds - incomingIds

        transactionRunner.run {
            mediaDao.upsertAll(items = items)
            customDao.deleteAll(ids = deleted, album = albumId)
            customDao.upsertAll(items = added.map { CustomItem(id = it, album = albumId) })

            exifDataDao.upsertAll(
                items = cloudAssets.mapNotNull {
                    it.exifInfo?.toExifData(
                        mediaId = Uuid.parse(it.id).toLongs { a, _ -> a }
                    )
                }
            )
        }

        return Result.Success(Unit)
    }
}