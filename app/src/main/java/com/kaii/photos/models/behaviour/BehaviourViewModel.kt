package com.kaii.photos.models.behaviour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.data.datasources.BehaviourDataSource
import com.kaii.photos.domain.settings.VideoLoopMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class BehaviourViewModel(
    private val dataSource: BehaviourDataSource
) : ViewModel() {
    val exitImmediately = dataSource.getExitImmediately().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    val openVideosExternally = dataSource.getOpenVideosExternally().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    val loopVideos = dataSource.getLoopVideos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = VideoLoopMode.Off
    )

    val useTapToNav = dataSource.getTapToNav().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    val autoplayVideos = dataSource.getAutoPlayVideos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    val muteVideosOnStart = dataSource.getMuteVideosOnStart().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    val editingOverwriteByDefault = dataSource.getEditingOverwriteByDefault().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    val editingExitOnSave = dataSource.getEditingExitOnSave().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds),
        initialValue = false
    )

    fun setExitImmediately(active: Boolean) {
        viewModelScope.launch {
            dataSource.setExitImmediately(active)
        }
    }

    fun setOpenVideosExternally(active: Boolean) {
        viewModelScope.launch {
            dataSource.setOpenVideosExternally(active)
        }
    }

    fun setLoopVideos(mode: VideoLoopMode) {
        viewModelScope.launch {
            dataSource.setLoopVideos(mode)
        }
    }

    fun setUseTapToNav(active: Boolean) {
        viewModelScope.launch {
            dataSource.setTapToNav(active)
        }
    }

    fun setAutoPlayVideos(active: Boolean) {
        viewModelScope.launch {
            dataSource.setAutoPlayVideos(active)
        }
    }

    fun setMuteVideosOnStart(active: Boolean) {
        viewModelScope.launch {
            dataSource.setMuteVideosOnStart(active)
        }
    }

    fun setEditingOverwriteByDefault(active: Boolean) {
        viewModelScope.launch {
            dataSource.setEditingOverwriteByDefault(active)
        }
    }

    fun setEditingExitOnSave(active: Boolean) {
        viewModelScope.launch {
            dataSource.setEditingExitOnSave(active)
        }
    }
}