package com.kaii.photos.di

import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGateway
import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGatewayImpl
import com.kaii.photos.file_management.managers.gateways.CloudCacheGateway
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.managers.gateways.SyncWorkerGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GatewayBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAndroidGateway(impl: AndroidMediaStoreGatewayImpl): AndroidMediaStoreGateway

    @Binds
    @Singleton
    abstract fun bindMediaStoreGateway(impl: AndroidMediaStoreGatewayImpl): MediaStoreGateway

    @Binds
    @Singleton
    abstract fun bindCloudCacheGateway(impl: AndroidMediaStoreGatewayImpl): CloudCacheGateway

    @Binds
    @Singleton
    abstract fun bindSyncWorkerGateway(impl: AndroidMediaStoreGatewayImpl): SyncWorkerGateway
}