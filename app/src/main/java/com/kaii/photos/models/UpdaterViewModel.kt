package com.kaii.photos.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.preferences.SettingsVersionImpl
import com.kaii.photos.domain.news.News
import com.kaii.photos.domain.news.UpdateState
import com.kaii.photos.repositories.LatestNewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class UpdaterViewModel @Inject constructor(
    private val latestNewsRepository: LatestNewsRepository,
    versions: SettingsVersionImpl
) : ViewModel() {
    private val _news = MutableStateFlow(emptyList<News>())
    val news = _news.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateState.Loading)
    val updateState = _updateState.asStateFlow()

    val showUpdateNotice = versions.getShowUpdateNotice().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            _news.value = latestNewsRepository.getNews()

            _updateState.value =
                if (latestNewsRepository.hasUpdate()) UpdateState.Available
                else UpdateState.NotAvailable
        }
    }
}