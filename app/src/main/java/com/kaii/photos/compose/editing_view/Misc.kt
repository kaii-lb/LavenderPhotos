package com.kaii.photos.compose.editing_view

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kaii.photos.compose.app_bars.BottomAppBarItem
import com.kaii.photos.mediastore.MediaType

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
        buttonHeight = 56.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        cornerRadius = 8.dp,
        enabled = enabled,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        action = onClick
    )
}

data class EditorApp(
    val icon: ImageBitmap,
    val name: String,
    val packageName: String
)

fun getAvailableEditorsForType(
    context: Context,
    mediaType: MediaType
): List<EditorApp> {
    val editIntent = Intent(Intent.ACTION_EDIT).apply {
        type =
            if (mediaType == MediaType.Image) "image/*"
            else "video/*"
    }

    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(
            editIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(editIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    return info.map {
        EditorApp(
            icon = it.activityInfo.loadIcon(context.packageManager).toBitmap(1024, 1024).asImageBitmap(),
            name = it.loadLabel(context.packageManager).toString(),
            packageName = it.activityInfo.packageName
        )
    }
}