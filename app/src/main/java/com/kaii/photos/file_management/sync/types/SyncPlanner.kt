package com.kaii.photos.file_management.sync.types

import com.kaii.photos.database.entities.MediaStoreData
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetResponseDto
import javax.inject.Inject
import kotlin.uuid.Uuid

class SyncPlanner @Inject constructor() {
    fun plan(
        cloudMedia: List<AssetResponseDto>,
        candidates: List<LocalSyncCandidate>
    ): SyncPlan {
        val cloudByHash = cloudMedia.associateBy { it.checksum }
        val toUpload = mutableListOf<MediaStoreData>()
        val toLink = mutableListOf<LinkAction>()
        val matchedCloudIds = mutableSetOf<String>()

        candidates.forEach { (media, hash) ->
            val match = cloudByHash[hash]
            if (match != null) {
                toLink.add(LinkAction(media.id, hash, "/api/assets/${match.id}/original"))
                matchedCloudIds.add(match.id)
            } else {
                toUpload.add(media.copy(hash = hash))
            }
        }

        val toDownload = cloudMedia.filter { it.id !in matchedCloudIds }.map { Uuid.parse(it.id) }

        return SyncPlan(toUpload, toDownload, toLink)
    }
}