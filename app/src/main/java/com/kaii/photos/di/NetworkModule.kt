package com.kaii.photos.di

import com.kaii.photos.BuildConfig
import com.kaii.photos.database.sync.AndroidNetworkMonitor
import com.kaii.photos.database.sync.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.clients.LoginClient
import io.github.kaii_lb.lavender.immichintegration.clients.ServerClient
import io.github.kaii_lb.lavender.immichintegration.clients.UserClient
import io.github.kaii_lb.lavender.immichintegration.clients.buildApiClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindNetworkModule {
    @Binds
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiClient(): ApiClient = buildApiClient(debugMode = BuildConfig.DEBUG)

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
    fun provideLoginClient(
        apiClient: ApiClient
    ): LoginClient = LoginClient(
        endpoint = "",
        auth = Auth.None,
        client = apiClient
    )

    @Provides
    @Singleton
    fun provideServerClient(
        apiClient: ApiClient
    ): ServerClient = ServerClient(
        endpoint = "",
        auth = Auth.None,
        client = apiClient
    )
}
