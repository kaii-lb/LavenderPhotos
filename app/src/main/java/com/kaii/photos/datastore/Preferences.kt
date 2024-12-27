package com.kaii.photos.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val Settings.Debugging: SettingsLogsImpl
    get() = SettingsLogsImpl(context, viewModelScope)

val Settings.Permissions: SettingsPermissionsImpl
    get() = SettingsPermissionsImpl(context, viewModelScope)


val Settings.TrashBin: SettingsTrashBinImpl
    get() = SettingsTrashBinImpl(context, viewModelScope)

val Settings.AlbumsList: SettingsAlbumsListImpl
    get() = SettingsAlbumsListImpl(context, viewModelScope)

val Settings.Versions: SettingsVersionImpl
    get() = SettingsVersionImpl(context, viewModelScope)

val Settings.User: SettingsUserImpl
    get() = SettingsUserImpl(context, viewModelScope)

val Settings.Storage: SettingsStorageImpl
    get() = SettingsStorageImpl(context, viewModelScope)

val Settings.Video: SettingsVideoImpl
    get() = SettingsVideoImpl(context, viewModelScope)

val Settings.LookAndFeel: SettingsLookAndFeelImpl
    get() = SettingsLookAndFeelImpl(context, viewModelScope)
