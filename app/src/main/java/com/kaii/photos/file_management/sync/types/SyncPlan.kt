package com.kaii.photos.file_management.sync.types

import com.kaii.photos.database.entities.MediaStoreData
import kotlin.uuid.Uuid

data class SyncPlan(
    val toUpload: List<MediaStoreData>,
    val toDownload: List<Uuid>,
    val toLink: List<LinkAction>
)
