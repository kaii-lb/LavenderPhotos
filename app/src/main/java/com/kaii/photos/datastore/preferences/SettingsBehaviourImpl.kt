package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    private val loopVideos = intPreferencesKey("behaviour_loop_videos")

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

    /** 0 -> Never loop
     * 1 -> Loop videos under 30s
     * 2 -> Always loop videos
     */
    fun getLoopVideos() = context.datastore.data.map {
        it[loopVideos] ?: 0
    }

    /** 0 -> Never loop
     * 1 -> Loop videos under 30s
     * 2 -> Always loop videos
     */
    fun setLoopVideos(value: Int) = scope.launch {
        if (value !in 0..2) throw IllegalArgumentException("Cannot set loop videos for a value of $value. Only accepted values are 0, 1, and 2.")

        context.datastore.edit {
            it[loopVideos] = value
        }
    }
}