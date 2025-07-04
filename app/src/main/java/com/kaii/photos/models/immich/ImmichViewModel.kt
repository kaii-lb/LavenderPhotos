package com.kaii.photos.models.immich

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.snap
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.lavender.immichintegration.ApiClient
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.SettingsImmichImpl
import com.kaii.photos.immich.ImmichAlbumDuplicateState
import com.kaii.photos.immich.ImmichAlbumSyncState
import com.kaii.photos.immich.ImmichApiService
import com.kaii.photos.immich.ImmichServerSidedAlbumsState
import com.kaii.photos.mediastore.getSQLiteQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "IMMICH_VIEW_MODEL"

class ImmichViewModel(
    private val immichSettings: SettingsImmichImpl
) : ViewModel() {
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
    val immichAlbumsDupState = _immichAlbumsSyncState.asStateFlow()

    private lateinit var immichApiService: ImmichApiService

    init {
        viewModelScope.launch {
            immichSettings.getImmichBasicInfo().collectLatest { immichPrefs ->
                val endpoint = immichPrefs.endpoint
                val token = immichPrefs.bearerToken

                if (endpoint.isNotEmpty() && token.isNotEmpty()) {
                    immichApiService = ImmichApiService(
                        client = ApiClient(),
                        endpoint = endpoint,
                        token = token
                    )
                    refreshAlbums()
                } else {
                    Log.d(TAG, "Immich endpoint or token not configured")
                    _immichServerAlbums.value =
                        ImmichServerSidedAlbumsState.Error("Immich endpoint or token not configured.")
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

    fun checkSyncStatus(immichAlbumId: String, expectedPhotoImmichIds: Set<String>) {
        viewModelScope.launch {
            var snapshot = _immichAlbumsSyncState.value.toMutableMap()

            var possible = snapshot.keys.find { it == immichAlbumId }

            if (possible == null) {
                snapshot = snapshot
                    .toMutableMap()
                    .apply {
                        put(
                            key = immichAlbumId,
                            value = ImmichAlbumSyncState.Loading
                        )
                    }

                possible = immichAlbumId
            }

            val result = immichApiService.checkDifference(
                immichId = immichAlbumId,
                expectedImmichIds = expectedPhotoImmichIds
            )

            Log.d(TAG, "Gotten result $result")

            snapshot = snapshot
                .toMutableMap()
                .apply {
                    remove(key = immichAlbumId)
                    put(
                        key = immichAlbumId,
                        value = result
                    )
                }

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
                mainViewModel.settings.AlbumsList.editInAlbumsList(
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
        context: Context,
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
                    context = context,
                    query = getSQLiteQuery(albums = albumInfo.paths)
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

                    mainViewModel.settings.AlbumsList.editInAlbumsList(
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

    fun setDuplicateState(
        immichId: String,
        state: ImmichAlbumDuplicateState
    ) {
        val snapshot = _immichAlbumsDupState.value.toMutableMap()

        snapshot[immichId] = state

        _immichAlbumsDupState.value = snapshot
    }
}