package com.aks_labs.tulsi.lavender_snackbars

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LavenderSnackbarController {
    private val _events = MutableSharedFlow<LavenderSnackbarEvents>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    suspend fun pushEvent(event: LavenderSnackbarEvents) {
        _events.emit(event)
    }
}
