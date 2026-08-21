package com.kaii.photos.data.immich

import com.kaii.photos.datastore.ImmichBasicInfo
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.ServerClient
import io.github.kaii_lb.lavender.immichintegration.clients.SharedLinkClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ImmichSessionManager(
    val assetsClient: AssetsClient,
    val albumsClient: AlbumsClient,
    val userClient: UserClient,
    val loginClient: LoginClient,
    val serverClient: ServerClient,
    val sharedLinkClient: SharedLinkClient,
    private val info: Flow<ImmichBasicInfo>,
    appScope: CoroutineScope
) {
    private val updateChannel = MutableStateFlow(ImmichBasicInfo.Empty)
    val infoUpdates = updateChannel.asStateFlow()

    init {
        appScope.launch {
            info.distinctUntilChanged().collectLatest { info ->
                updateChannel.value = info

                assetsClient.setEndpoint(info.endpoint)
                assetsClient.setAuth(info.auth)

                albumsClient.setEndpoint(info.endpoint)
                albumsClient.setAuth(info.auth)

                userClient.setEndpoint(info.endpoint)
                userClient.setAuth(info.auth)

                loginClient.setEndpoint(info.endpoint)
                loginClient.setAuth(info.auth)

                serverClient.setEndpoint(info.endpoint)
                serverClient.setAuth(info.auth)

                sharedLinkClient.setEndpoint(info.endpoint)
                sharedLinkClient.setAuth(info.auth)
            }
        }
    }
}