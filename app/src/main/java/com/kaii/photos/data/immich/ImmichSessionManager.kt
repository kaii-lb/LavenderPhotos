package com.kaii.photos.data.immich

import com.kaii.photos.datastore.ImmichBasicInfo
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ImmichSessionManager(
    val assetsClient: AssetsClient,
    val albumsClient: AlbumsClient,
    val userClient: UserClient,
    private val info: Flow<ImmichBasicInfo>,
    appScope: CoroutineScope
) {
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