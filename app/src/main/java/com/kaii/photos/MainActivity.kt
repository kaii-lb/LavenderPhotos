package com.kaii.photos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.lavender.snackbars.LavenderSnackbarBox
import com.kaii.lavender.snackbars.LavenderSnackbarHostState
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.SecureFolderView
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.immich.ImmichInfoPage
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.pages.PermissionHandler
import com.kaii.photos.compose.pages.StartupLoadingPage
import com.kaii.photos.compose.pages.main.MainPages
import com.kaii.photos.compose.settings.BehaviourSettingsPage
import com.kaii.photos.compose.settings.DataAndBackupPage
import com.kaii.photos.compose.settings.DebuggingSettingsPage
import com.kaii.photos.compose.settings.ExtendedLicensePage
import com.kaii.photos.compose.settings.GeneralSettingsPage
import com.kaii.photos.compose.settings.LicensePage
import com.kaii.photos.compose.settings.LookAndFeelSettingsPage
import com.kaii.photos.compose.settings.MemoryAndStorageSettingsPage
import com.kaii.photos.compose.settings.PrivacyAndSecurityPage
import com.kaii.photos.compose.settings.UpdatesPage
import com.kaii.photos.compose.single_photo.SecurePhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.datastore.state.rememberAlbumGridState
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.helpers.rememberUpdater
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModelFactory
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.main_grid.MainGridViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.permissions.PermissionsViewModel
import com.kaii.photos.models.permissions.PermissionsViewModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.models.secure_folder.SecureFolderViewModel
import com.kaii.photos.models.secure_folder.SecureFolderViewModelFactory
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.permissions.StartupManager
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.reflect.typeOf

private const val TAG = "com.kaii.photos.MainActivity"

val LocalNavController = compositionLocalOf<NavHostController> {
    throw IllegalStateException("CompositionLocal LocalNavController not present")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val settings = applicationContext.appModule.settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            settings.permissions.setIsMediaManager(MediaStore.canManageMedia(applicationContext))
        }

        val startupManager = StartupManager(context = applicationContext)
        runBlocking { startupManager.checkState() }

        val initialFollowDarkTheme = runBlocking {
            settings.lookAndFeel.getFollowDarkMode().first()
        }

        setContent {
            val startupState by startupManager.state.collectAsStateWithLifecycle()

            val followDarkTheme by settings.lookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(initialValue = initialFollowDarkTheme)

            PhotosTheme(
                theme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                lavenderEdgeToEdge(
                    isDarkMode = isSystemInDarkTheme(),
                    navBarColor = Color.Transparent,
                    statusBarColor = Color.Transparent
                )

                val navControllerLocal = rememberNavController()
                CompositionLocalProvider(
                    LocalNavController provides navControllerLocal
                ) {
                    SetContentForActivity(
                        startupManager = startupManager,
                        settings = settings,
                        startupPage =
                            when (startupState) {
                                StartupManager.State.MissingPermissions -> Screens.Startup.PermissionsPage

                                StartupManager.State.NeedsIndexing -> Screens.Startup.ProcessingPage

                                else -> Screens.MainPages.MainGrid.GridView
                            }
                    )
                }
            }

            val hasClearedCache by settings.versions.getHasClearedGlideCache().collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(hasClearedCache) {
                if (!hasClearedCache) {
                    settings.storage.clearThumbnailCache()
                    settings.versions.setHasClearedGlideCache(true)
                }
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun SetContentForActivity(
        startupManager: StartupManager,
        settings: Settings,
        startupPage: Screens
    ) {
        window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

        // TODO: remove from here
        val context = LocalContext.current
        val logPath = "${context.appStorageDir}/log.txt"
        Log.d(TAG, "Log save path is $logPath")

        val canRecordLogs by settings.debugging.getRecordLogs()
            .collectAsStateWithLifecycle(initialValue = false)

        LaunchedEffect(canRecordLogs) {
            if (canRecordLogs) {
                val logManager = LogManager(context = context)
                logManager.startRecording()
            }
        }

        val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(context = context))
        val multiAlbumViewModel = viewModel<MultiAlbumViewModel>(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.Empty
            )
        )

        val deviceAlbums = rememberAlbumGridState().albums.collectAsStateWithLifecycle()
        val snackbarHostState = remember { LavenderSnackbarHostState() }

        val navController = LocalNavController.current
        LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = startupPage,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .background(MaterialTheme.colorScheme.background),
                enterTransition = {
                    slideInHorizontally { width -> width } + fadeIn()
                },
                exitTransition = {
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                popExitTransition = {
                    slideOutHorizontally { width -> width } + fadeOut()
                },
                popEnterTransition = {
                    slideInHorizontally { width -> -width } + fadeIn()
                }
            ) {
                composable<Screens.Startup.PermissionsPage> {
                    val viewModel = viewModel<PermissionsViewModel>(factory = PermissionsViewModelFactory(context))

                    PermissionHandler(
                        startupManager = startupManager,
                        viewModel = viewModel
                    )
                }

                composable<Screens.Startup.PermissionsPage> {
                    StartupLoadingPage(
                        startupManager = startupManager,
                        window = window
                    )
                }

                navigation<Screens.MainPages>(
                    startDestination = Screens.MainPages.MainGrid.GridView
                ) {
                    composable<Screens.MainPages.MainGrid.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(window = window)

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.MainPages)
                        }
                        val viewModel = viewModel<MainGridViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = MainGridViewModelFactory(context = context)
                        )

                        MainPages(
                            multiAlbumViewModel = multiAlbumViewModel,
                            searchViewModel = searchViewModel,
                            mainGridViewModel = viewModel,
                            deviceAlbums = deviceAlbums,
                            window = window,
                            incomingIntent = null
                        )
                    }

                    composable<Screens.MainPages.MainGrid.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        val screen = it.toRoute<Screens.MainPages.MainGrid.SinglePhoto>()
                        multiAlbumViewModel.changePaths(album = screen.albumInfo)

                        val editIndex = it.savedStateHandle.get<Int>("editIndex")
                        SinglePhotoView(
                            window = window,
                            viewModel = multiAlbumViewModel,
                            index = editIndex ?: screen.index,
                            albumInfo = screen.albumInfo
                        )
                    }

                    composable<Screens.MainPages.Search.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        val screen = it.toRoute<Screens.MainPages.Search.SinglePhoto>()
                        val editIndex = it.savedStateHandle.get<Int>("editIndex")

                        SinglePhotoView(
                            window = window,
                            viewModel = searchViewModel,
                            index = editIndex ?: screen.index,
                            albumInfo = AlbumInfo.Empty
                        )
                    }
                }

                navigation<Screens.Album>(
                    startDestination = Screens.Album.GridView::class
                ) {
                    composable<Screens.Album.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        val screen = it.toRoute<Screens.Album.GridView>()
                        setupNextScreen(window = window)

                        multiAlbumViewModel.changePaths(album = screen.albumInfo)

                        SingleAlbumView(
                            albumInfo = screen.albumInfo,
                            viewModel = multiAlbumViewModel
                        )
                    }

                    composable<Screens.Album.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        val screen = it.toRoute<Screens.Album.SinglePhoto>()
                        multiAlbumViewModel.changePaths(album = screen.albumInfo)

                        val editIndex = it.savedStateHandle.get<Int>("editIndex")
                        SinglePhotoView(
                            window = window,
                            viewModel = multiAlbumViewModel,
                            index = editIndex ?: screen.index,
                            albumInfo = screen.albumInfo
                        )
                    }
                }

                navigation<Screens.Favourites>(
                    startDestination = Screens.Favourites.GridView
                ) {
                    composable<Screens.Favourites.GridView> {
                        setupNextScreen(window = window)

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Favourites)
                        }
                        val viewModel = viewModel<FavouritesViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = FavouritesViewModelFactory(context = context)
                        )

                        FavouritesGridView(viewModel = viewModel)
                    }

                    composable<Screens.Favourites.SinglePhoto> {
                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Favourites)
                        }
                        val viewModel = viewModel<FavouritesViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = FavouritesViewModelFactory(context = context)
                        )

                        val screen = it.toRoute<Screens.Favourites.SinglePhoto>()
                        val editIndex = it.savedStateHandle.get<Int>("editIndex")
                        SinglePhotoView(
                            viewModel = viewModel,
                            window = window,
                            index = editIndex ?: screen.index
                        )
                    }

                    composable<Screens.Favourites.MigrationPage> {
                        FavouritesMigrationPage()
                    }
                }

                navigation<Screens.Trash>(
                    startDestination = Screens.Trash.GridView
                ) {
                    composable<Screens.Trash.GridView> {
                        setupNextScreen(window = window)

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Trash)
                        }
                        val viewModel = viewModel<TrashViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = TrashViewModelFactory(context = context)
                        )

                        TrashedPhotoGridView(viewModel = viewModel)
                    }

                    composable<Screens.Trash.SinglePhoto> {
                        val screen = it.toRoute<Screens.Trash.SinglePhoto>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Trash)
                        }
                        val viewModel = viewModel<TrashViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = TrashViewModelFactory(context = context)
                        )

                        SingleTrashedPhotoView(
                            window = window,
                            index = screen.index,
                            viewModel = viewModel
                        )
                    }
                }

                navigation<Screens.SecureFolder>(
                    startDestination = Screens.SecureFolder.GridView
                ) {
                    composable<Screens.SecureFolder.GridView> {
                        setupNextScreen(window = window)

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.SecureFolder)
                        }

                        val viewModel = viewModel<SecureFolderViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = SecureFolderViewModelFactory(context = context)
                        )

                        SecureFolderView(window = window, viewModel = viewModel)
                    }

                    composable<Screens.SecureFolder.SinglePhoto> {
                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.SecureFolder)
                        }
                        val viewModel = viewModel<SecureFolderViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = SecureFolderViewModelFactory(context = context)
                        )

                        val screen: Screens.SecureFolder.SinglePhoto = it.toRoute()

                        SecurePhotoView(
                            index = screen.index,
                            viewModel = viewModel,
                            window = window
                        )
                    }
                }

                navigation<Screens.Immich>(
                    startDestination = Screens.Immich.InfoPage
                ) {
                    composable<Screens.Immich.InfoPage> {
                        ImmichInfoPage()
                    }

                    composable<Screens.Immich.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(window = window)

                        val screen = it.toRoute<Screens.Immich.GridView>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Immich)
                        }
                        val viewModel = viewModel<ImmichAlbumViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = ImmichAlbumViewModelFactory(
                                context = context,
                                albumInfo = screen.albumInfo
                            )
                        )

                        SingleAlbumView(
                            albumInfo = screen.albumInfo,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.Immich.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        val screen = it.toRoute<Screens.Immich.SinglePhoto>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Immich)
                        }
                        val viewModel = viewModel<ImmichAlbumViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = ImmichAlbumViewModelFactory(
                                context = context,
                                albumInfo = screen.albumInfo
                            )
                        )

                        val editIndex = it.savedStateHandle.get<Int>("editIndex")
                        SinglePhotoView(
                            viewModel = viewModel,
                            window = window,
                            index = editIndex ?: screen.index,
                            albumInfo = screen.albumInfo
                        )
                    }
                }

                navigation<Screens.CustomAlbum>(
                    startDestination = Screens.CustomAlbum.GridView::class
                ) {
                    composable<Screens.CustomAlbum.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(window = window)

                        val screen = it.toRoute<Screens.CustomAlbum.GridView>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.CustomAlbum)
                        }
                        val viewModel: CustomAlbumViewModel = viewModel(
                            viewModelStoreOwner = storeOwner,
                            factory = CustomAlbumViewModelFactory(
                                context = context,
                                albumInfo = screen.albumInfo
                            )
                        )

                        SingleAlbumView(
                            albumInfo = screen.albumInfo,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.CustomAlbum.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        val screen = it.toRoute<Screens.CustomAlbum.SinglePhoto>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.CustomAlbum)
                        }
                        val viewModel: CustomAlbumViewModel = viewModel(
                            viewModelStoreOwner = storeOwner,
                            factory = CustomAlbumViewModelFactory(
                                context = context,
                                albumInfo = screen.albumInfo
                            )
                        )

                        val editIndex = it.savedStateHandle.get<Int>("editIndex")
                        SinglePhotoView(
                            albumInfo = screen.albumInfo,
                            viewModel = viewModel,
                            index = editIndex ?: screen.index,
                            window = window
                        )
                    }
                }

                navigation<Screens.Settings.MainPage>(
                    startDestination = Screens.Settings.MainPage.General
                ) {
                    composable<Screens.Settings.MainPage.General> {
                        GeneralSettingsPage()
                    }

                    composable<Screens.Settings.MainPage.PrivacyAndSecurity> {
                        PrivacyAndSecurityPage(startupManager = startupManager)
                    }

                    composable<Screens.Settings.MainPage.LookAndFeel> {
                        LookAndFeelSettingsPage()
                    }

                    composable<Screens.Settings.MainPage.Behaviour> {
                        BehaviourSettingsPage()
                    }

                    composable<Screens.Settings.MainPage.MemoryAndStorage> {
                        MemoryAndStorageSettingsPage()
                    }

                    composable<Screens.Settings.MainPage.Debugging> {
                        DebuggingSettingsPage()
                    }
                }

                navigation<Screens.Settings.Misc>(
                    startDestination = Screens.Settings.Misc.DataAndBackup
                ) {
                    composable<Screens.Settings.Misc.DataAndBackup> {
                        DataAndBackupPage()
                    }

                    composable<Screens.Settings.Misc.UpdatePage> {
                        UpdatesPage()
                    }

                    composable<Screens.Settings.Misc.LicensesPage> {
                        LicensePage()
                    }

                    composable<Screens.Settings.Misc.ExtendedLicensePage> {
                        ExtendedLicensePage()
                    }
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
                    setupNextScreen(window = window)

                    val screen: Screens.ImageEditor = it.toRoute()

                    ImageEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        isFromOpenWithView = false,
                        albumInfo = screen.albumInfo
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
                    setupNextScreen(window)

                    val screen = it.toRoute<Screens.VideoEditor>()

                    VideoEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        albumInfo = screen.albumInfo,
                        window = window,
                        isFromOpenWithView = false
                    )
                }
            }
        }

        // TODO: remove from here?
        val checkForUpdatesOnStartup by settings.versions.getCheckUpdatesOnStartup()
            .collectAsStateWithLifecycle(initialValue = false)

        if (checkForUpdatesOnStartup) {
            val updater = rememberUpdater()
            LaunchedEffect(Unit) {
                updater.startupUpdateCheck(navController)
            }
        }

        ReportDrawn()
    }

    override fun onResume() {
        super.onResume()

        // run work manager immediately after user navigates back to app
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                SyncWorker::class.java.name,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(SyncWorker::class).build()
            )
    }
}

fun setupNextScreen(window: Window) {
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}
