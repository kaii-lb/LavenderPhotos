package com.kaii.photos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.SecureFolderView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.grids.albums.AlbumGroup
import com.kaii.photos.compose.grids.albums.SingleAlbumView
import com.kaii.photos.compose.immich.ImmichAccountPage
import com.kaii.photos.compose.immich.dashboard.ImmichDashboardPage
import com.kaii.photos.compose.immich.ImmichLoginPage
import com.kaii.photos.compose.immich.backup_options_page.ImmichBackupOptionsPage
import com.kaii.photos.compose.immich.share_link_page.ImmichShareLinkPage
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
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.Settings
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.Updater
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.editor.EditorViewModel
import com.kaii.photos.models.editor.EditorViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModelFactory
import com.kaii.photos.models.immich_info_page.ImmichInfoViewModel
import com.kaii.photos.models.immich_info_page.ImmichInfoViewModelFactory
import com.kaii.photos.models.immich_share_album_page.ImmichShareAlbumViewModel
import com.kaii.photos.models.immich_share_album_page.ImmichShareAlbumViewModelFactory
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
import com.kaii.photos.screens.rememberImmichBackupOptionsState
import com.kaii.photos.ui.theme.PhotosTheme
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarBox
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.typeOf

val LocalNavController = compositionLocalOf<NavHostController> {
    throw IllegalStateException("CompositionLocal LocalNavController not present")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val settings = applicationContext.appModule.settings
        val startupManager = StartupManager(context = applicationContext)

        lifecycleScope.launch(Dispatchers.IO) {
            startupManager.checkState()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appModule.settings.permissions.setIsMediaManager(
                    MediaStore.canManageMedia(applicationContext)
                )
            }
        }

        setContent {
            val followDarkTheme by settings.lookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = 0)

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
                            when (startupManager.state) {
                                StartupManager.State.MissingPermissions -> Screens.Startup.PermissionsPage

                                StartupManager.State.NeedsIndexing -> Screens.Startup.ProcessingPage

                                else -> Screens.MainPages
                            }
                    )
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

        val context = LocalContext.current
        val navController = LocalNavController.current

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val canRecordLogs = settings.debugging.getRecordLogs().first()
                if (canRecordLogs) {
                    val logManager = LogManager(context = context)
                    logManager.startRecording()
                }

                val checkForUpdatesOnStartup = settings.versions.getCheckUpdatesOnStartup().first()
                if (checkForUpdatesOnStartup) {
                    val updater = Updater(
                        context = context,
                        coroutineScope = lifecycleScope
                    )
                    updater.startupUpdateCheck(navController)
                }

                val hasClearedCache = settings.versions.getHasClearedGlideCache().first()
                if (!hasClearedCache) {
                    settings.storage.clearThumbnailCache()
                    settings.versions.setHasClearedGlideCache(true)
                }
            }
        }

        val snackbarHostState = remember { LavenderSnackbarHostState() }

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

                composable<Screens.Startup.ProcessingPage> {
                    StartupLoadingPage(startupManager = startupManager)
                }

                navigation<Screens.MainPages>(
                    startDestination = Screens.MainPages.MainGrid.GridView
                ) {
                    composable<Screens.MainPages.MainGrid.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Folder>() to AlbumType.Folder.NavType()
                        )
                    ) {
                        setupNextScreen(window = window)

                        val deviceAlbums by appModule.albumGridState.albums.collectAsStateWithLifecycle()
                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.MainPages)
                        }
                        val viewModel = viewModel<MainGridViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = MainGridViewModelFactory(context = context)
                        )

                        val searchViewModel = it.sharedViewModel<SearchViewModel>(
                            factory = SearchViewModelFactory(context = context)
                        )

                        MainPages(
                            viewModel = viewModel,
                            searchViewModel = searchViewModel,
                            deviceAlbums = { deviceAlbums },
                            window = window,
                            incomingIntent = null,
                            refreshAlbums = appModule.albumGridState::refresh
                        )
                    }

                    composable<Screens.MainPages.MainGrid.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Folder>() to AlbumType.Folder.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.MainPages.MainGrid.SinglePhoto>()
                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.MainPages)
                        }
                        val viewModel = viewModel<MainGridViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = MainGridViewModelFactory(context = context)
                        )

                        LaunchedEffect(Unit) {
                            viewModel.changeAlbum(screen.album)
                        }

                        val editId by it.savedStateHandle.getStateFlow(
                            key = "editId",
                            initialValue = -1L
                        ).collectAsStateWithLifecycle()

                        SinglePhotoView(
                            window = window,
                            viewModel = viewModel,
                            index = screen.index,
                            editId = { editId },
                            album = screen.album
                        )
                    }

                    composable<Screens.MainPages.Search.SinglePhoto> {
                        val screen = it.toRoute<Screens.MainPages.Search.SinglePhoto>()
                        val editId by it.savedStateHandle.getStateFlow(
                            key = "editId",
                            initialValue = -1L
                        ).collectAsStateWithLifecycle()

                        val searchViewModel = it.sharedViewModel<SearchViewModel>(
                            factory = SearchViewModelFactory(context = context)
                        )

                        SinglePhotoView(
                            window = window,
                            viewModel = searchViewModel,
                            index = screen.index,
                            editId = { editId }
                        )
                    }
                }

                navigation<Screens.Album>(
                    startDestination = Screens.Album.GridView::class
                ) {
                    composable<Screens.Album.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Folder>() to AlbumType.Folder.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.Album.GridView>()
                        setupNextScreen(window = window)

                        val multiAlbumViewModel = it.sharedViewModel<MultiAlbumViewModel>(
                            factory = MultiAlbumViewModelFactory(
                                context = context,
                                album = screen.album
                            )
                        )
                        multiAlbumViewModel.changeAlbum(album = screen.album)

                        SingleAlbumView(
                            album = screen.album,
                            viewModel = multiAlbumViewModel
                        )
                    }

                    composable<Screens.Album.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Folder>() to AlbumType.Folder.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.Album.SinglePhoto>()
                        val multiAlbumViewModel = it.sharedViewModel<MultiAlbumViewModel>(
                            factory = MultiAlbumViewModelFactory(
                                context = context,
                                album = screen.album
                            )
                        )
                        multiAlbumViewModel.changeAlbum(album = screen.album)

                        val editId by it.savedStateHandle.getStateFlow(
                            key = "editId",
                            initialValue = -1L
                        ).collectAsStateWithLifecycle()

                        SinglePhotoView(
                            window = window,
                            viewModel = multiAlbumViewModel,
                            index = screen.index,
                            editId = { editId },
                            album = screen.album
                        )
                    }
                }

                navigation<Screens.Favourites>(
                    startDestination = Screens.Favourites.GridView
                ) {
                    composable<Screens.Favourites.GridView> {
                        setupNextScreen(window = window)

                        val viewModel = it.sharedViewModel<FavouritesViewModel>(
                            factory = FavouritesViewModelFactory(context = context)
                        )

                        FavouritesGridView(viewModel = viewModel)
                    }

                    composable<Screens.Favourites.SinglePhoto> {
                        val viewModel = it.sharedViewModel<FavouritesViewModel>(
                            factory = FavouritesViewModelFactory(context = context)
                        )

                        val screen = it.toRoute<Screens.Favourites.SinglePhoto>()
                        val editId by it.savedStateHandle.getStateFlow(
                            key = "editId",
                            initialValue = -1L
                        ).collectAsStateWithLifecycle()

                        SinglePhotoView(
                            viewModel = viewModel,
                            window = window,
                            index = screen.index,
                            editId = { editId }
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

                        val viewModel = it.sharedViewModel<TrashViewModel>(
                            factory = TrashViewModelFactory(context = context)
                        )

                        TrashedPhotoGridView(viewModel = viewModel)
                    }

                    composable<Screens.Trash.SinglePhoto> {
                        val screen = it.toRoute<Screens.Trash.SinglePhoto>()

                        val viewModel = it.sharedViewModel<TrashViewModel>(
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

                        val viewModel = it.sharedViewModel<SecureFolderViewModel>(
                            factory = SecureFolderViewModelFactory(context = context)
                        )

                        SecureFolderView(window = window, viewModel = viewModel)
                    }

                    composable<Screens.SecureFolder.SinglePhoto> {
                        val viewModel = it.sharedViewModel<SecureFolderViewModel>(
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
                    startDestination = Screens.Immich.Dashboard
                ) {
                    composable<Screens.Immich.Dashboard> {
                        val viewModel = it.sharedViewModel<ImmichInfoViewModel>(
                            factory = ImmichInfoViewModelFactory(
                                context = context
                            )
                        )

                        ImmichDashboardPage(viewModel = viewModel)
                    }

                    composable<Screens.Immich.Account> {
                        val viewModel = it.sharedViewModel<ImmichInfoViewModel>(
                            factory = ImmichInfoViewModelFactory(
                                context = context
                            )
                        )

                        ImmichAccountPage(viewModel = viewModel)
                    }

                    composable<Screens.Immich.Login> {
                        val viewModel = it.sharedViewModel<ImmichInfoViewModel>(
                            factory = ImmichInfoViewModelFactory(
                                context = context
                            )
                        )

                        ImmichLoginPage(viewModel = viewModel)
                    }

                    composable<Screens.Immich.BackupOptions> {
                        ImmichBackupOptionsPage(
                            state = rememberImmichBackupOptionsState(),
                            navController = navController
                        )
                    }

                    composable<Screens.Immich.ShareAlbumPage> {
                        val screen = it.toRoute<Screens.Immich.ShareAlbumPage>()
                        val viewModel = it.sharedViewModel<ImmichShareAlbumViewModel>(
                            factory = ImmichShareAlbumViewModelFactory(
                                context = context,
                                albumImmichId = screen.albumImmichId
                            )
                        )

                        ImmichShareLinkPage(
                            latestImage = screen.latestImage,
                            albumTitle = screen.albumTitle,
                            itemCount = screen.itemCount,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.Immich.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Cloud>() to AlbumType.Cloud.NavType()
                        )
                    ) {
                        setupNextScreen(window = window)

                        val screen = it.toRoute<Screens.Immich.GridView>()
                        val viewModel = it.sharedViewModel<ImmichAlbumViewModel>(
                            factory = ImmichAlbumViewModelFactory(
                                context = context,
                                album = screen.album
                            )
                        )

                        SingleAlbumView(
                            album = screen.album,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.Immich.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Cloud>() to AlbumType.Cloud.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.Immich.SinglePhoto>()

                        val viewModel = it.sharedViewModel<ImmichAlbumViewModel>(
                            factory = ImmichAlbumViewModelFactory(
                                context = context,
                                album = screen.album
                            )
                        )

                        val editId by it.savedStateHandle.getStateFlow(
                            key = "editId",
                            initialValue = -1L
                        ).collectAsStateWithLifecycle()

                        SinglePhotoView(
                            viewModel = viewModel,
                            window = window,
                            index = screen.index,
                            editId = { editId },
                            album = screen.album
                        )
                    }
                }

                navigation<Screens.CustomAlbum>(
                    startDestination = Screens.CustomAlbum.GridView::class
                ) {
                    composable<Screens.CustomAlbum.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Custom>() to AlbumType.Custom.NavType()
                        )
                    ) {
                        setupNextScreen(window = window)

                        val screen = it.toRoute<Screens.CustomAlbum.GridView>()
                        val viewModel = it.sharedViewModel<CustomAlbumViewModel>(
                            factory = CustomAlbumViewModelFactory(
                                context = context,
                                album = screen.album
                            )
                        )

                        SingleAlbumView(
                            album = screen.album,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.CustomAlbum.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Custom>() to AlbumType.Custom.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.CustomAlbum.SinglePhoto>()
                        val viewModel = it.sharedViewModel<CustomAlbumViewModel>(
                            factory = CustomAlbumViewModelFactory(
                                context = context,
                                album = screen.album
                            )
                        )

                        val editId by it.savedStateHandle.getStateFlow(
                            key = "editId",
                            initialValue = -1L
                        ).collectAsStateWithLifecycle()

                        SinglePhotoView(
                            album = screen.album,
                            viewModel = viewModel,
                            index = screen.index,
                            editId = { editId },
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

                composable<Screens.AlbumGroup> {
                    val screen = it.toRoute<Screens.AlbumGroup>()

                    AlbumGroup(
                        id = screen.id,
                        name = screen.name,
                        albumGridState = appModule.albumGridState
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
                    setupNextScreen(window = window)

                    val screen: Screens.ImageEditor = it.toRoute()
                    val viewModel = viewModel<EditorViewModel>(
                        factory = EditorViewModelFactory(context = context)
                    )

                    val exitOnSave by viewModel.exitOnSave.collectAsStateWithLifecycle()
                    val overwriteByDefault by viewModel.overwriteByDefault.collectAsStateWithLifecycle()
                    val exportQuality by viewModel.exportQuality.collectAsStateWithLifecycle()

                    ImageEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        isFromOpenWithView = false,
                        album = screen.album,
                        exportQuality = { exportQuality },
                        exitOnSave = { exitOnSave },
                        overwriteByDefault = { overwriteByDefault }
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
                    setupNextScreen(window)

                    val screen = it.toRoute<Screens.VideoEditor>()

                    VideoEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        album = screen.album,
                        window = window,
                        isFromOpenWithView = false
                    )
                }
            }
        }

        ReportDrawn()
    }

    override fun onResume() {
        super.onResume()

        appModule.scope.launch(Dispatchers.IO) {
            if (SyncManager(applicationContext).getGeneration() > 0L) {
                delay(2000) // so it isn't immediate on startup

                // run work manager immediately after user navigates back to app
                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        SyncWorker::class.java.name,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.Builder(SyncWorker::class).build()
                    )
            }
        }
    }
}

fun setupNextScreen(window: Window) {
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    factory: ViewModelProvider.Factory? = null
): T {
    val navGraphRoute = destination.parent?.route ?: return viewModel(factory = factory)
    val navController = LocalNavController.current
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return viewModel(viewModelStoreOwner = parentEntry, factory = factory)
}