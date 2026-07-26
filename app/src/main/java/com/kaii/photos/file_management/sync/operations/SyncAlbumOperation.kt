package com.kaii.photos.file_management.sync.operations

import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.file_management.sync.ProgressManager
import com.kaii.photos.file_management.sync.types.SyncPlanner
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class SyncAlbumOperation(
    private val resolveCandidates: ResolveLocalCandidatesOperation,
    private val planner: SyncPlanner,
    private val upload: UploadMediaOperation,
    private val download: DownloadMediaOperation,
    private val trashLocal: TrashLocalMediaOperation,
    private val mediaDao: MediaDao,
    private val assetsClient: AssetsClient,
    private val progressManager: ProgressManager
) {
    suspend fun execute(
        cloudMedia: List<AssetResponseDto>,
        localMedia: List<MediaStoreData>,
        originAlbumId: String,
        originImmichId: String,
        destinationPath: String
    ): Unit = withContext(Dispatchers.IO) {
        if (cloudMedia.isEmpty() && localMedia.isEmpty()) return@withContext

        val cloudById = cloudMedia.associateBy { it.id }
        val (candidates, sideEffects) = resolveCandidates.execute(localMedia, cloudById)
        val plan = planner.plan(cloudMedia, candidates)

        progressManager.addToTotalItems(plan.toUpload.size + plan.toDownload.size + sideEffects.toTrashLocal.size)

        plan.toLink.forEach { mediaDao.linkToImmich(id = it.localId, hash = it.hash, immichUrl = it.immichUrl) }

        if (plan.toUpload.isNotEmpty()) upload.execute(plan.toUpload, originImmichId)
        if (plan.toDownload.isNotEmpty()) download.execute(plan.toDownload, destinationPath)
        if (sideEffects.toTrashLocal.isNotEmpty()) trashLocal.execute(sideEffects.toTrashLocal, originAlbumId)
        if (sideEffects.toTrashCloud.isNotEmpty()) assetsClient.delete(ids = sideEffects.toTrashCloud.map { Uuid.parse(it) })
    }
}