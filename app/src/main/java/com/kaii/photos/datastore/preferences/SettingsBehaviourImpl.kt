package com.kaii.photos.datastore.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.map

class SettingsBehaviourImpl(
    private val datastore: DataStore<Preferences>
) {
    private val exitImmediately = booleanPreferencesKey("behaviour_exit_immediately")
    private val openVideosExternally = booleanPreferencesKey("behaviour_open_videos_externally")
    private val loopVideos = intPreferencesKey("behaviour_loop_videos")
    private val tapToNav = booleanPreferencesKey("behaviour_tap_to_nav")
    private val shouldAutoPlayKey = booleanPreferencesKey("video_should_autoplay")
    private val muteOnStartKey = booleanPreferencesKey("video_mute_on_start")
    private val overwriteByDefaultKey = booleanPreferencesKey("editing_overwrite_by_default")
    private val exitOnSaveKey = booleanPreferencesKey("exit_on_save")

    fun getExitImmediately() = datastore.data.map {
        it[exitImmediately] == true
    }

    suspend fun setExitImmediately(value: Boolean) {
        datastore.edit {
            it[exitImmediately] = value
        }
    }

    fun getOpenVideosExternally() = datastore.data.map {
        it[openVideosExternally] == true
    }

    suspend fun setOpenVideosExternally(value: Boolean) {
        datastore.edit {
            it[openVideosExternally] = value
        }
    }

    /** 0 -> Never loop
     * 1 -> Loop videos under 30s
     * 2 -> Always loop videos
     */
    fun getLoopVideos() = datastore.data.map {
        it[loopVideos] ?: 0
    }

    /** 0 -> Never loop
     * 1 -> Loop videos under 30s
     * 2 -> Always loop videos
     */
    suspend fun setLoopVideos(value: Int) {
        if (value !in 0..2) throw IllegalArgumentException("Cannot set loop videos for a value of $value. Only accepted values are 0, 1, and 2.")

        datastore.edit {
            it[loopVideos] = value
        }
    }

    fun getTapToNav() = datastore.data.map {
        it[tapToNav] ?: false
    }

    suspend fun setTapToNav(active: Boolean) {
        datastore.edit {
            it[tapToNav] = active
        }
    }

    fun getAutoPlayVideos() =
        datastore.data.map {
            it[shouldAutoPlayKey] != false
        }

    suspend fun setAutoPlayVideos(value: Boolean) {
        datastore.edit {
            it[shouldAutoPlayKey] = value
        }
    }

    fun getMuteVideosOnStart() =
        datastore.data.map {
            it[muteOnStartKey] == true
        }

    suspend fun setMuteVideosOnStart(value: Boolean) {
        datastore.edit {
            it[muteOnStartKey] = value
        }
    }

    fun getEditingOverwriteByDefault() =
        datastore.data.map {
            it[overwriteByDefaultKey] == true
        }

    suspend fun setEditingOverwriteByDefault(value: Boolean) {
        datastore.edit {
            it[overwriteByDefaultKey] = value
        }
    }

    fun getEditingExitOnSave() =
        datastore.data.map {
            it[exitOnSaveKey] == true
        }

    suspend fun setEditingExitOnSave(value: Boolean) {
        datastore.edit {
            it[exitOnSaveKey] = value
        }
    }
}