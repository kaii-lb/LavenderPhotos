package com.kaii.photos.immich

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastMap
import com.kaii.lavender.immichintegration.AlbumManager
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.immichintegration.serialization.Album
import com.kaii.lavender.immichintegration.serialization.CreateAlbum
import com.kaii.photos.datastore.SQLiteQuery
import kotlinx.coroutines.delay

private const val TAG = "IMMICH_API_SERVICE"

class ImmichApiService(
    client: ApiClient,
    private val endpoint: String,
    private val token: String
) {
    companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 500
    }

    private val albumManager =
        AlbumManager(
            apiClient = client,
            endpointBase = endpoint,
            bearerToken = token
        )

    suspend fun getAllAlbums(): List<Album>? {
        try {
            return albumManager.getAllAlbums()
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            return null
        }
    }

    suspend fun refreshAlbums(): Result<List<Album>> {
        for (i in 0..(MAX_RETRIES - 1)) {
            val new = getAllAlbums()

            if (new != null) return Result.success(new)

            val delay = INITIAL_BACKOFF_MS * 1 shl i
            delay(delay.toLong())
        }

        return Result.failure(
            Exception(
                "Unable to fetch albums!"
            )
        )
    }

    suspend fun getAlbumInfo(
        immichId: String
    ): Album? {
        try {
            return albumManager.getAlbumInfo(
                albumId = immichId
            )
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            return null
        }
    }

    suspend fun checkDifference(
        immichId: String,
        expectedImmichIds: Set<String>
    ): ImmichAlbumSyncState {
        try {
            val remoteAlbum = getAlbumInfo(immichId = immichId)

            if (remoteAlbum == null) return ImmichAlbumSyncState.Error("Album $immichId does not exist!")

            val remotePhotoIds = remoteAlbum.assets.map { it.deviceAssetId }.toSet()

            val isInSync = remotePhotoIds == expectedImmichIds

            return if (isInSync) {
                ImmichAlbumSyncState.InSync(remotePhotoIds)
            } else {
                ImmichAlbumSyncState.OutOfSync(
                    missing = expectedImmichIds.minus(remotePhotoIds),
                    extra = remotePhotoIds.minus(expectedImmichIds)
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            return ImmichAlbumSyncState.Error(e.toString())
        }
    }

    suspend fun removeAlbumFromSync(
        immichId: String
    ) = if (albumManager.deleteAlbum(albumId = immichId)) Result.success(immichId)
    else Result.failure(Exception("Cannot delete album"))

    suspend fun addAlbumToSync(
        immichId: String,
        albumName: String,
        currentAlbums: List<Album>,
        context: Context,
        query: SQLiteQuery
    ): Result<String> {
        var newId = immichId
        Log.d(TAG, "Current albums $currentAlbums")
        Log.d(TAG, "Requested albums $immichId")
        if (immichId !in currentAlbums.fastMap { it.id }) {
            val new = albumManager.createAlbum(
                album = CreateAlbum(
                    albumName = albumName,
                    albumUsers = emptyList(),
                    assetIds = emptyList(),
                    description = ""
                )
            )

            if (new == null) return Result.failure(Exception("Failed creating server side album!"))
            newId = new.id
        }

        SchedulingManager.scheduleUploadTask(
            context = context,
            immichAlbumId = newId,
            immichEndpointBase = endpoint,
            immichBearerToken = token,
            mediastoreQuery = query
        )

        return Result.success(newId)
    }
}

sealed class ImmichAlbumSyncState {
    object Loading : ImmichAlbumSyncState()

    data class OutOfSync(
        val missing: Set<String>,
        val extra: Set<String>
    ) : ImmichAlbumSyncState()

    data class InSync(
        val ids: Set<String>
    ) : ImmichAlbumSyncState()

    data class Error(
        val message: String
    ) : ImmichAlbumSyncState()

    data class Syncing(
        val uploaded: Int,
        val total: Int
    ) : ImmichAlbumSyncState()
}

sealed class ImmichServerSidedAlbumsState {
    object Loading : ImmichServerSidedAlbumsState()

    data class Synced(
        val albums: Set<Album>
    ) : ImmichServerSidedAlbumsState()

    data class Error(
        val message: String
    ) : ImmichServerSidedAlbumsState()
}

sealed class ImmichAlbumDuplicateState {
    object DupeFree : ImmichAlbumDuplicateState()

    data class HasDupes(
        val deviceAssetIds: Set<String>
    ) : ImmichAlbumDuplicateState()
}