package com.kaii.photos.data.datasources.trash

import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class NetworkTrashDataSource @Inject constructor(
    private val assetsClient: AssetsClient
) : TrashDataSource {
    private var isCancelled = false

    override fun start(): Flow<List<MediaStoreData>> {
        isCancelled = false

        return flow {
            emit(emptyList())

            while (!isCancelled) {
                emit(query())

                delay(15.seconds)
            }
        }
    }

    override suspend fun query(): List<MediaStoreData> = withContext(Dispatchers.IO) {
        assetsClient.getInTrash()?.map {
            it.toMediaStoreData(
                overrideAbsolutePath = it.originalFileName
            )
        } ?: emptyList()
    }

    override fun cancel() {
        isCancelled = true
    }
}