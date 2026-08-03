package com.kaii.photos.di

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideSettings(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): Settings = Settings(
        context = context,
        scope = scope
    )

    @Provides
    @Singleton
    fun provideProgressManager(
        @ApplicationScope scope: CoroutineScope,
        settings: SettingsImmichImpl
    ): ProgressManager = ProgressManager(
        scope = scope,
        settings = settings
    )

    @Provides
    @Singleton
    fun provideSettingsAlbumsList(settings: Settings): SettingsAlbumsListImpl = settings.albums

    @Provides
    @Singleton
    fun provideSettingsImmichImpl(settings: Settings): SettingsImmichImpl = settings.immich

    @Provides
    @Singleton
    fun provideSettingsPhotoGridImpl(settings: Settings): SettingsPhotoGridImpl = settings.photoGrid

    @Provides
    @Singleton
    fun provideSettingsLookAndFeelImpl(settings: Settings): SettingsLookAndFeelImpl = settings.lookAndFeel

    @Provides
    @Singleton
    fun provideSettingsBehaviourImpl(settings: Settings): SettingsBehaviourImpl = settings.behaviour

    @Provides
    @Singleton
    fun provideSettingsStorageImpl(settings: Settings): SettingsStorageImpl = settings.storage

    @Provides
    @Singleton
    fun provideSettingsPermissionsImpl(settings: Settings): SettingsPermissionsImpl = settings.permissions

    @Provides
    @Singleton
    fun provideSettingsVersionImpl(settings: Settings): SettingsVersionImpl = settings.versions
}
