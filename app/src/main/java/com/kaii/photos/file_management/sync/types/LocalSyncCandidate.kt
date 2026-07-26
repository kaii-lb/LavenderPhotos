package com.kaii.photos.file_management.sync.types

import com.kaii.photos.database.entities.MediaStoreData

data class LocalSyncCandidate(
    val media: MediaStoreData,
    val hash: String
)