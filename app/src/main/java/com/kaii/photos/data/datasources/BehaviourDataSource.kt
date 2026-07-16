package com.kaii.photos.data.datasources

import com.kaii.photos.datastore.preferences.SettingsBehaviourImpl
import com.kaii.photos.domain.settings.VideoLoopMode
import kotlinx.coroutines.flow.map

class BehaviourDataSource(
    private val behaviour: SettingsBehaviourImpl
) {
    fun getExitImmediately() = behaviour.getExitImmediately()
    suspend fun setExitImmediately(active: Boolean) = behaviour.setExitImmediately(active)

    fun getOpenVideosExternally() = behaviour.getOpenVideosExternally()
    suspend fun setOpenVideosExternally(active: Boolean) = behaviour.setOpenVideosExternally(active)

    fun getLoopVideos() = behaviour.getLoopVideos().map { VideoLoopMode.entries[it] }
    suspend fun setLoopVideos(mode: VideoLoopMode) = behaviour.setLoopVideos(mode.ordinal)

    fun getAutoPlayVideos() = behaviour.getAutoPlayVideos()
    suspend fun setAutoPlayVideos(active: Boolean) = behaviour.setAutoPlayVideos(active)

    fun getMuteVideosOnStart() = behaviour.getMuteVideosOnStart()
    suspend fun setMuteVideosOnStart(active: Boolean) = behaviour.setMuteVideosOnStart(active)

    fun getEditingOverwriteByDefault() = behaviour.getEditingOverwriteByDefault()
    suspend fun setEditingOverwriteByDefault(active: Boolean) = behaviour.setEditingOverwriteByDefault(active)

    fun getEditingExitOnSave() = behaviour.getEditingExitOnSave()
    suspend fun setEditingExitOnSave(active: Boolean) = behaviour.setEditingExitOnSave(active)
}