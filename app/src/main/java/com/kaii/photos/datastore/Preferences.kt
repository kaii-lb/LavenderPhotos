@file:OptIn(ExperimentalUuidApi::class)

package com.kaii.photos.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.kaii.photos.datastore.preferences.SettingsAlbumsListImpl
import com.kaii.photos.datastore.preferences.SettingsBehaviourImpl
import com.kaii.photos.datastore.preferences.SettingsDebuggingImpl
import com.kaii.photos.datastore.preferences.SettingsDefaultTabsImpl
import com.kaii.photos.datastore.preferences.SettingsEditingImpl
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsLookAndFeelImpl
import com.kaii.photos.datastore.preferences.SettingsPermissionsImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.datastore.preferences.SettingsStorageImpl
import com.kaii.photos.datastore.preferences.SettingsVersionImpl
import com.kaii.photos.datastore.preferences.SettingsVideoImpl
import kotlinx.coroutines.CoroutineScope
import kotlin.uuid.ExperimentalUuidApi

private val datastore = preferencesDataStore(name = "settings")
internal val Context.datastore by datastore

class Settings(val context: Context, val scope: CoroutineScope) {
    val debugging by lazy {
        SettingsDebuggingImpl(context, scope)
    }

    val permissions by lazy {
        SettingsPermissionsImpl(context, scope)
    }

    val albums by lazy {
        SettingsAlbumsListImpl(context, scope)
    }

    val versions by lazy {
        SettingsVersionImpl(context, scope)
    }

    val storage by lazy {
        SettingsStorageImpl(context, scope)
    }

    val video by lazy {
        SettingsVideoImpl(context, scope)
    }

    val lookAndFeel by lazy {
        SettingsLookAndFeelImpl(context, scope)
    }

    val editing by lazy {
        SettingsEditingImpl(context, scope)
    }

    val mainPhotosView by lazy {
        SettingMainPhotosViewImpl(context, scope)
    }

    val defaultTabs by lazy {
        SettingsDefaultTabsImpl(context, scope)
    }

    val photoGrid by lazy {
        SettingsPhotoGridImpl(context, scope)
    }

    val immich by lazy {
        SettingsImmichImpl(context, scope)
    }

    val behaviour by lazy {
        SettingsBehaviourImpl(context, scope)
    }
}