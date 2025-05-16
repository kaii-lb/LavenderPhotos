package com.aks_labs.tulsi.lavender_snackbars

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.collectLatest

class LavenderSnackbarHostState {
    val snackbarHostState = SnackbarHostState()

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration
        )
    }
}

@Composable
fun rememberLavenderSnackbarHostState(): LavenderSnackbarHostState {
    val snackbarHostState = remember { LavenderSnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        LavenderSnackbarController.events.collectLatest { event ->
            when (event) {
                is LavenderSnackbarEvents.MessageEvent -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = event.duration
                    )
                }
            }
        }
    }

    return snackbarHostState
}

