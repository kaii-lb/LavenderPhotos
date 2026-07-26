package com.kaii.photos.data.immich

import com.kaii.photos.datastore.ImmichBasicInfo
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ImmichSessionManager(
    apiClient: ApiClient,
    private val info: Flow<ImmichBasicInfo>,
    appScope: CoroutineScope
) {
    val assetsClient = AssetsClient(
        client = apiClient,
        endpoint = "",
        auth = Auth.None
    )

    val albumsClient = AlbumsClient(
        client = apiClient,
        endpoint = "",
        auth = Auth.None
    )

    val userClient = UserClient(
        client = apiClient,
        endpoint = "",
        auth = Auth.None
    )

    init {
        appScope.launch {
            info.distinctUntilChanged().collectLatest { info ->
                assetsClient.setEndpoint(info.endpoint)
                assetsClient.setAuth(info.auth)

                albumsClient.setEndpoint(info.endpoint)
                albumsClient.setAuth(info.auth)

                userClient.setEndpoint(info.endpoint)
                userClient.setAuth(info.auth)
            }
        }
    }
}