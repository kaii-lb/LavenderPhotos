package com.kaii.photos.di

import com.kaii.photos.PhotosApplication
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.datastore.preferences.SettingsBehaviourImpl
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsLookAndFeelImpl
import com.kaii.photos.datastore.preferences.SettingsPermissionsImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.datastore.preferences.SettingsStorageImpl
import com.kaii.photos.datastore.preferences.SettingsVersionImpl
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
    fun provideSettings(): Settings = PhotosApplication.appModule.settings

    @Provides
    @Singleton
    fun provideProgressManager(): ProgressManager = PhotosApplication.appModule.cloudProgressManager

    @Provides
    @Singleton
    fun provideSettingsAlbumsList(): SettingsAlbumsListImpl = PhotosApplication.appModule.settings.albums

    @Provides
    @Singleton
    fun provideSettingsImmichImpl(): SettingsImmichImpl = PhotosApplication.appModule.settings.immich

    @Provides
    @Singleton
    fun provideSettingsPhotoGridImpl(): SettingsPhotoGridImpl = PhotosApplication.appModule.settings.photoGrid

    @Provides
    @Singleton
    fun provideSettingsLookAndFeelImpl(): SettingsLookAndFeelImpl = PhotosApplication.appModule.settings.lookAndFeel

    @Provides
    @Singleton
    fun provideSettingsBehaviourImpl(): SettingsBehaviourImpl = PhotosApplication.appModule.settings.behaviour

    @Provides
    @Singleton
    fun provideSettingsStorageImpl(): SettingsStorageImpl = PhotosApplication.appModule.settings.storage

    @Provides
    @Singleton
    fun provideSettingsPermissionsImpl(): SettingsPermissionsImpl = PhotosApplication.appModule.settings.permissions

    @Provides
    @Singleton
    fun provideSettingsVersionImpl(): SettingsVersionImpl = PhotosApplication.appModule.settings.versions
}
