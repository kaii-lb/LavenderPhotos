package com.kaii.photos.file_management.sync.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.file_management.sync.types.LocalSyncCandidate
import com.kaii.photos.file_management.sync.types.ReconciliationSideEffects
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetResponseDto
import java.io.File

class ResolveLocalCandidatesOperation(
    private val mediaDao: MediaDao
) {
    suspend fun execute(
        localMedia: List<MediaStoreData>,
        cloudById: Map<String, AssetResponseDto>
    ): Pair<List<LocalSyncCandidate>, ReconciliationSideEffects> {
        val toTrashLocal = mutableListOf<MediaStoreData>()
        val toTrashCloud = mutableListOf<String>()
        val candidates = mutableListOf<LocalSyncCandidate>()

        localMedia.forEach { local ->
            var hash = local.hash ?: calculateSha1Checksum(File(local.absolutePath))

            if (local.immichId != null) {
                val cloudItem = cloudById[local.immichId]

                if (cloudItem?.isTrashed == true) {
                    toTrashLocal.add(local)
                    return@forEach
                }

                if (cloudItem != null && local.dateModified != cloudItem.toMediaStoreData().dateModified) {
                    toTrashCloud.add(cloudItem.id)
                    hash = calculateSha1Checksum(File(local.absolutePath))
                }
            }

            mediaDao.linkToHash(id = local.id, hash = hash)
            candidates.add(LocalSyncCandidate(local, hash))
        }

        return candidates to ReconciliationSideEffects(toTrashLocal, toTrashCloud)
    }
}