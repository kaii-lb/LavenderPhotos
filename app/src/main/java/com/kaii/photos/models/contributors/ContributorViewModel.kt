package com.kaii.photos.models.contributors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.data.providers.AppVersionProvider
import com.kaii.photos.domain.about.ContributorItem
import com.kaii.photos.repositories.ContributorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContributorViewModel(
    private val contributorRepository: ContributorRepository,
    versionProvider: AppVersionProvider
) : ViewModel() {
    private val _contributors = MutableStateFlow(emptyList<ContributorItem>())
    val contributors = _contributors.asStateFlow()

    val appVersion = versionProvider.getCurrentVersionString()

    init {
        viewModelScope.launch {
            _contributors.update { contributorRepository.getNewsData() }
        }
    }
}