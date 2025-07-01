package com.kaii.photos.immich

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kaii.lavender.immichintegration.AlbumManager
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.immichintegration.AssetManager
import com.kaii.lavender.immichintegration.serialization.AlbumOrder
import com.kaii.lavender.immichintegration.serialization.File
import com.kaii.lavender.immichintegration.serialization.ModifyAlbumAsset
import com.kaii.lavender.immichintegration.serialization.UpdateAlbumInfo
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MultiAlbumDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val TAG = "LAVENDER_UPLOAD_WORKER"

class UploadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        const val ENDPOINT_BASE = "endpoint_base"
        const val BEARER_TOKEN = "bearer_token"
        const val MEDIASTORE_QUERY = "mediastore_query"
        const val ALBUM_ID = "album_id"
        const val UPLOADED_COUNT = "uploaded_count"
        const val TOTAL_COUNT = "total_count"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val queryString =
                workerParams.inputData.getString(MEDIASTORE_QUERY)
                    ?: return@withContext Result.failure()
            val endpointBase =
                workerParams.inputData.getString(ENDPOINT_BASE) ?: return@withContext Result.failure()
            val bearerToken =
                workerParams.inputData.getString(BEARER_TOKEN) ?: return@withContext Result.failure()
            val albumId =
                workerParams.inputData.getString(ALBUM_ID) ?: return@withContext Result.failure()

            val apiClient = ApiClient()
            val albumManager = AlbumManager(
                apiClient = apiClient,
                endpointBase = endpointBase,
                bearerToken = bearerToken
            )

            var immichAlbum = albumManager.getAlbumInfo(albumId = albumId)!!

            val query = Json.decodeFromString<SQLiteQuery>(queryString)
            val dataSource = MultiAlbumDataSource(
                context = context,
                queryString = query,
                sortBy = MediaItemSortMode.DateTaken,
                cancellationSignal = CancellationSignal()
            )

            val media = dataSource.query()
            val assetManager = AssetManager(
                apiClient = apiClient,
                endpointBase = endpointBase,
                bearerToken = bearerToken
            )

            val immichAlbumAssetsIds = immichAlbum.assets.map { it.deviceAssetId }

            // TODO: check for existence outside of album
            var shouldBackup = media.mapNotNull { item ->
                val immichFile = File(
                    path = item.absolutePath,
                    size = java.io.File(item.absolutePath).length(),
                    dateCreated = item.dateTaken * 1000,
                    lastModified = item.dateModified * 1000
                )
                val deviceAssetId = "${item.displayName}-${immichFile.size}"

                if (immichFile.path.isEmpty()) return@mapNotNull null

                if (immichAlbumAssetsIds.isEmpty() || !immichAlbumAssetsIds.contains(deviceAssetId)) immichFile
                else null
            }

            var uploadedCount = 0
            var existingCount = 0
            val successList = mutableListOf<Pair<String, Long>>()

            shouldBackup.forEach { item ->
                // TODO: handle video duration
                Log.d(TAG, "Item to upload $item")

                if (!immichAlbumAssetsIds.contains("${item.name}-${item.size}")) {
                    assetManager.uploadAsset(
                        file = item,
                        deviceId = Build.MODEL
                    )?.let {
                        uploadedCount += 1
                        successList.add(Pair(it.id, item.lastModified))
                    }
                } else {
                    existingCount += 1
                }
            }

            if (successList.isNotEmpty()) {
                albumManager.addAssetToAlbum(
                    albumId = albumId,
                    assets = ModifyAlbumAsset(
                        ids = successList.map { it.first }
                    )
                )

                albumManager.updateAlbumInfo(
                    albumId = albumId,
                    info = UpdateAlbumInfo(
                        albumName = immichAlbum.albumName,
                        albumThumbnailAssetId = successList.maxBy { it.second }.first,
                        description = immichAlbum.description,
                        isActivityEnabled = immichAlbum.isActivityEnabled,
                        order = immichAlbum.order ?: AlbumOrder.Descending
                    )
                )
            }

            return@withContext Result.success(
                workDataOf(
                    UPLOADED_COUNT to uploadedCount,
                    TOTAL_COUNT to shouldBackup.size
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            return@withContext Result.failure()
        }
    }
}