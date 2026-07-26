package com.kaii.photos.file_management.sync.types

import com.kaii.photos.database.entities.MediaStoreData

data class ReconciliationSideEffects(
    val toTrashLocal: List<MediaStoreData>,
    val toTrashCloud: List<String>
)