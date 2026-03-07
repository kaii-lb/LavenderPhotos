package com.kaii.photos.compose.app_bars.wallpaper_setter

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WallpaperSetterTopBar(
    uri: Uri,
    mimeType: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .fillMaxWidth(1f)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(
            onClick = onDismiss,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .dropShadow(
                    shape = CircleShape,
                    shadow = Shadow(
                        radius = 6.dp,
                        color = Color.Black,
                        spread = (-4).dp,
                        alpha = 0.5f
                    )
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_arrow),
                contentDescription = stringResource(id = R.string.return_to_previous_page),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        val context = LocalContext.current
        val resources = LocalResources.current
        FilledTonalIconButton(
            onClick = {
                val intent = Intent().apply {
                    action = Intent.ACTION_ATTACH_DATA
                    data = uri
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra("mimeType", mimeType)
                }

                context.startActivity(
                    Intent.createChooser(
                        intent,
                        resources.getString(R.string.set_as_wallpaper)
                    )
                )
            },
            shape = MaterialShapes.Square.toShape(),
            modifier = Modifier
                .dropShadow(
                    shape = RoundedCornerShape(12.dp),
                    shadow = Shadow(
                        radius = 6.dp,
                        color = Color.Black,
                        spread = (-4).dp,
                        alpha = 0.5f
                    )
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more_options),
                contentDescription = stringResource(id = R.string.show_options),
                modifier = Modifier
                    .size(24.dp)
            )
        }
    }
}