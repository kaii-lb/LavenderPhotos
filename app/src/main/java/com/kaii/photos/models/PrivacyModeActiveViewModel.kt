package com.kaii.photos.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.repositories.PrivacyModeActiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyModeActiveViewModel @Inject constructor(
    private val repo: PrivacyModeActiveRepository
) : ViewModel() {
    val events = repo.events

    fun unlock() {
        viewModelScope.launch {
            repo.unlock()
        }
    }

    fun markUnlocked() {
        repo.markUnlocked()
    }
}