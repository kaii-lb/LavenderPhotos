package com.kaii.photos.models.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.presentation.ui.theme.LavenderThemes
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.repositories.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val repo: ThemeRepository
) : ViewModel() {
    val themes = LavenderThemes.Theme.entries

    private val _previewConfiguration = MutableStateFlow(ThemeConfiguration.Default)
    val previewConfiguration = _previewConfiguration.asStateFlow()

    val hasChanges = repo.getThemeConfiguration().combine(previewConfiguration) { applied, preview ->
        applied != preview
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    init {
        viewModelScope.launch {
            repo.getThemeConfiguration().collect { config ->
                _previewConfiguration.value = config
            }
        }
    }

    fun applyThemeConfiguration() {
        viewModelScope.launch {
            repo.setThemeConfiguration(_previewConfiguration.value)
        }
    }

    fun setPreviewTheme(theme: LavenderThemes.Theme) {
        _previewConfiguration.value = _previewConfiguration.value.copy(
            theme = theme
        )
    }

    fun setStyle(style: LavenderThemes.Style) {
        _previewConfiguration.value = _previewConfiguration.value.copy(
            style = style
        )
    }

    fun setDynamic(dynamic: Boolean) {
        _previewConfiguration.value = _previewConfiguration.value.copy(
            dynamic = dynamic
        )
    }
}