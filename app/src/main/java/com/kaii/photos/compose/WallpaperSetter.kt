package com.kaii.photos.compose

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.compose.app_bars.WallpaperSetterBottomBar
import com.kaii.photos.compose.app_bars.WallpaperSetterTopBar
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
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
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext, emptyList())
            )
            val initial =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }
            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = initial)

            PhotosTheme(
                theme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                enableEdgeToEdge(
                    navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                    statusBarStyle = SystemBarStyle.auto(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb()
                    )
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

        val bitmap = remember {
            contentResolver.openInputStream(intent.data!!)?.use {
                BitmapFactory.decodeStream(it)
            } ?: createBitmap(512, 512, Bitmap.Config.ARGB_8888)
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
            Box(
                modifier = Modifier
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = 0.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = 0.dp
                    )
            ) {
                val windowInfo = LocalWindowInfo.current
                val localDensity = LocalDensity.current
                val aspectRatio = bitmap.width.toFloat() / bitmap.height

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

                GlideImage(
                    model = intent.data,
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val widthOverride = constraints.maxHeight * aspectRatio
                            val placeable = measurable.measure(constraints.copy(maxWidth = widthOverride.roundToInt()))

                            layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                        .onGloballyPositioned {
                            with(localDensity) {
                                imageSize = it.size
                            }
                        }
                        .offset {
                            animatedOffset.round()
                        }
                        .scale(animatedScale)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                with(localDensity) {
                                    val minX = (windowInfo.containerSize.width - imageSize.width * scale) / 2
                                    val maxX = -minX

                                    val minY = (windowInfo.containerSize.height - imageSize.height * scale) / 2
                                    val maxY = -minY

                                    offset = Offset(
                                        x = (offset.x + pan.x * scale).coerceIn(minX, maxX),
                                        y = (offset.y + pan.y * scale).coerceIn(minY, maxY)
                                    )
                                }
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