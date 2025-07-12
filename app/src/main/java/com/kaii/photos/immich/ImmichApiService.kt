package com.kaii.photos.immich

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastMap
import com.kaii.lavender.immichintegration.AlbumManager
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.immichintegration.UserAuth
import com.kaii.lavender.immichintegration.UserManager
import com.kaii.lavender.immichintegration.serialization.Album
import com.kaii.lavender.immichintegration.serialization.CreateAlbum
import com.kaii.lavender.immichintegration.serialization.File
import com.kaii.lavender.immichintegration.serialization.LoginCredentials
import com.kaii.lavender.immichintegration.serialization.UserFull
import com.kaii.photos.datastore.ImmichBackupMedia
import com.kaii.photos.datastore.SQLiteQuery
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "IMMICH_API_SERVICE"

class ImmichApiService(
    client: ApiClient,
    val endpoint: String,
    val token: String
) {
    companion object {
        const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 500
    }

    private val albumManager =
        AlbumManager(
            apiClient = client,
            endpointBase = endpoint,
            bearerToken = token
        )

    private val userManager =
        UserManager(
            apiClient = client,
            endpointBase = endpoint,
            bearerToken = token
        )

    private val userAuth =
        UserAuth(
            apiClient = client,
            endpointBase = endpoint
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
                albumId = immichId,
                withoutAssets = false
            )
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            return null
        }
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    suspend fun checkDifference(
        immichId: String,
        expectedImmichBackupMedia: Set<ImmichBackupMedia>
    ): ImmichAlbumSyncState {
        try {
            val remoteAlbum = getAlbumInfo(immichId = immichId)

            if (remoteAlbum == null) return ImmichAlbumSyncState.Error("Album $immichId does not exist!")

            val remoteBackupMedia = remoteAlbum.assets.map {
                ImmichBackupMedia(
                    deviceAssetId = it.deviceAssetId,
                    absolutePath = "",
                    checksum = Base64.decode(it.checksum).toHexString()
                )
            }.toSet()

            val isInSync = remoteBackupMedia.map { it.deviceAssetId } == expectedImmichBackupMedia.map { it.deviceAssetId }

            return if (isInSync) {
                ImmichAlbumSyncState.InSync(expectedImmichBackupMedia)
            } else {
                ImmichAlbumSyncState.OutOfSync(
                    missing = expectedImmichBackupMedia.filter { it.deviceAssetId !in remoteBackupMedia.map { it.deviceAssetId } }.toSet(),
                    extra = remoteBackupMedia.filter { it.deviceAssetId !in expectedImmichBackupMedia.map { it.deviceAssetId } }.toSet()
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
        albumId: Int,
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

        Log.d(TAG, "Ended up with new id $newId")

        SchedulingManager.scheduleUploadTask(
            lavenderAlbumId = albumId,
            context = context,
            immichAlbumId = newId,
            immichEndpointBase = endpoint,
            immichBearerToken = token,
            mediastoreQuery = query
        )

        return Result.success(newId)
    }

    suspend fun getUserInfo() = try {
        userManager.getMyUser()
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
        null
    }

    suspend fun setUsername(
        newName: String
    ) = try {
        userManager.changeName(newName) != null
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()

        false
    }

    suspend fun getProfilePic(
        userId: String
    ) = try {
        userManager.getProfilePic(userId)
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()
        null
    }

    suspend fun setProfilePic(
        file: File
    ) = try {
        userManager.createProfilePic(file) != null
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()

        false
    }

    suspend fun loginUser(
        credentials: LoginCredentials
    ) = try {
        userAuth.login(credentials)
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()

        null
    }

    suspend fun logoutUser() = try {
        userAuth.logout(token)?.successful
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
        e.printStackTrace()

        false
    }
}

sealed class ImmichAlbumSyncState {
    object Loading : ImmichAlbumSyncState()

    data class OutOfSync(
        val missing: Set<ImmichBackupMedia>,
        val extra: Set<ImmichBackupMedia>
    ) : ImmichAlbumSyncState()

    data class InSync(
        val ids: Set<ImmichBackupMedia>
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

@Serializable
sealed class ImmichAlbumDuplicateState {
    @Serializable
    object DupeFree : ImmichAlbumDuplicateState()

    @Serializable
    data class HasDupes(
        val dupeAssets: List<ImmichBackupMedia>
    ) : ImmichAlbumDuplicateState()
}

sealed class ImmichUserLoginState {
    data class IsLoggedIn(val info: UserFull) : ImmichUserLoginState()

    object IsNotLoggedIn : ImmichUserLoginState()
}