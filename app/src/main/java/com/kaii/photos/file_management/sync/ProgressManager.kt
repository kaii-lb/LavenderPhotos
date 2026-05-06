package com.kaii.photos.file_management.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressManager {
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
        _progress.value = count.toFloat() / totalItems
    }

    fun increaseProgress() {
        currentItems += 1
        _progress.value = currentItems.toFloat() / totalItems
    }

    fun increaseProgressBy(amount: Int) {
        currentItems += amount
        _progress.value = currentItems.toFloat() / totalItems
    }

    fun stopTracking() {
        _isTracking.value = false
    }
}