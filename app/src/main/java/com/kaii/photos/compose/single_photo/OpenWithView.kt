package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.lavender.snackbars.LavenderSnackbarBox
import com.kaii.lavender.snackbars.LavenderSnackbarHostState
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.BottomAppBarItem
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.formatDate
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.delay

class OpenWithView : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data

        if (uri == null) {
            Toast.makeText(applicationContext, applicationContext.resources.getString(R.string.media_non_existent), Toast.LENGTH_LONG).show()
            finish()
            return
        }


        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext, emptyList())
            )

            enableEdgeToEdge(
                navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                statusBarStyle =
                    if (!isSystemInDarkTheme()) {
                        SystemBarStyle.light(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    } else {
                        SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb())
                    }
            )

            val initialDarkMode =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }

            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(
                    initialValue = initialDarkMode
                )

            PhotosTheme(
                theme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val navController = rememberNavController()

                CompositionLocalProvider(
                    LocalNavController provides navController,
                    LocalMainViewModel provides mainViewModel
                ) {
                    val snackbarHostState = remember {
                        LavenderSnackbarHostState()
                    }

                    LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
                        NavHost(
                            navController = navController,
                            startDestination = MultiScreenViewType.OpenWithView.name,
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .background(MaterialTheme.colorScheme.background),
                            enterTransition = {
                                slideInHorizontally(
                                    animationSpec = tween(
                                        durationMillis = 350
                                    )
                                ) { width -> width } + fadeIn()
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        durationMillis = 350
                                    )
                                ) { width -> -width } + fadeOut()
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        durationMillis = 350
                                    )
                                ) { width -> width } + fadeOut()
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    animationSpec = tween(
                                        durationMillis = 350
                                    )
                                ) { width -> -width } + fadeIn()
                            }
                        ) {
                            composable(MultiScreenViewType.OpenWithView.name) {
                                enableEdgeToEdge(
                                    navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                                    statusBarStyle =
                                        if (!isSystemInDarkTheme()) {
                                            SystemBarStyle.light(
                                                MaterialTheme.colorScheme.background.toArgb(),
                                                MaterialTheme.colorScheme.background.toArgb()
                                            )
                                        } else {
                                            SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb())
                                        }
                                )

                                Content(
                                    uri = uri,
                                    window = window
                                )
                            }

                            composable<Screens.ImageEditor>(
                                enterTransition = {
                                    slideInVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                }
                            ) {
                                enableEdgeToEdge(
                                    navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                                    statusBarStyle = SystemBarStyle.auto(
                                        MaterialTheme.colorScheme.surfaceContainer.toArgb(),
                                        MaterialTheme.colorScheme.surfaceContainer.toArgb()
                                    )
                                )

                                val screen: Screens.ImageEditor = it.toRoute()

                                ImageEditor(
                                    uri = screen.uri.toUri(),
                                    absolutePath = screen.absolutePath,
                                    isFromOpenWithView = true
                                )
                            }

                            composable<Screens.VideoEditor>(
                                enterTransition = {
                                    slideInVertically(
                                        animationSpec = AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                },
                                exitTransition = {
                                    slideOutVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                },
                                popEnterTransition = {
                                    slideInVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                },
                                popExitTransition = {
                                    slideOutVertically(
                                        AnimationConstants.expressiveTween(AnimationConstants.DURATION)
                                    ) { height -> height } + fadeOut(
                                        animationSpec = tween(
                                            durationMillis = AnimationConstants.DURATION_LONG
                                        )
                                    )
                                }
                            ) {
                                enableEdgeToEdge(
                                    navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                                    statusBarStyle = SystemBarStyle.auto(
                                        MaterialTheme.colorScheme.surface.toArgb(),
                                        MaterialTheme.colorScheme.surface.toArgb()
                                    )
                                )

                                val screen = it.toRoute<Screens.VideoEditor>()

                                VideoEditor(
                                    uri = screen.uri.toUri(),
                                    absolutePath = screen.absolutePath,
                                    window = window,
                                    isFromOpenWithView = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun Content(
    uri: Uri,
    window: Window
) {
    val appBarsVisible = remember { mutableStateOf(true) }
    val context = LocalContext.current

    val releaseExoPlayer: MutableState<() -> Unit> = remember { mutableStateOf({}) }

    val mimeType = context.contentResolver.getType(uri) ?: "image/*"
    val type =
        if (mimeType.contains("image")) MediaType.Image
        else MediaType.Video

    Scaffold(
        topBar = {
            TopBar(
                appBarsVisible = appBarsVisible
            ) {
                releaseExoPlayer.value()
            }
        },
        bottomBar = {
            BottomBar(
                uri = uri,
                appBarsVisible = appBarsVisible,
                window = window,
                mediaType = type,
                mimeType = mimeType
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val scale = rememberSaveable { mutableFloatStateOf(1f) }
            val rotation = rememberSaveable { mutableFloatStateOf(0f) }
            val offset = remember { mutableStateOf(Offset.Zero) }

            val isTouchLocked = remember { mutableStateOf(false) }
            val controlsVisible = remember { mutableStateOf(false) }

            if (type == MediaType.Video) {
                OpenWithVideoPlayer(
                    uri = uri,
                    controlsVisible = controlsVisible,
                    appBarsVisible = appBarsVisible,
                    window = window,
                    releaseExoPlayer = releaseExoPlayer,
                    isTouchLocked = isTouchLocked,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            window = window,
                            appBarsVisible = appBarsVisible,
                            item = MediaStoreData(
                                type = MediaType.Video
                            )
                        )
                )
            } else {
                GlideImage(
                    model = uri,
                    contentDescription = "opened image",
                    contentScale = ContentScale.Fit,
                    failure = placeholder(R.drawable.broken_image),
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .mediaModifier(
                            scale = scale,
                            rotation = rotation,
                            offset = offset,
                            window = window,
                            appBarsVisible = appBarsVisible
                        )
                ) {
                    it.diskCacheStrategy(DiskCacheStrategy.NONE)
                }
            }
        }
    }
}

@Composable
private fun OpenWithVideoPlayer(
    uri: Uri,
    controlsVisible: MutableState<Boolean>,
    appBarsVisible: MutableState<Boolean>,
    isTouchLocked: MutableState<Boolean>,
    window: Window,
    releaseExoPlayer: MutableState<() -> Unit>,
    modifier: Modifier
) {
    val isPlaying = remember { mutableStateOf(false) }
    val lastIsPlaying = rememberSaveable { mutableStateOf(isPlaying.value) }

    val isMuted = remember { mutableStateOf(false) }

    val currentVideoPosition = remember { mutableFloatStateOf(0f) }
    val duration = remember { mutableFloatStateOf(0f) }

    val exoPlayer = rememberExoPlayerWithLifeCycle(
        videoSource = uri,
        absolutePath = null,
        isPlaying = isPlaying,
        currentVideoPosition = currentVideoPosition,
        duration = duration
    )

    releaseExoPlayer.value = {
        exoPlayer.stop()
        exoPlayer.release()
    }

    val context = LocalContext.current
    BackHandler {
        isPlaying.value = false
        currentVideoPosition.floatValue = 0f
        duration.floatValue = 0f

        releaseExoPlayer.value()

        (context as Activity).finish()
    }

    LaunchedEffect(isMuted.value) {
        exoPlayer.volume = if (isMuted.value) 0f else 1f

        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder().apply {
                setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                AudioAttributes.DEFAULT
                setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            }.build(),
            !isMuted.value
        )
    }

    val localConfig = LocalConfiguration.current
    LaunchedEffect(isPlaying.value, localConfig.orientation) {
        if (!isPlaying.value) {
            controlsVisible.value = true
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (localConfig.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                setBarVisibility(
                    visible = true,
                    window = window
                ) {
                    appBarsVisible.value = true
                }
            }
            exoPlayer.pause()
        } else {
            exoPlayer.playWhenReady = true
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exoPlayer.play()
        }

        lastIsPlaying.value = isPlaying.value

        currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f
        if (kotlin.math.ceil(currentVideoPosition.floatValue) >= kotlin.math.ceil(duration.floatValue) && duration.floatValue != 0f && !isPlaying.value) {
            delay(1000)
            exoPlayer.pause()
            exoPlayer.seekTo(0)
            currentVideoPosition.floatValue = 0f
            isPlaying.value = false
        }

        while (isPlaying.value) {
            currentVideoPosition.floatValue = exoPlayer.currentPosition / 1000f

            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        Column(
            modifier = modifier.then(Modifier.align(Alignment.Center)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val playerView = rememberPlayerView(exoPlayer, context as Activity, null)
            AndroidView(
                factory = {
                    playerView
                }
            )
        }

        var doubleTapDisplayTimeMillis by remember { mutableIntStateOf(0) }
        val seekBackBackgroundColor by animateColorAsState(
            targetValue = if (doubleTapDisplayTimeMillis < 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate double tap to skip background color"
        )
        val seekForwardBackgroundColor by animateColorAsState(
            targetValue = if (doubleTapDisplayTimeMillis > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
            animationSpec = tween(
                durationMillis = 350
            ),
            label = "Animate double tap to skip background color"
        )

        LaunchedEffect(doubleTapDisplayTimeMillis) {
            delay(1000)
            doubleTapDisplayTimeMillis = 0
        }

        var showVideoPlayerControlsTimeout by remember { mutableIntStateOf(0) }
        val isLandscape by rememberDeviceOrientation()

        LaunchedEffect(showVideoPlayerControlsTimeout) {
            delay(5000)
            setBarVisibility(
                visible = false,
                window = window
            ) {
                appBarsVisible.value = it

                controlsVisible.value = it
            }

            showVideoPlayerControlsTimeout = 0
        }

        LaunchedEffect(controlsVisible.value) {
            if (controlsVisible.value) showVideoPlayerControlsTimeout += 1
        }

        LaunchedEffect(isLandscape) {
            setBarVisibility(
                visible = !isLandscape,
                window = window
            ) {
                appBarsVisible.value = it
                if (!isLandscape) controlsVisible.value = it
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!isTouchLocked.value && doubleTapDisplayTimeMillis == 0) {
                                setBarVisibility(
                                    visible = if (isLandscape) false else !controlsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }
                                controlsVisible.value = !controlsVisible.value
                            }
                        },

                        onDoubleTap = { position ->
                            if (!isTouchLocked.value && position.x < size.width / 2) {
                                doubleTapDisplayTimeMillis -= 1000

                                val prev = isPlaying.value
                                exoPlayer.seekBack()
                                isPlaying.value = prev
                            } else if (!isTouchLocked.value && position.x >= size.width / 2) {
                                doubleTapDisplayTimeMillis += 1000

                                val prev = isPlaying.value
                                exoPlayer.seekForward()
                                isPlaying.value = prev
                            }

                            showVideoPlayerControlsTimeout += 1
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(0, 100, 100, 0))
                    .background(seekBackBackgroundColor)
                    .zIndex(2f)
            ) {
                AnimatedVisibility(
                    visible = doubleTapDisplayTimeMillis < 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 300
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    exit =
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 300
                            )
                        ) + scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_rewind),
                        contentDescription = stringResource(id = R.string.video_seek_icon_desc),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(100, 0, 0, 100))
                    .background(seekForwardBackgroundColor)
                    .zIndex(2f)
            ) {
                AnimatedVisibility(
                    visible = doubleTapDisplayTimeMillis > 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 300
                            )
                        ) + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    exit =
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 300
                            )
                        ) + scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        ),
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fast_forward),
                        contentDescription = stringResource(id = R.string.video_seek_icon_desc),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible.value,
            enter = expandIn(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 350
                )
            ),
            exit = shrinkOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            ),
            modifier = Modifier
                .fillMaxSize(1f)
                .align(Alignment.Center)
        ) {
            VideoPlayerControls(
                exoPlayer = exoPlayer,
                isPlaying = isPlaying,
                isMuted = isMuted,
                currentVideoPosition = currentVideoPosition,
                duration = duration,
                title = "Media",
                onAnyTap = {
                    showVideoPlayerControlsTimeout += 1
                },
                modifier = Modifier
                    .fillMaxSize(1f)
            )
        }

        if ((isTouchLocked.value || controlsVisible.value) && localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .animateContentSize()
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                FilledTonalIconToggleButton(
                    checked = isTouchLocked.value,
                    onCheckedChange = {
                        isTouchLocked.value = it
                        showVideoPlayerControlsTimeout += 1
                    },
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors().copy(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Top)
                ) {
                    Icon(
                        painter = painterResource(id = if (isTouchLocked.value) R.drawable.secure_folder else R.drawable.unlock),
                        contentDescription = stringResource(id = R.string.video_lock_screen),
                        modifier = Modifier
                            .size(20.dp)
                    )
                }

                if (controlsVisible.value) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 4.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                setBarVisibility(
                                    visible = !appBarsVisible.value,
                                    window = window
                                ) {
                                    appBarsVisible.value = it
                                }

                                showVideoPlayerControlsTimeout += 1
                                isTouchLocked.value = false
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.more_options),
                                contentDescription = stringResource(id = R.string.show_options),
                                modifier = Modifier
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    appBarsVisible: MutableState<Boolean>,
    releaseExoPlayer: () -> Unit
) {
    AnimatedVisibility(
        visible = appBarsVisible.value,
        enter =
            slideInVertically(
                animationSpec = tween(
                    durationMillis = 250
                )
            ) { width -> -width } + fadeIn(),
        exit =
            slideOutVertically(
                animationSpec = tween(
                    durationMillis = 250
                )
            ) { width -> -width } + fadeOut(),
    ) {
        TopAppBar(
            title = {
                val isLandscape by rememberDeviceOrientation()
                Text(
                    text = stringResource(id = R.string.media),
                    fontSize = TextUnit(18f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = if (isLandscape) 300.dp else 180.dp)
                )
            },
            navigationIcon = {
                val context = LocalContext.current

                IconButton(
                    onClick = {
                        releaseExoPlayer()
                        (context as Activity).finish()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = stringResource(id = R.string.return_to_previous_page),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomBar(
    uri: Uri,
    appBarsVisible: MutableState<Boolean>,
    window: Window,
    mediaType: MediaType,
    mimeType: String
) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    AnimatedVisibility(
        visible = appBarsVisible.value,
        enter =
            slideInVertically(
                animationSpec = tween(
                    durationMillis = 250
                )
            ) { width -> width } + fadeIn(),
        exit =
            slideOutVertically(
                animationSpec = tween(
                    durationMillis = 300
                )
            ) { width -> width } + fadeOut(),
    ) {
        val isLandscape by rememberDeviceOrientation()

        BottomAppBar(
            actions = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(12.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        if (isLandscape)
                            Arrangement.spacedBy(
                                space = 48.dp,
                                alignment = Alignment.CenterHorizontally
                            )
                        else Arrangement.SpaceEvenly
                ) {
                    BottomAppBarItem(
                        text = "Share",
                        iconResId = R.drawable.share,
                        cornerRadius = 32.dp,
                        action = {
                            shareImage(uri, context)
                        }
                    )

                    val resources = LocalResources.current
                    BottomAppBarItem(
                        text = stringResource(id = R.string.edit),
                        iconResId = R.drawable.paintbrush,
                        cornerRadius = 32.dp,
                        action = {
                            val extension = mimeType.split("/")[1]
                            val currentTime = System.currentTimeMillis()
                            val date = formatDate(
                                timestamp = currentTime / 1000,
                                sortBy = MediaItemSortMode.DateTaken,
                                format = DisplayDateFormat.Default
                            )
                            val name = resources.getString(R.string.edit_desc, "$date.$extension")

                            val contentValues = ContentValues().apply {
                                put(MediaColumns.DISPLAY_NAME, name)
                                put(MediaColumns.DATE_MODIFIED, currentTime)
                                put(MediaColumns.DATE_TAKEN, currentTime)
                                put(MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                                put(MediaColumns.MIME_TYPE, mimeType)
                            }

                            val contentUri = context.contentResolver.insert(
                                if (mediaType == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )

                            contentUri?.let {
                                context.contentResolver.getMediaStoreDataFromUri(contentUri)?.absolutePath?.let { absolutePath ->
                                    context.contentResolver.copyUriToUri(
                                        from = uri,
                                        to = contentUri
                                    )

                                    setBarVisibility(
                                        visible = true,
                                        window = window
                                    ) {
                                        appBarsVisible.value = it
                                    }

                                    navController.navigate(
                                        if (mediaType == MediaType.Image) {
                                            Screens.ImageEditor(
                                                absolutePath = absolutePath,
                                                uri = contentUri.toString(),
                                                dateTaken = currentTime / 1000
                                            )
                                        } else {
                                            Screens.VideoEditor(
                                                uri = contentUri.toString(),
                                                absolutePath = absolutePath
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        )
    }
}
