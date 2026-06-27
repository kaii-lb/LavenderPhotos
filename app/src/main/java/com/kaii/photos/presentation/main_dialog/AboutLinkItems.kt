package com.kaii.photos.presentation.main_dialog

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import com.kaii.photos.R

enum class AboutLinkItems(
    val title: Int,
    val icon: Int,
    val intent: Intent,
    val enabled: Boolean = true
) {
    Translation(
        title = R.string.translation,
        icon = R.drawable.globe,
        intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://hosted.weblate.org/projects/lavender-photos/".toUri()
            }
    ),

    Donations(
        title = R.string.support,
        icon = R.drawable.donation,
        enabled = false,
        intent =
            Intent(Intent.ACTION_VIEW).apply {
                // data = TODO()
            }
    );

    val color: Color
        @Composable
        get() =
            when (this) {
                Donations -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
}