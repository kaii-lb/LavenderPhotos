package com.kaii.photos.di

import com.kaii.photos.PhotosApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAssetsClient(
        apiClient: ApiClient
    ): AssetsClient = AssetsClient(
        endpoint = "",
        auth = Auth.None,
        client = apiClient
    )

    @Provides
    @Singleton
    fun provideAlbumsClient(
        apiClient: ApiClient
    ): AlbumsClient = AlbumsClient(
        endpoint = "",
        auth = Auth.None,
        client = apiClient
    )

    @Provides
    @Singleton
    fun provideUserClient(
        apiClient: ApiClient
    ): UserClient = UserClient(
        endpoint = "",
        auth = Auth.None,
        client = apiClient
    )

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient = PhotosApplication.appModule.apiClient
}
