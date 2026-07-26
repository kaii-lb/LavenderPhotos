package com.kaii.photos.di

import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.file_management.sync.ProgressManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsAlbumsList(): SettingsAlbumsListImpl = PhotosApplication.appModule.settings.albums

    @Provides
    @Singleton
    fun provideProgressManager(): ProgressManager = PhotosApplication.appModule.cloudProgressManager
}
