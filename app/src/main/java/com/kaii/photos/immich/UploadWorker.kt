package com.kaii.photos.immich

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaii.lavender.immichintegration.AlbumManager
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.immichintegration.AssetManager
import com.kaii.lavender.immichintegration.TrashManager
import com.kaii.lavender.immichintegration.serialization.AlbumOrder
import com.kaii.lavender.immichintegration.serialization.DeleteAssets
import com.kaii.lavender.immichintegration.serialization.File
import com.kaii.lavender.immichintegration.serialization.ModifyAlbumAsset
import com.kaii.lavender.immichintegration.serialization.RestoreFromTrash
import com.kaii.lavender.immichintegration.serialization.UpdateAlbumInfo
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.calculateSha1Checksum
import com.kaii.photos.mediastore.MultiAlbumDataSource
import com.kaii.photos.models.multi_album.DisplayDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "com.kaii.photos.immich.UploadWorker"

class UploadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        const val ENDPOINT_BASE = "endpoint_base"
        const val BEARER_TOKEN = "bearer_token"
        const val MEDIASTORE_QUERY = "mediastore_query"
        const val ALBUM_ID = "album_id"
        const val LAVENDER_ALBUM_ID = "lavender_album_id"
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val queryString =
                workerParams.inputData.getString(MEDIASTORE_QUERY)
                    ?: return@withContext Result.failure()
            val endpointBase =
                workerParams.inputData.getString(ENDPOINT_BASE)
                    ?: return@withContext Result.failure()
            val bearerToken =
                workerParams.inputData.getString(BEARER_TOKEN)
                    ?: return@withContext Result.failure()
            val albumId =
                workerParams.inputData.getString(ALBUM_ID) ?: return@withContext Result.failure()
            val lavenderAlbumId =
                workerParams.inputData.getInt(LAVENDER_ALBUM_ID, Int.MAX_VALUE)

            if (lavenderAlbumId == Int.MAX_VALUE) return@withContext Result.failure()

            val apiClient = ApiClient()
            val albumManager = AlbumManager(
                apiClient = apiClient,
                endpointBase = endpointBase,
                bearerToken = bearerToken
            )
            val trashManager = TrashManager(
                apiClient = apiClient,
                endpointBase = endpointBase,
                bearerToken = bearerToken
            )

            val database = MediaDatabase.getInstance(context)

            var immichAlbum = albumManager.getAlbumInfo(albumId = albumId)!!

            val query = Json.decodeFromString<SQLiteQuery>(queryString)
            val dataSource = MultiAlbumDataSource(
                context = context,
                queryString = query,
                sortBy = MediaItemSortMode.DateTaken,
                cancellationSignal = CancellationSignal(),
                displayDateFormat = DisplayDateFormat.Default,
                database = database
            )

            val media = dataSource.query()
            val assetManager = AssetManager(
                apiClient = apiClient,
                endpointBase = endpointBase,
                bearerToken = bearerToken
            )

            val immichAlbumAssetsIds = immichAlbum.assets.map { it.deviceAssetId }

            var shouldBackup = media.mapNotNull { item ->
                val immichFile = File(
                    path = item.absolutePath,
                    size = item.size,
                    dateCreated = item.dateTaken * 1000,
                    lastModified = item.dateModified * 1000
                )

                if (immichFile.path.isEmpty()) return@mapNotNull null

                if (immichAlbumAssetsIds.isEmpty() || !immichAlbumAssetsIds.contains(item.deviceAssetId)) immichFile
                else null
            }

            val successList = mutableListOf<UploadingImmichAsset>()

            try {
                val currentTotal = immichViewModel.immichUploadedMediaTotal.value
                immichViewModel.updatePhotoUploadProgress(
                    uploaded = 0,
                    total = currentTotal + shouldBackup.size,
                    immichId = immichAlbum.id
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Couldn't update UI, immichViewModel inaccessible")
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }

            shouldBackup.forEach { item ->
                // TODO: handle video duration
                Log.d(TAG, "Item to upload $item")

                assetManager.uploadAsset(
                    file = item,
                    deviceId = Build.MODEL
                )?.let {
                    Log.d(TAG, "Upload status for ${item.path} is ${it.status}")
                    successList.add(
                        UploadingImmichAsset(
                            id = it.id,
                            lastModified = item.lastModified
                        )
                    )

                    try {
                        val currentCount = immichViewModel.immichUploadedMediaCount.value
                        val currentTotal = immichViewModel.immichUploadedMediaTotal.value

                        immichViewModel.updatePhotoUploadProgress(
                            uploaded = currentCount + 1,
                            total = currentTotal,
                            immichId = albumId
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "Couldn't update UI, mainViewModel inaccessible")
                        Log.e(TAG, e.toString())
                        e.printStackTrace()
                    }
                }
            }

            Log.d(TAG, "Success list $successList")
            if (successList.isNotEmpty()) {
                val assetInfo = successList.mapNotNull {
                    assetManager.getAssetInfo(it.id)
                }

                val dupes = assetInfo
                    .filter { it.duplicateId != null }
                    .groupBy { it.duplicateId }

                try {
                    trashManager.restoreItems(
                        ids = RestoreFromTrash(
                            ids = assetInfo.filter {
                                it.isTrashed == true
                            }.map { it.id } - assetInfo.filter { it.duplicateId != null }
                                .map { it.id } + dupes.keys.mapNotNull { dupes[it]?.first()?.id }
                        )
                    )

                    val toBeAdded = successList.map { it.id } - assetInfo.filter { it.duplicateId != null }
                        .map { it.id } + dupes.keys.mapNotNull { dupes[it]?.first()?.id }
                    albumManager.addAssetToAlbum(
                        albumId = albumId,
                        assets = ModifyAlbumAsset(
                            ids = toBeAdded
                        )
                    )

                    albumManager.updateAlbumInfo(
                        albumId = albumId,
                        info = UpdateAlbumInfo(
                            albumName = immichAlbum.albumName,
                            albumThumbnailAssetId = successList.maxBy { it.lastModified }.id,
                            description = immichAlbum.description,
                            isActivityEnabled = immichAlbum.isActivityEnabled,
                            order = immichAlbum.order ?: AlbumOrder.Descending
                        )
                    )

                    immichAlbum = albumManager.getAlbumInfo(albumId = albumId)!!

                    val backupMedia = media.map {
                        calculateSha1Checksum(
                            java.io.File(it.absolutePath)
                        )
                    }

                    val allMedia = immichAlbum.assets.filter { asset ->
                        Base64.decode(asset.checksum).toHexString() !in backupMedia
                    }

                    assetManager.deleteAssets(
                        assets = DeleteAssets(
                            ids = allMedia.map { it.id },
                            force = false
                        )
                    )
                } catch (e: Throwable) {
                    Log.e(TAG, e.toString())
                    e.printStackTrace()
                }
            }

            try {
                val currentTotal = immichViewModel.immichUploadedMediaTotal.value

                immichViewModel.updatePhotoUploadProgress(
                    uploaded = 0,
                    total = currentTotal - shouldBackup.size,
                    immichId = immichAlbum.id
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Couldn't update UI, mainViewModel inaccessible")
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }

            return@withContext Result.success()
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            return@withContext Result.failure()
        }
    }
}

private data class UploadingImmichAsset(
    val id: String,
    val lastModified: Long
)
