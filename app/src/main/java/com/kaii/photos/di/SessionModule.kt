package com.kaii.photos.di

import com.kaii.photos.data.immich.ImmichSessionManager
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.ServerClient
import io.github.kaii_lb.lavender.immichintegration.clients.SharedLinkClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideImmichSessionManager(
        assetsClient: AssetsClient,
        albumsClient: AlbumsClient,
        userClient: UserClient,
        loginClient: LoginClient,
        serverClient: ServerClient,
        sharedLinkClient: SharedLinkClient,
        @ApplicationScope appScope: CoroutineScope,
        immich: SettingsImmichImpl
    ): ImmichSessionManager = ImmichSessionManager(
        assetsClient = assetsClient,
        albumsClient = albumsClient,
        userClient = userClient,
        serverClient = serverClient,
        sharedLinkClient = sharedLinkClient,
        info = immich.getImmichBasicInfo(),
        appScope = appScope,
        loginClient = loginClient
    )
}
