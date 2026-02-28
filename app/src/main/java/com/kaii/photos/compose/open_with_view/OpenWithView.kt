package com.kaii.photos.compose.open_with_view

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.lavender.snackbars.LavenderSnackbarBox
import com.kaii.lavender.snackbars.LavenderSnackbarHostState
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.typeOf

private const val TAG = "com.kaii.photos.compose.open_with_view.OpenWithView"

class OpenWithView : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data

        if (uri == null) {
            Toast.makeText(applicationContext, applicationContext.resources.getString(R.string.media_non_existent), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val applicationDatabase = MediaDatabase.getInstance(applicationContext)

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext, emptyList())
            )

            val initialDarkMode =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }

            val followDarkTheme by mainViewModel.settings.lookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(
                    initialValue = initialDarkMode
                )

            PhotosTheme(
                theme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val navController = rememberNavController()

                lavenderEdgeToEdge(
                    isDarkMode = isSystemInDarkTheme(),
                    navBarColor = MaterialTheme.colorScheme.surfaceContainer,
                    statusBarColor = MaterialTheme.colorScheme.background
                )

                CompositionLocalProvider(
                    LocalNavController provides navController,
                    LocalMainViewModel provides mainViewModel,
                    LocalAppDatabase provides applicationDatabase
                ) {
                    val snackbarHostState = remember {
                        LavenderSnackbarHostState()
                    }

                    LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
                        NavHost(
                            navController = navController,
                            startDestination = Screens.OpenWithView,
                            modifier = Modifier
                                .fillMaxSize(1f),
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
                            composable<Screens.OpenWithView> {
                                Content(
                                    uri = uri,
                                    window = window
                                )
                            }

                            composable<Screens.ImageEditor>(
                                typeMap = mapOf(
                                    typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                                ),
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
                                val screen: Screens.ImageEditor = it.toRoute()

                                ImageEditor(
                                    uri = screen.uri.toUri(),
                                    absolutePath = screen.absolutePath,
                                    albumInfo = null,
                                    isFromOpenWithView = true
                                )
                            }

                            composable<Screens.VideoEditor>(
                                typeMap = mapOf(
                                    typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                                ),
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
                                val screen = it.toRoute<Screens.VideoEditor>()

                                VideoEditor(
                                    uri = screen.uri.toUri(),
                                    absolutePath = screen.absolutePath,
                                    window = window,
                                    isFromOpenWithView = true,
                                    albumInfo = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(uri: Uri, window: Window) {
    val context = LocalContext.current
    var incomingData by remember { mutableStateOf<MediaStoreData?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            incomingData = try {
                context.contentResolver.getMediaStoreDataFromUri(uri = uri) ?: MediaStoreData.dummyItem
            } catch (e: Throwable) {
                Log.d(TAG, "Couldn't decode incoming data!\n${e.message}")
                MediaStoreData.dummyItem
            }
        }
    }

    AnimatedContent(
        targetState = incomingData,
        transitionSpec = {
            fadeIn().togetherWith(fadeOut())
        },
        modifier = Modifier
            .fillMaxSize()
    ) { state ->
        when {
            state == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            state != MediaStoreData.dummyItem -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    InitSinglePhotoView(
                        incomingData = incomingData!!,
                        window = window
                    )
                }
            }

            else -> {
                OpenWithContent(
                    uri = uri,
                    window = window
                )
            }
        }
    }
}

@Composable
private fun InitSinglePhotoView(
    incomingData: MediaStoreData,
    window: Window
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    val immichInfo by mainViewModel.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

    val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
        factory = MultiAlbumViewModelFactory(
            context = context,
            albumInfo = AlbumInfo.createPathOnlyAlbum(
                paths = setOf(
                    incomingData.absolutePath.parent()
                )
            ),
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat
        )
    )

    LaunchedEffect(immichInfo) {
        multiAlbumViewModel.changePaths(
            sortMode = sortMode,
            format = displayDateFormat,
            accessToken = immichInfo.accessToken
        )
    }

    val items = multiAlbumViewModel.mediaFlow.collectAsLazyPagingItems()
    val index = remember(items.itemCount, items.loadState) {
        (0 until items.itemCount).find {
            val item = (items.peek(it) as? PhotoLibraryUIModel.MediaImpl)?.item
            item?.id == incomingData.id
        }
    }

    if (index != null) {
        SinglePhotoView(
            window = window,
            viewModel = multiAlbumViewModel,
            index = index,
            albumInfo = AlbumInfo.createPathOnlyAlbum(
                paths = setOf(
                    incomingData.absolutePath.parent()
                )
            ),
            isOpenWithDefaultView = true
        )
    }
}