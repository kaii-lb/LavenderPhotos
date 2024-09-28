package com.kaii.photos.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

object CustomMaterialTheme {
    val colorScheme
        @Composable
        get() = getCustomMaterialColorScheme()
}

@Composable
private fun getCustomMaterialColorScheme(): ColorScheme {
    if (isSystemInDarkTheme()) {
        val scheme = MaterialTheme.colorScheme
        return scheme
    } else {
        val scheme = MaterialTheme.colorScheme.copy(
            background = MaterialTheme.colorScheme.surfaceContainer,
            surfaceContainer = MaterialTheme.colorScheme.primaryContainer,
            surface = MaterialTheme.colorScheme.surfaceContainer,
            onBackground = MaterialTheme.colorScheme.onSurface,
            onSurface = MaterialTheme.colorScheme.onPrimaryContainer,
            secondaryContainer = MaterialTheme.colorScheme.surface,
        )

        return scheme
    }
}
