package com.kaii.photos.models.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.data.providers.UpdaterParamProvider
import com.kaii.photos.domain.news.News
import com.kaii.photos.domain.news.UpdateState
import com.kaii.photos.repositories.LatestNewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpdaterViewModel(
    private val latestNewsRepository: LatestNewsRepository,
    paramProvider: UpdaterParamProvider
) : ViewModel() {
    private val _news = MutableStateFlow(emptyList<News>())
    val news = _news.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateState.Loading)
    val updateState = _updateState.asStateFlow()

    val showUpdateNotice = paramProvider.showUpdateNotice.stateIn(
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