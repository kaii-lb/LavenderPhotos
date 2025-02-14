package com.kaii.photos

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.MainAppBottomBar
import com.kaii.photos.compose.MainAppDialog
import com.kaii.photos.compose.MainAppSelectingBottomBar
import com.kaii.photos.compose.MainAppTopBar
import com.kaii.photos.compose.PermissionHandler
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.getAppBarContentTransition
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.LockedFolderView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.setBarVisibility
import com.kaii.photos.compose.settings.AboutPage
import com.kaii.photos.compose.settings.DebuggingSettingsPage
import com.kaii.photos.compose.settings.GeneralSettingsPage
import com.kaii.photos.compose.settings.LookAndFeelSettingsPage
import com.kaii.photos.compose.settings.MainSettingsPage
import com.kaii.photos.compose.settings.MemoryAndStorageSettingsPage
import com.kaii.photos.compose.settings.UpdatesPage
import com.kaii.photos.compose.single_photo.EditingView
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.Migration3to4
import com.kaii.photos.database.Migration4to5
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import java.io.File

private const val TAG = "MAIN_ACTIVITY"

val LocalNavController = compositionLocalOf<NavHostController> {
    throw IllegalStateException("CompositionLocal LocalNavController not present")
}

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var applicationDatabase: MediaDatabase
        lateinit var mainViewModel: MainViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val mediaDatabase = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).apply {
            addMigrations(Migration3to4(applicationContext), Migration4to5(applicationContext))
        }.build()
        applicationDatabase = mediaDatabase

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        setContent {
            mainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext)
            )

            val continueToApp = remember {
                // Manifest.permission.MANAGE_MEDIA is optional
                mainViewModel.startupPermissionCheck(applicationContext)
                mutableStateOf(
                    mainViewModel.checkCanPass()
                )
            }

            val initial =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }
            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(
                initialValue = initial
            )

            PhotosTheme(
                darkTheme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                AnimatedContent(
                    targetState = continueToApp.value,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                            .using(
                                SizeTransform(clip = false)
                            )
                    },
                    label = "PermissionHandlerToMainViewAnimatedContent"
                ) { stateValue ->
                    if (!stateValue) {
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

                        PermissionHandler(continueToApp)
                    } else {
                        SetContentForActivity()
                    }
                }
            }
        }
    }

    @Composable
    private fun SetContentForActivity() {
        window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

        val navControllerLocal = rememberNavController()

        val currentView =
            rememberSaveable { mutableStateOf(MainScreenViewType.PhotosGridView) }

        val context = LocalContext.current
        val showDialog = remember { mutableStateOf(false) }

        val scale = remember { mutableFloatStateOf(1f) }
        val rotation = remember { mutableFloatStateOf(0f) }
        val offset = remember { mutableStateOf(Offset.Zero) }
        val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }

        val logPath = "${context.appStorageDir}/log.txt"
        Log.d(TAG, "Log save path is $logPath")

        val canRecordLogs by mainViewModel.settings.Debugging.getRecordLogs().collectAsStateWithLifecycle(initialValue = false)

        LaunchedEffect(canRecordLogs) {
            if (canRecordLogs) {
                try {
                    File(logPath).delete()
                    Runtime.getRuntime().exec("logcat -c")
                    Runtime.getRuntime().exec("logcat -f $logPath")
                } catch (e: Throwable) {
                    Log.e(TAG, e.toString())
                }
            }
        }

        mainViewModel.settings.AlbumsList.addToAlbumsList("DCIM/Camera")

        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albums = albumsList,
                sortBy = MediaItemSortMode.DateTaken
            )
        )

        LaunchedEffect(albumsList) {
            if (navControllerLocal.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name) return@LaunchedEffect

            Log.d(TAG, "Refreshing main photos view")
            Log.d(TAG, "In view model: ${multiAlbumViewModel.albums} new: $albumsList")
            multiAlbumViewModel.reinitDataSource(
                context = context,
                albumsList = albumsList,
                sortBy = MediaItemSortMode.DateTaken
            )
        }

        CompositionLocalProvider(LocalNavController provides navControllerLocal) {
            NavHost(
                navController = navControllerLocal,
                startDestination = MultiScreenViewType.MainScreen.name,
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
                composable(MultiScreenViewType.MainScreen.name) {
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
                    setupNextScreen(
                        selectedItemsList = selectedItemsList,
                        window = window
                    )

                    if (albumsList != multiAlbumViewModel.albums) {
                        multiAlbumViewModel.reinitDataSource(
                            context = context,
                            albumsList = albumsList,
                            sortBy = multiAlbumViewModel.sortBy
                        )
                    }

                    Content(currentView, showDialog, selectedItemsList, multiAlbumViewModel)
                }

                composable<Screens.SinglePhotoView> {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(
                            MaterialTheme.colorScheme.surfaceContainer.copy(
                                alpha = 0.2f
                            ).toArgb()
                        ),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.SinglePhotoView = it.toRoute()

                    if (screen.albums != multiAlbumViewModel.albums) {
                        multiAlbumViewModel.reinitDataSource(
                            context = context,
                            albumsList = screen.albums,
                            sortBy = multiAlbumViewModel.sortBy
                        )
                    }

                    SinglePhotoView(
                        navController = navControllerLocal,
                        window = window,
                        scale = scale,
                        rotation = rotation,
                        offset = offset,
                        viewModel = multiAlbumViewModel,
                        mediaItemId = screen.mediaItemId,
                        loadsFromMainViewModel = screen.loadsFromMainViewModel
                    )
                }

                composable<Screens.SingleAlbumView> {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.surface.toArgb(),
                            MaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.SingleAlbumView = it.toRoute()

                    if (screen.albums != multiAlbumViewModel.albums) {
                        multiAlbumViewModel.reinitDataSource(
                            context = context,
                            albumsList = screen.albums,
                            sortBy = multiAlbumViewModel.sortBy
                        )
                    }

                    SingleAlbumView(
                        selectedItemsList = selectedItemsList,
                        currentView = currentView,
                        viewModel = multiAlbumViewModel
                    )
                }

                composable<Screens.SingleTrashedPhotoView> {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.surface.toArgb(),
                            MaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.SingleTrashedPhotoView = it.toRoute()

                    SingleTrashedPhotoView(
                        window = window,
                        scale = scale,
                        rotation = rotation,
                        offset = offset,
                        mediaItemId = screen.mediaItemId
                    )
                }

                composable(MultiScreenViewType.TrashedPhotoView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.surface.toArgb(),
                            MaterialTheme.colorScheme.surface.toArgb()
                        )
                    )

                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    TrashedPhotoGridView(
                        selectedItemsList = selectedItemsList,
                        currentView = currentView
                    )
                }

                composable(MultiScreenViewType.LockedFolderView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.surface.toArgb(),
                            MaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    LockedFolderView(window = window, currentView = currentView)
                }

                composable<Screens.SingleHiddenPhotoView> {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.surface.toArgb(),
                            MaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.SingleHiddenPhotoView = it.toRoute()

                    SingleHiddenPhotoView(
                        mediaItemId = screen.mediaItemId,
                        window = window,
                        scale = scale,
                        rotation = rotation,
                        offset = offset
                    )
                }

                composable(MultiScreenViewType.AboutAndUpdateView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    AboutPage {
                        navControllerLocal.popBackStack()
                    }
                }

                composable(MultiScreenViewType.FavouritesGridView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    FavouritesGridView(
                        selectedItemsList = selectedItemsList,
                        currentView = currentView
                    )
                }

                composable<Screens.EditingScreen>(
                    enterTransition = {
                        slideInVertically(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        ) { height -> height } + fadeIn(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        )
                    },
                    exitTransition = {
                        slideOutVertically(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        ) { height -> height } + fadeOut(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        )
                    },
                    popEnterTransition = {
                        slideInVertically(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        ) { height -> height } + fadeIn(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        )
                    },
                    popExitTransition = {
                        slideOutVertically(
                            animationSpec = tween(
                                durationMillis = 600
                            )
                        ) { height -> height } + fadeOut(
                            animationSpec = tween(
                                durationMillis = 600
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
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.EditingScreen = it.toRoute()
                    val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)

                    EditingView(
                        absolutePath = screen.absolutePath,
                        dateTaken = screen.dateTaken,
                        uri = screen.uri.toUri(),
                        window = window,
                        overwriteByDefault = overwriteByDefault
                    )
                }

                composable(MultiScreenViewType.SettingsMainView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    MainSettingsPage()
                }

                composable(MultiScreenViewType.SettingsDebuggingView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    DebuggingSettingsPage()
                }

                composable(MultiScreenViewType.SettingsGeneralView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    GeneralSettingsPage()
                }

                composable(MultiScreenViewType.SettingsMemoryAndStorageView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    MemoryAndStorageSettingsPage()
                }

                composable(MultiScreenViewType.SettingsLookAndFeelView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    LookAndFeelSettingsPage()
                }

                composable(MultiScreenViewType.UpdatesPage.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            MaterialTheme.colorScheme.background.toArgb(),
                            MaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    UpdatesPage()
                }
            }
        }
    }

    @Composable
    private fun Content(
        currentView: MutableState<MainScreenViewType>,
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        multiAlbumViewModel: MultiAlbumViewModel,
    ) {
        val mediaStoreData =
            multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

        val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }

        LaunchedEffect(mediaStoreData.value) {
            groupedMedia.value = mediaStoreData.value
        }

        Scaffold(
            topBar = {
                TopBar(showDialog = showDialog, selectedItemsList = selectedItemsList, currentView = currentView)
            },
            bottomBar = {
                BottomBar(currentView = currentView, selectedItemsList = selectedItemsList)
            },
            modifier = Modifier
                .fillMaxSize(1f)
        ) { padding ->
            val localConfig = LocalConfiguration.current
            var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

            LaunchedEffect(localConfig) {
                isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            }

            val safeDrawingPadding = if (isLandscape) {
                val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

                val layoutDirection = LocalLayoutDirection.current
                val left = safeDrawing.calculateStartPadding(layoutDirection)
                val right = safeDrawing.calculateEndPadding(layoutDirection)

                Pair(left, right)
            } else {
                Pair(0.dp, 0.dp)
            }

            Column(
                modifier = Modifier
                    .padding(
                        safeDrawingPadding.first,
                        padding.calculateTopPadding(),
                        safeDrawingPadding.second,
                        padding.calculateBottomPadding()
                    )
            ) {
                MainAppDialog(showDialog, currentView, selectedItemsList)

                val context = LocalContext.current
                AnimatedContent(
                    targetState = currentView.value,
                    transitionSpec = {
                        if (targetState.index > initialState.index) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "MainAnimatedContentView"
                ) { stateValue ->
                    when (stateValue) {
                        MainScreenViewType.PhotosGridView -> {
                            selectedItemsList.clear()

                            PhotoGrid(
                                groupedMedia = groupedMedia,
                                albums = multiAlbumViewModel.albums,
                                viewProperties = ViewProperties.Album,
                                selectedItemsList = selectedItemsList,
                            )
                        }

                        MainScreenViewType.SecureFolder -> LockedFolderEntryView(currentView)
                        MainScreenViewType.AlbumsGridView -> {
                            AlbumsGridView(currentView)
                        }

                        MainScreenViewType.SearchPage -> {
                            selectedItemsList.clear()

                            SearchPage(selectedItemsList, currentView)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        currentView: MutableState<MainScreenViewType>
    ) {
        val show by remember {
            derivedStateOf {
                selectedItemsList.size > 0
            }
        }

        MainAppTopBar(
            alternate = show,
            showDialog = showDialog,
            selectedItemsList = selectedItemsList,
            currentView = currentView
        )
    }

    @Composable
    private fun BottomBar(
        currentView: MutableState<MainScreenViewType>,
        selectedItemsList: SnapshotStateList<MediaStoreData>
    ) {
        val navController = LocalNavController.current
        val show by remember {
            derivedStateOf {
                selectedItemsList.size > 0
            }
        }

        AnimatedContent(
            targetState = show && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
            transitionSpec = {
                getAppBarContentTransition(show)
            },
            label = "MainBottomBarAnimatedContentView"
        ) { state ->
            if (!state) {
                MainAppBottomBar(currentView, selectedItemsList)
            } else {
                MainAppSelectingBottomBar(selectedItemsList)
            }
        }
    }
}

private fun setupNextScreen(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    window: Window
) {
    selectedItemsList.clear()
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    window.setDecorFitsSystemWindows(false)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}
