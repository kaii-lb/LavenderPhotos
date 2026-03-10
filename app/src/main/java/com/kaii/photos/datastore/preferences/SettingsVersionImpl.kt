package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.kaii.photos.datastore.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsVersionImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val showUpdateNotice = booleanPreferencesKey("show_update_notice")
    private val checkForUpdatesOnStartup = booleanPreferencesKey("check_for_updates_on_startup")
    private val clearGlideCache = booleanPreferencesKey("version_clear_glide_cache")
    private val migrateFav = booleanPreferencesKey("version_migrate_fav")

    fun getShowUpdateNotice(): Flow<Boolean> =
        context.datastore.data.map {
            it[showUpdateNotice] != false
        }

    fun setShowUpdateNotice(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[showUpdateNotice] = value
        }
    }

    fun getCheckUpdatesOnStartup(): Flow<Boolean> =
        context.datastore.data.map {
            it[checkForUpdatesOnStartup] == true
        }

    fun setCheckUpdatesOnStartup(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[checkForUpdatesOnStartup] = value
        }
    }

    fun getHasClearedGlideCache() =
        context.datastore.data.map {
            it[clearGlideCache] == true
        }

    fun setHasClearedGlideCache(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[clearGlideCache] = value
        }
    }

    fun getMigrateFav(): Flow<Boolean> =
        context.datastore.data.map { data ->
            data[migrateFav] != false
        }

    fun setMigrateFav(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[migrateFav] = value
        }
    }
}