package com.kaii.photos.compose.app_bars.wallpaper_setter

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.WallpaperTypeDialog
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "com.kaii.photos.app_bars.wallpaper_setter.WallpaperSetterBottomBar"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperSetterBottomBar(
    bitmap: Bitmap,
    offset: Offset,
    outerScale: Float,
    modifier: Modifier = Modifier,
    close: () -> Unit,
) {
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .fillMaxWidth(1f)
            .height(56.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            WallpaperTypeDialog(
                onSetWallpaperType = { wallpaperType, scrollable ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        val deviceSize = Size(
                            width = windowManager.currentWindowMetrics.bounds.width().toFloat(),
                            height = windowManager.currentWindowMetrics.bounds.height().toFloat()
                        )

                        val originalAspectRatio = bitmap.width.toFloat() / bitmap.height
                        val deviceAspectRatio = deviceSize.width / deviceSize.height
                        val scale =
                            if (originalAspectRatio >= deviceAspectRatio) deviceSize.height / bitmap.height
                            else deviceSize.width / bitmap.width

                        val wallpaperWidth = if (scrollable) {
                            // deviceSize.width * 2f
                            deviceSize.width * outerScale + offset.x * (1f / scale)
                        } else {
                            deviceSize.width
                        }

                        Log.d(TAG, "${bitmap.width} $scale $outerScale $offset $wallpaperWidth")

                        val destinationBitmap =
                            createBitmap(
                                wallpaperWidth.toInt(),
                                deviceSize.height.toInt(),
                                bitmap.config ?: Bitmap.Config.ARGB_8888
                            )

                        val canvas = Canvas(destinationBitmap)

                        val centeringOffset = Offset(
                            x = (deviceSize.width - bitmap.width * scale * outerScale) / 2f,
                            y = (deviceSize.height - bitmap.height * scale * outerScale) / 2f
                        )

                        val matrix = Matrix().apply {
                            postScale(scale * outerScale, scale * outerScale)
                            postTranslate(centeringOffset.x + offset.x, centeringOffset.y + offset.y)
                        }
                        canvas.drawBitmap(bitmap, matrix, Paint().apply { isAntiAlias = true })

                        val wallpaperManager = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager

                        wallpaperManager.setBitmap(destinationBitmap, null, true, wallpaperType.flag)

                        delay(1000)

                        close()
                    }
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }

        FullWidthDialogButton(
            text = stringResource(id = R.string.apply),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single,
            modifier = Modifier
                .dropShadow(
                    shape = RoundedCornerShape(1000.dp),
                    shadow = Shadow(
                        radius = 8.dp,
                        color = Color.Black,
                        spread = 2.dp,
                        alpha = 0.5f
                    )
                )
        ) {
            showDialog = true
        }
    }
}