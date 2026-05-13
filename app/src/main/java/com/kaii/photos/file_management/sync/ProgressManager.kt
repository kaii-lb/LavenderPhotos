package com.kaii.photos.file_management.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kaii.photos.helpers.AnimationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProgressManager(
    private val scope: CoroutineScope
) {
    enum class State {
        StartingUp,
        Tracking,
        Error,
        Idle
    }

    var state by mutableStateOf(State.StartingUp)
        private set

    var progress by mutableFloatStateOf(0f)
        private set

    var currentItems by mutableIntStateOf(0)
        private set

    var totalItems by mutableIntStateOf(0)
        private set

    init {
        // startup gauge check
        scope.launch {
            delay(AnimationConstants.DURATION_SHORT.toLong())

            progress = 1f

            delay(AnimationConstants.DURATION_EXTRA_EXTRA_LONG * 2L)
            state = State.Idle
        }
    }

    fun startTracking(totalItems: Int) {
        this.totalItems = totalItems
        progress = 0f
        currentItems = 0

        state = State.Tracking
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
        scope.launch {
            delay(AnimationConstants.DURATION_EXTRA_EXTRA_LONG.toLong())
            state = if (progress == 1f) State.Idle else State.Error
        }
    }

    private fun updateProgress() {
        progress =
            if (totalItems <= 0) 0f
            else currentItems.toFloat() / totalItems
    }
}