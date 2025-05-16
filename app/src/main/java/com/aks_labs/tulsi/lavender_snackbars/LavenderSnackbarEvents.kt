package com.aks_labs.tulsi.lavender_snackbars

import androidx.compose.material3.SnackbarDuration

sealed class LavenderSnackbarEvents {
    data class MessageEvent(
        val message: String,
        val iconResId: Int? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : LavenderSnackbarEvents()
}

