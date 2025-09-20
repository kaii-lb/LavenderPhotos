package com.kaii.photos.compose.editing_view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kaii.photos.compose.app_bars.BottomAppBarItem

@Composable
fun EditingViewBottomAppBarItem(
    text: String,
    icon: Int,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    BottomAppBarItem(
        text = text,
        iconResId = icon,
        buttonWidth = 84.dp,
        buttonHeight = 56.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        cornerRadius = 8.dp,
        enabled = enabled,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        action = onClick
    )
}

