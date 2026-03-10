package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.kaii.photos.datastore.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsEditingImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val overwriteByDefaultKey = booleanPreferencesKey("editing_overwrite_by_default")
    private val exitOnSaveKey = booleanPreferencesKey("exit_on_save")

    fun getOverwriteByDefault(): Flow<Boolean> =
        context.datastore.data.map {
            it[overwriteByDefaultKey] == true
        }

    fun setOverwriteByDefault(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[overwriteByDefaultKey] = value
        }
    }

    fun getExitOnSave(): Flow<Boolean> =
        context.datastore.data.map {
            it[exitOnSaveKey] == true
        }

    fun setExitOnSave(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[exitOnSaveKey] = value
        }
    }
}