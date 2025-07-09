package com.kaii.photos.models.immich

import android.app.Application
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.immichintegration.serialization.File
import com.kaii.lavender.immichintegration.serialization.LoginCredentials
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBackupMedia
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.SettingsAlbumsListImpl
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.immich.ImmichAlbumDuplicateState
import com.kaii.photos.immich.ImmichAlbumSyncState
import com.kaii.photos.immich.ImmichApiService
import com.kaii.photos.immich.ImmichServerSidedAlbumsState
import com.kaii.photos.immich.ImmichUserLoginState
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

private const val TAG = "IMMICH_VIEW_MODEL"

class ImmichViewModel(
    application: Application,
    private val immichSettings: SettingsImmichImpl,
    private val albumSettings: SettingsAlbumsListImpl
) : AndroidViewModel(application) {
    private val _immichUploadedMediaCount = MutableStateFlow(0)
    val immichUploadedMediaCount = _immichUploadedMediaCount.asStateFlow()

    private val _immichUploadedMediaTotal = MutableStateFlow(0)
    val immichUploadedMediaTotal = _immichUploadedMediaTotal.asStateFlow()

    private val _immichServerAlbums: MutableStateFlow<ImmichServerSidedAlbumsState> =
        MutableStateFlow(ImmichServerSidedAlbumsState.Loading)
    val immichServerAlbums = _immichServerAlbums.asStateFlow()

    private val _immichAlbumsSyncState: MutableStateFlow<Map<String, ImmichAlbumSyncState>> =
        MutableStateFlow(emptyMap())
    val immichAlbumsSyncState = _immichAlbumsSyncState.asStateFlow()

    private val _immichAlbumsDupState: MutableStateFlow<Map<String, ImmichAlbumDuplicateState>> =
        MutableStateFlow(emptyMap())
    val immichAlbumsDupState = _immichAlbumsDupState.asStateFlow()

    private val _immichUserLoginState: MutableStateFlow<ImmichUserLoginState> =
        MutableStateFlow(ImmichUserLoginState.IsNotLoggedIn)
    val immichUserLoginState = _immichUserLoginState.asStateFlow()

    private lateinit var immichApiService: ImmichApiService
    private lateinit var immichEndpoint: String

    init {
        viewModelScope.launch {
            immichSettings.getImmichBasicInfo().collectLatest { immichPrefs ->
                val endpoint = immichPrefs.endpoint
                val token = immichPrefs.bearerToken

                if (endpoint.isNotEmpty() || token.isNotEmpty()) {
                    immichEndpoint = endpoint
                    immichApiService = ImmichApiService(
                        client = ApiClient(),
                        endpoint = endpoint,
                        token = token
                    )
                    refreshAlbums()
                    refreshUserInfo()
                } else {
                    Log.d(TAG, "Immich endpoint or token not configured")
                    immichEndpoint = ""
                    immichApiService = ImmichApiService(
                        client = ApiClient(),
                        endpoint = endpoint,
                        token = token
                    )

                    _immichServerAlbums.value = ImmichServerSidedAlbumsState.Error("Immich endpoint or token not configured.")
                    _immichAlbumsDupState.value = emptyMap()
                    _immichAlbumsSyncState.value = emptyMap()
                    _immichUploadedMediaTotal.value = 0
                    _immichUploadedMediaCount.value = 0
                    _immichUserLoginState.value = ImmichUserLoginState.IsNotLoggedIn
                }
            }
        }
    }

    fun refreshAlbums(
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _immichServerAlbums.value = ImmichServerSidedAlbumsState.Loading
            val result = immichApiService.refreshAlbums()

            result.onSuccess { albums ->
                _immichServerAlbums.value = ImmichServerSidedAlbumsState.Synced(
                    albums = albums.toSet()
                )
            }.onFailure { throwable ->
                _immichServerAlbums.value =
                    ImmichServerSidedAlbumsState.Error(throwable.message.toString())
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = "Failed refreshing albums",
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            onDone()
        }
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    fun checkSyncStatus(immichAlbumId: String, expectedPhotoImmichIds: Set<ImmichBackupMedia>) {
        viewModelScope.launch {
            var snapshot = _immichAlbumsSyncState.value.toMutableMap()

            var possible = snapshot.keys.find { it == immichAlbumId }

            if (possible == null) {
                snapshot.put(
                    key = immichAlbumId,
                    value = ImmichAlbumSyncState.Loading
                )

                possible = immichAlbumId
            }

            var result = immichApiService.checkDifference(
                immichId = immichAlbumId,
                expectedImmichChecksums = expectedPhotoImmichIds.map { it.checksum }.toSet()
            )

            if (result is ImmichAlbumSyncState.OutOfSync) {
                val albumDupes = immichAlbumsDupState.value[immichAlbumId]

                if (albumDupes is ImmichAlbumDuplicateState.HasDupes) {
                    val dupes = albumDupes.dupeAssets
                        .map {
                            Base64.decode(it.checksum).toHexString()
                        }
                        .filter {
                            it in expectedPhotoImmichIds.map { it.checksum }
                        }

                    Log.d(TAG, "Dupes are $dupes")
                    Log.d(TAG, "Result is $result")

                    if (result.missing.minus(dupes).isEmpty()) {
                        result = if (result.extra.isEmpty()) {
                            ImmichAlbumSyncState.InSync(expectedPhotoImmichIds.map { it.checksum }.toSet())
                        } else {
                            ImmichAlbumSyncState.OutOfSync(emptySet(), result.extra)
                        }
                    }
                }
            }

            Log.d(TAG, "Gotten result $result")

            snapshot.remove(immichAlbumId)
            snapshot.put(
                key = immichAlbumId,
                value = result
            )

            _immichAlbumsSyncState.value = snapshot
        }
    }

    fun removeAlbumFromSync(
        albumInfo: AlbumInfo,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _immichServerAlbums.value = ImmichServerSidedAlbumsState.Loading

            val result = immichApiService.removeAlbumFromSync(albumInfo.immichId)
            result.onSuccess { _ ->
                albumSettings.editInAlbumsList(
                    albumInfo = albumInfo,
                    newInfo = albumInfo.copy(
                        immichId = ""
                    )
                )
                refreshAlbums()
                onDone()
            }.onFailure { throwable ->
                onDone()
                _immichServerAlbums.value =
                    ImmichServerSidedAlbumsState.Error(throwable.message.toString())
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = "Failed deleting album",
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }
    }

    fun addAlbumToSync(
        albumInfo: AlbumInfo,
        notificationBody: MutableState<String>,
        notificationPercentage: MutableFloatState,
        onDone: (newId: String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (_immichServerAlbums.value is ImmichServerSidedAlbumsState.Synced) {
                val result = immichApiService.addAlbumToSync(
                    immichId = albumInfo.immichId,
                    albumName = albumInfo.name,
                    currentAlbums = (_immichServerAlbums.value as ImmichServerSidedAlbumsState.Synced).albums.toList(),
                    context = application.applicationContext,
                    query = getSQLiteQuery(albums = albumInfo.paths),
                    albumId = albumInfo.id
                )

                result.onSuccess { id ->
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.ProgressEvent(
                            message = "Syncing albums...",
                            body = notificationBody,
                            icon = R.drawable.cloud_upload,
                            percentage = notificationPercentage
                        )
                    )

                    albumSettings.editInAlbumsList(
                        albumInfo = albumInfo,
                        newInfo = albumInfo.copy(
                            immichId = id
                        )
                    )
                    refreshAlbums()
                    onDone(id)
                }.onFailure { throwable ->
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = "Failed uploading album",
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                    onDone("")
                }
            } else {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = "Failed uploading album",
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
                onDone("")
            }
        }
    }

    fun updatePhotoUploadProgress(
        immichId: String,
        uploaded: Int,
        total: Int
    ) {
        val snapshot = _immichAlbumsSyncState.value.toMutableMap()

        snapshot.apply {
            remove(key = immichId)
            put(
                key = immichId,
                value = ImmichAlbumSyncState.Syncing(
                    uploaded = uploaded,
                    total = total
                )
            )
        }
        _immichUploadedMediaCount.value += uploaded
        _immichUploadedMediaTotal.value += total
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    fun refreshDuplicateState(
        immichId: String,
        media: Set<ImmichBackupMedia>,
        onDone: () -> Unit = {}
    ) = viewModelScope.launch(Dispatchers.IO) {
        refreshAlbums()

        val snapshot = _immichAlbumsDupState.value.toMutableMap()

        val dupes = media
            .groupBy {
                it.checksum
            }
            .flatMap {
                if (it.value.size > 1) it.value else emptyList()
            }

        Log.d(TAG, "Dupes are ${dupes.map { it.checksum }}")

        snapshot[immichId] = if (dupes.isNotEmpty() && immichId != "") {
            ImmichAlbumDuplicateState.HasDupes(dupes)
        } else {
            ImmichAlbumDuplicateState.DupeFree
        }

        _immichAlbumsDupState.value = snapshot

        onDone()
    }

    fun refreshUserInfo(
        onDone: () -> Unit = {}
    ) = viewModelScope.launch {
        val state = immichApiService.getUserInfo()
        if (state != null) {
            val pfp = immichApiService.getProfilePic(state.id)
            if (pfp != null) {
                java.io.File(application.applicationContext.appStorageDir + "/immich_pfp.png").apply {
                    parentFile?.mkdirs()
                    createNewFile()
                    outputStream().use {
                        it.write(pfp)
                    }
                }
            }

            _immichUserLoginState.value =
                ImmichUserLoginState.IsLoggedIn(state.copy(profileImagePath = application.applicationContext.appStorageDir + "/immich_pfp.png"))
        } else {
            _immichUserLoginState.value = ImmichUserLoginState.IsNotLoggedIn
        }

        onDone()
    }

    fun setUsername(
        newName: String
    ) = viewModelScope.launch {
        val success = immichApiService.setUsername(newName)

        if (success) retryRefresh()
    }

    fun setProfilePic(
        file: File
    ) = viewModelScope.launch {
        val success = immichApiService.setProfilePic(file)

        if (success) {
            val pic = java.io.File(file.path)
            java.io.File(application.applicationContext.appStorageDir + "/immich_pfp.png").outputStream().use {
                it.write(pic.readBytes())
            }

            retryRefresh()
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loginUser(
        credentials: LoginCredentials,
        endpointBase: String
    ): Boolean {
        val response = immichApiService.loginUser(credentials)

        if (response != null) {
            immichSettings.setImmichBasicInfo(
                ImmichBasicInfo(
                    endpoint = endpointBase,
                    bearerToken = response.accessToken
                )
            )

            retryRefresh()
            return true
        } else return false
    }

    private suspend fun retryRefresh() {
        refreshUserInfo()
        for (i in 0..3) {
            if (_immichUserLoginState.value is ImmichUserLoginState.IsLoggedIn) break
            delay(1000L * (1 shl i))
            refreshUserInfo()
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun logoutUser() {
        val response = immichApiService.logoutUser()

        if (response != null) {
            immichSettings.setImmichBasicInfo(
                ImmichBasicInfo(
                    endpoint = immichEndpoint,
                    bearerToken = ""
                )
            )

            java.io.File(application.applicationContext.appStorageDir + "/immich_pfp.png").delete()

            refreshUserInfo()
        }
    }

    fun refreshAllFor(
        immichId: String,
        expectedPhotoImmichIds: Set<ImmichBackupMedia>,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        refreshDuplicateState(immichId, expectedPhotoImmichIds)
        refreshAlbums()
        checkSyncStatus(
            immichAlbumId = immichId,
            expectedPhotoImmichIds = expectedPhotoImmichIds
        )
        refreshAlbums {
            onDone()
        }
    }
}