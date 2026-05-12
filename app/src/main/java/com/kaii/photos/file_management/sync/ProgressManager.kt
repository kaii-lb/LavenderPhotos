package com.kaii.photos.file_management.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressManager {
    // TODO: make this display a startup "loading check"
    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    var currentItems = 0
    var totalItems = 0

    fun startTracking(totalItems: Int) {
        this.totalItems = totalItems
        _progress.value = 0f
        currentItems = 0

        _isTracking.value = true
    }

    fun addToTotalItems(count: Int) {
        totalItems += count
        updateProgress()
    }

    fun increaseProgress() {
        currentItems += 1
        updateProgress()
    }

    fun increaseProgressBy(amount: Int) {
        currentItems += amount
        updateProgress()
    }

    fun stopTracking() {
        _isTracking.value = false
    }

    private fun updateProgress() {
        _progress.value =
            if (totalItems <= 0) 0f
            else currentItems.toFloat() / totalItems
    }

}