package com.kaii.photos.compose.pages

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.PhotosApplication
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.app_bars.wallpaper_setter.WallpaperSetterBottomBar
import com.kaii.photos.compose.app_bars.wallpaper_setter.WallpaperSetterTopBar
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WallpaperSetter : ComponentActivity() {
    enum class Type(val flag: Int) {
        HomeScreen(WallpaperManager.FLAG_SYSTEM),
        LockScreen(WallpaperManager.FLAG_LOCK),
        Both(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeSerial by PhotosApplication.appModule.settings.lookAndFeel
                .getThemeConfiguration()
                .collectAsStateWithLifecycle(initialValue = ThemeConfiguration.Default.serialize())

            PhotosTheme(
                theme = ThemeConfiguration(themeSerial)
            ) {
                lavenderEdgeToEdge(
                    isDarkMode = isSystemInDarkTheme(),
                    navBarColor = Color.Transparent,
                    statusBarColor = Color.Transparent
                )

                Content(intent = intent) {
                    finish()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalGlideComposeApi::class, ExperimentalLayoutApi::class)
    @Composable
    private fun Content(
        intent: Intent,
        close: () -> Unit
    ) {
        var offset by remember { mutableStateOf(Offset.Zero) }
        var scale by remember { mutableFloatStateOf(1f) }

        var bitmap by remember {
            mutableStateOf(createBitmap(512, 512, Bitmap.Config.ARGB_8888))
        }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                bitmap = contentResolver.openInputStream(intent.data!!)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            }
        }

        Scaffold(
            topBar = {
                WallpaperSetterTopBar(
                    uri = intent.data!!,
                    mimeType = intent.getStringExtra("mimeType")!!,
                    onDismiss = close
                )
            },
            bottomBar = {
                WallpaperSetterBottomBar(
                    bitmap = bitmap,
                    offset = offset,
                    outerScale = scale,
                    close = close
                )
            }
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = 0.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = 0.dp
                    )
            ) {
                val containerWidth = constraints.maxWidth.toFloat()
                val containerHeight = constraints.maxHeight.toFloat()

                var imageSize by remember { mutableStateOf(IntSize.Zero) }

                var animate by remember { mutableStateOf(false) }
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(
                        durationMillis = if (animate) AnimationConstants.DURATION_SHORT else 0
                    ),
                    finishedListener = {
                        animate = false
                    }
                )
                val animatedOffset by animateOffsetAsState(
                    targetValue = offset,
                    animationSpec = tween(
                        durationMillis = if (animate) AnimationConstants.DURATION_SHORT else 0
                    ),
                    finishedListener = {
                        animate = false
                    }
                )

                val windowInfo = LocalWindowInfo.current

                GlideImage(
                    model = intent.data,
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val scaleX = windowInfo.containerSize.width.toFloat() / bitmap.width
                            val scaleY = windowInfo.containerSize.height.toFloat() / bitmap.height
                            val scaleToFit = max(scaleX, scaleY)

                            val targetWidth = (bitmap.width * scaleToFit).roundToInt()
                            val targetHeight = (bitmap.height * scaleToFit).roundToInt()

                            imageSize = IntSize(targetWidth, targetHeight)

                            val placeable = measurable.measure(
                                Constraints.fixed(targetWidth, targetHeight)
                            )

                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(
                                    (constraints.maxWidth - targetWidth) / 2,
                                    (constraints.maxHeight - targetHeight) / 2
                                )
                            }
                        }
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = animatedOffset.x
                            translationY = animatedOffset.y
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                val centerX = (containerWidth - imageSize.width * scale) / 2
                                val maxX = max(centerX, -centerX)
                                val minX = min(centerX, -centerX)

                                val centerY = (containerHeight - imageSize.height * scale) / 2
                                val maxY = max(centerY, -centerY)
                                val minY = min(centerY, -centerY)

                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(minX, maxX),
                                    y = (offset.y + pan.y * scale).coerceIn(minY, maxY)
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    animate = true
                                    if (scale == 1f) {
                                        scale = 2f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}