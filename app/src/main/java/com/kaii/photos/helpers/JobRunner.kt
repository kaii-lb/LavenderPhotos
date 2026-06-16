package com.kaii.photos.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class SingleJobRunner(private val coroutineScope: CoroutineScope) {
    protected var job: Job? = null

    open fun run(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        job = coroutineScope.launch(block = block)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

class JobDebouncer(
    private val coroutineScope: CoroutineScope,
    private val delayMs: Long
) : SingleJobRunner(coroutineScope) {
    override fun run(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        job = coroutineScope.launch {
            delay(delayMs)
            block()
        }
    }
}