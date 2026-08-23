package com.kaii.photos.presentation.selection

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SelectionManagerEventEffect(
    eventFlow:  Flow<SelectionEvent>
) {
    val resources = LocalResources.current

    LaunchedEffect(eventFlow) {
        eventFlow.collectLatest { event ->
            LavenderSnackbarController.pushEvent(
                event = LavenderSnackbarEvent.MessageEvent(
                    message = resources.getString(event.message),
                    icon = event.icon,
                    duration = SnackbarDuration.Short
                )
            )
        }
    }
}