package com.kaii.photos.helpers

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

// thanks to https://github.com/Kotlin/kotlinx.coroutines/issues/2631#issuecomment-2812699291
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
fun <T, R> StateFlow<T>.mapSync(transform: (T) -> R): StateFlow<R> = object : StateFlow<R> {
    override val replayCache: List<R> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        var lastEmittedValue: Any? = Any()
        this@mapSync.collect { newValue ->
            val transformedValue = transform(newValue)
            if (transformedValue != lastEmittedValue) {
                lastEmittedValue = transformedValue
                collector.emit(transformedValue)
            }
        }
    }

    private var lastUpstreamValue = this@mapSync.value

    override var value: R = transform(lastUpstreamValue)
        private set
        get() {
            val currentUpstreamValue: T = this@mapSync.value
            if (currentUpstreamValue == lastUpstreamValue) return field
            val newValue = transform(currentUpstreamValue)
            field = newValue
            lastUpstreamValue = currentUpstreamValue
            return newValue
        }
}