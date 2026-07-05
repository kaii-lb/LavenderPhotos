package com.kaii.photos.compose.open_with_view

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.sync.FirstTimeSyncWorker
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.models.editor.EditorViewModel
import com.kaii.photos.models.editor.EditorViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarBox
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarHostState
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

        setContent {
            val themeSerial by PhotosApplication.appModule.settings.lookAndFeel
                .getThemeConfiguration()
                .collectAsStateWithLifecycle(initialValue = ThemeConfiguration.Default.serialize())

            PhotosTheme(
                theme = ThemeConfiguration(themeSerial)
            ) {
                val navController = rememberNavController()

                lavenderEdgeToEdge(
                    isDarkMode = isSystemInDarkTheme(),
                    navBarColor = MaterialTheme.colorScheme.surfaceContainer,
                    statusBarColor = MaterialTheme.colorScheme.background
                )

                CompositionLocalProvider(
                    LocalNavController provides navController
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
                                    typeOf<AlbumType>() to AlbumType.NavType()
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
                                val context = LocalContext.current
                                val screen: Screens.ImageEditor = it.toRoute()
                                val viewModel = viewModel<EditorViewModel>(
                                    factory = EditorViewModelFactory(
                                        context = context,
                                        album = screen.album
                                    )
                                )

                                val info by viewModel.immichInfo.collectAsStateWithLifecycle()
                                ImageEditor(
                                    uri = screen.uri,
                                    info = { info },
                                    isFromOpenWithView = true,
                                    exportQuality = { 8 },
                                    overwriteByDefault = { false },
                                    editImage = viewModel::editImage,
                                    setNavProps = viewModel::setNavProps
                                )
                            }

                            composable<Screens.VideoEditor>(
                                typeMap = mapOf(
                                    typeOf<AlbumType>() to AlbumType.NavType()
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
                                    uri = screen.uri,
                                    window = window,
                                    isFromOpenWithView = true,
                                    album = null
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
        incomingData = try {
            context.contentResolver.getMediaStoreDataFromUri(uri = uri) ?: MediaStoreData.dummyItem
        } catch (e: Throwable) {
            Log.d(TAG, "Couldn't decode incoming data!\n${e.message}")
            MediaStoreData.dummyItem
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
                val blurViews by PhotosApplication.appModule.settings.lookAndFeel.getBlurViews().collectAsStateWithLifecycle(initialValue = false)
                val useBlackBackground by PhotosApplication.appModule.settings.lookAndFeel.getUseBlackBackgroundForViews()
                    .collectAsStateWithLifecycle(initialValue = false)

                OpenWithContent(
                    uri = uri,
                    window = window,
                    blurViews = blurViews,
                    useBlackBackground = useBlackBackground
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
    val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
        factory = MultiAlbumViewModelFactory(
            context = context,
            album = AlbumType.Folder(
                id = "",
                name = incomingData.parentPath.filename(),
                paths = setOf(
                    incomingData.parentPath
                ),
                pinned = false,
                immichId = null
            )
        )
    )

    val items = multiAlbumViewModel.mediaFlow.collectAsLazyPagingItems()
    val index = remember(items.itemCount, items.loadState) {
        (0 until items.itemCount).find {
            val item = (items.peek(it) as? PhotoLibraryUIModel.MediaImpl)?.item
            item?.id == incomingData.id
        }
    }

    LaunchedEffect(index, items.loadState) {
        if (index != null && !items.loadState.isIdle) return@LaunchedEffect

        SyncManager(context).setGeneration(0)
        FirstTimeSyncWorker.start(context)
    }

    if (index != null) {
        SinglePhotoView(
            window = window,
            viewModel = multiAlbumViewModel,
            index = index,
            editId = { 0L },
            album = AlbumType.Folder(
                id = "",
                name = "",
                paths = setOf(
                    incomingData.absolutePath.parent()
                ),
                pinned = false,
                immichId = null
            ),
            isOpenWithDefaultView = true
        )
    }
}