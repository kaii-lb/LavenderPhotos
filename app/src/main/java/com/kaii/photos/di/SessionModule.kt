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
        appScope: CoroutineScope,
        immich: SettingsImmichImpl
    ): ImmichSessionManager = ImmichSessionManager(
        assetsClient = assetsClient,
        albumsClient = albumsClient,
        userClient = userClient,
        info = immich.getImmichBasicInfo(),
        appScope = appScope,
        loginClient = loginClient
    )
}
