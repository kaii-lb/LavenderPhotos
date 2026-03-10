package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.kaii.photos.datastore.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsBehaviourImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val exitImmediately = booleanPreferencesKey("behaviour_exit_immediately")
    private val openVideosExternally = booleanPreferencesKey("behaviour_open_videos_externally")

    fun getExitImmediately() = context.datastore.data.map {
        it[exitImmediately] == true
    }

    fun setExitImmediately(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[exitImmediately] = value
        }
    }

    fun getOpenVideosExternally() = context.datastore.data.map {
        it[openVideosExternally] == true
    }

    fun setOpenVideosExternally(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[openVideosExternally] = value
        }
    }
}