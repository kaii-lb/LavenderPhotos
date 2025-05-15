package com.aks_labs.tulsi.lavender_snackbars

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun LavenderSnackbarBox(
    snackbarHostState: LavenderSnackbarHostState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        content()

        SnackbarHost(
            hostState = snackbarHostState.snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            val iconResId: Int? = null

            Snackbar(
                modifier = Modifier,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dismissAction = null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconResId != null) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                    }

                    Text(
                        text = data.visuals.message,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
