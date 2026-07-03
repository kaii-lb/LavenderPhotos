package com.kaii.photos.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

open class SingleJobRunner(
    private val coroutineScope: CoroutineScope,
    private val coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    protected var job: Job? = null

    open fun run(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        job = coroutineScope.launch(block = block, context = coroutineContext)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

class JobDebouncer(
    private val coroutineScope: CoroutineScope,
    private val delay: Duration
) : SingleJobRunner(coroutineScope) {
    override fun run(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        job = coroutineScope.launch {
            delay(delay)
            block()
        }
    }
}

@Composable
fun rememberSingleJobRunner(): SingleJobRunner {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        SingleJobRunner(coroutineScope)
    }
}