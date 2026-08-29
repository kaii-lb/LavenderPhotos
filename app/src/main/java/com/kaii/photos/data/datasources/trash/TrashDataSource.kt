package com.kaii.photos.data.datasources.trash

import com.kaii.photos.database.entities.MediaStoreData
import kotlinx.coroutines.flow.Flow

interface TrashDataSource {
    fun start(): Flow<List<MediaStoreData>>
    suspend fun query(): List<MediaStoreData>
    fun cancel()
}