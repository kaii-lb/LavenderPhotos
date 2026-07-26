package com.kaii.photos.di

import com.kaii.photos.PhotosApplication
import com.kaii.photos.data.immich.ImmichSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
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
        appScope: CoroutineScope
    ): ImmichSessionManager = ImmichSessionManager(
        assetsClient = assetsClient,
        albumsClient = albumsClient,
        userClient = userClient,
        info = PhotosApplication.appModule.settings.immich.getImmichBasicInfo(),
        appScope = appScope
    )
}
