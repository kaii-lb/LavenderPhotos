package com.kaii.photos

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.work.ExistingWorkPolicy
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.main_dialog.MainDialog
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.SecureFolderView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.grids.albums.AlbumGroup
import com.kaii.photos.compose.grids.albums.SingleAlbumView
import com.kaii.photos.compose.immich.ImmichAccountPage
import com.kaii.photos.compose.immich.ImmichLoginPage
import com.kaii.photos.compose.immich.backup_options_page.ImmichBackupOptionsPage
import com.kaii.photos.compose.immich.dashboard.ImmichDashboardPage
import com.kaii.photos.compose.immich.share_link_page.ImmichShareLinkPage
import com.kaii.photos.compose.pages.AboutPage
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.pages.PermissionHandler
import com.kaii.photos.compose.pages.PrivacyModeActivePage
import com.kaii.photos.compose.pages.ScreenLock
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
import com.kaii.photos.compose.settings.ThemePage
import com.kaii.photos.compose.settings.UpdatesPage
import com.kaii.photos.compose.single_photo.SecurePhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.sync.SyncManager
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.sharedViewModel
import com.kaii.photos.domain.news.UpdateState
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.NullableByteArrayNavType
import com.kaii.photos.helpers.OnBackPressedEffect
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.CustomAlbumViewModel
import com.kaii.photos.models.FavouritesViewModel
import com.kaii.photos.models.ImmichAlbumViewModel
import com.kaii.photos.models.ImmichInfoViewModel
import com.kaii.photos.models.MainDialogViewModel
import com.kaii.photos.models.MainGridViewModel
import com.kaii.photos.models.MultiAlbumViewModel
import com.kaii.photos.models.PrivacyModeActiveViewModel
import com.kaii.photos.models.SearchViewModel
import com.kaii.photos.models.SecureFolderViewModel
import com.kaii.photos.models.TrashViewModel
import com.kaii.photos.models.UpdaterViewModel
import com.kaii.photos.models.behaviour.BehaviourViewModel
import com.kaii.photos.models.behaviour.BehaviourViewModelFactory
import com.kaii.photos.models.contributors.ContributorViewModel
import com.kaii.photos.models.contributors.ContributorViewModelFactory
import com.kaii.photos.models.data_and_backup.DataAndBackupViewModel
import com.kaii.photos.models.data_and_backup.DataAndBackupViewModelFactory
import com.kaii.photos.models.editor.EditorViewModel
import com.kaii.photos.models.editor.EditorViewModelFactory
import com.kaii.photos.models.immich_share_album_page.ImmichShareAlbumViewModel
import com.kaii.photos.models.permissions.PermissionsViewModel
import com.kaii.photos.models.permissions.PermissionsViewModelFactory
import com.kaii.photos.models.theme.ThemeViewModel
import com.kaii.photos.models.theme.ThemeViewModelFactory
import com.kaii.photos.permissions.StartupManager
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.screens.rememberImmichBackupOptionsState
import com.kaii.photos.ui.theme.PhotosTheme
import com.kaii.photos.widgets.ExpressivePINFieldState
import dagger.hilt.android.AndroidEntryPoint
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarBox
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.milliseconds

val LocalNavController = compositionLocalOf<NavHostController> {
    throw IllegalStateException("CompositionLocal LocalNavController not present")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isCheckingCredentials = true
        splashScreen.setKeepOnScreenCondition { isCheckingCredentials }

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val settings = PhotosApplication.appModule.settings
        val startupManager = StartupManager(
            context = applicationContext,
            settings = settings.permissions
        )

        lifecycleScope.launch(Dispatchers.IO) {
            startupManager.checkState()
            isCheckingCredentials = false
        }

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

                val navControllerLocal = rememberNavController()

                CompositionLocalProvider(
                    LocalNavController provides navControllerLocal
                ) {
                    Log.d(MainActivity::class.qualifiedName, "APP STARTUP STATE ${startupManager.state}")

                    SetContentForActivity(
                        startupManager = startupManager,
                        startupPage =
                            when (startupManager.state) {
                                StartupManager.State.MissingPermissions -> Screens.Startup.PermissionsPage
                                StartupManager.State.NeedsIndexing -> Screens.Startup.ProcessingPage
                                StartupManager.State.PasswordLocked -> Screens.Startup.ScreenLock
                                StartupManager.State.PrivacyModeActive -> Screens.Startup.PrivacyModeActive
                                StartupManager.State.Unlocked -> Screens.MainPages
                            }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun SetContentForActivity(
        startupManager: StartupManager,
        startupPage: Screens
    ) {
        window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

        val context = LocalContext.current
        navController = LocalNavController.current

        val snackbarHostState = remember { LavenderSnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = startupPage,
                modifier = Modifier
                    .fillMaxSize()
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
                    val viewModel = viewModel<PermissionsViewModel>(factory = PermissionsViewModelFactory())

                    PermissionHandler(
                        startupManager = startupManager,
                        viewModel = viewModel
                    )
                }

                composable<Screens.Startup.ProcessingPage> {
                    StartupLoadingPage(startupManager = startupManager)
                }

                composable<Screens.Startup.ScreenLock> {
                    ScreenLock(
                        action = ExpressivePINFieldState.Action.Unlock
                    )
                }

                composable<Screens.Startup.PrivacyModeActive> {
                    val viewModel = hiltViewModel<PrivacyModeActiveViewModel>()

                    PrivacyModeActivePage(
                        viewModel = viewModel
                    )
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

                        val deviceAlbums by PhotosApplication.appModule.albumGridState.albums.collectAsStateWithLifecycle()
                        val viewModel = it.sharedViewModel<MainGridViewModel>(
                            screenScope = Screens.MainPages
                        )

                        val searchViewModel = it.sharedViewModel<SearchViewModel>(
                            screenScope = Screens.MainPages
                        )

                        var lastDestination by remember { mutableStateOf(navController.currentDestination) }
                        OnBackPressedEffect { destination ->
                            val isFromSettings = lastDestination?.route?.startsWith(Screens.Settings::class.qualifiedName!!) == true
                            val isFromImmich = lastDestination?.route?.startsWith(Screens.Immich.Dashboard::class.qualifiedName!!) == true
                            val isMainGrid = destination.hasRoute(Screens.MainPages.MainGrid.GridView::class)

                            if ((isFromSettings || isFromImmich) && isMainGrid) {
                                navController.navigate(Screens.MainPages.MainGrid.SettingsDialog)
                            }

                            lastDestination = destination
                        }

                        LaunchedEffect(Unit) {
                            viewModel.updateStateChannel.collectLatest { updateState ->
                                if (updateState == UpdateState.Available) {
                                    LavenderSnackbarController.pushEvent(
                                        event = LavenderSnackbarEvent.ActionEvent(
                                            message = resources.getString(R.string.updates_new_version_available),
                                            icon = R.drawable.update,
                                            actionIcon = R.drawable.download,
                                            action = {
                                                navController.navigate(Screens.Settings.Misc.UpdatePage)
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        MainPages(
                            viewModel = viewModel,
                            searchViewModel = searchViewModel,
                            deviceAlbums = { deviceAlbums },
                            window = window,
                            incomingIntent = null,
                            refreshAlbums = PhotosApplication.appModule.albumGridState::refresh
                        )
                    }

                    dialog<Screens.MainPages.MainGrid.SettingsDialog> {
                        val viewModel = it.sharedViewModel<MainDialogViewModel>(
                            screenScope = Screens.MainPages
                        )

                        val sheetState = rememberBottomSheetState(
                            initialValue = SheetValue.Hidden,
                            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
                        )

                        val extraSecureFolderNavEntry by viewModel.extraSecureFolderNavEntry.collectAsStateWithLifecycle()
                        val immichInfo by viewModel.immichInfo.collectAsStateWithLifecycle()

                        MainDialog(
                            sheetState = sheetState,
                            coroutineScope = coroutineScope,
                            extraSecureFolderEntry = { extraSecureFolderNavEntry },
                            immichInfo = { immichInfo },
                            dismiss = {
                                sheetState.hide()
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<Screens.MainPages.MainGrid.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Folder>() to AlbumType.Folder.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.MainPages.MainGrid.SinglePhoto>()
                        val viewModel = it.sharedViewModel<MainGridViewModel>(
                            screenScope = Screens.MainPages
                        )

                        LaunchedEffect(Unit) {
                            viewModel.changeAlbum(
                                paths = screen.album.paths
                            )
                        }

                        val editId by it.savedStateHandle.getStateFlow<Long?>(
                            key = "editId",
                            initialValue = null
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
                        val editId by it.savedStateHandle.getStateFlow<Long?>(
                            key = "editId",
                            initialValue = null
                        ).collectAsStateWithLifecycle()

                        val searchViewModel = it.sharedViewModel<SearchViewModel>(
                            screenScope = Screens.MainPages
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

                        val viewModel = it.sharedViewModel<MultiAlbumViewModel, MultiAlbumViewModel.Factory>(
                            screenScope = Screens.Album,
                            creationCallback = { factory ->
                                factory.create(album = screen.album)
                            }
                        )

                        LaunchedEffect(Unit) {
                            viewModel.changeAlbum(
                                album = screen.album
                            )
                        }

                        SingleAlbumView(
                            album = screen.album,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.Album.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumType.Folder>() to AlbumType.Folder.NavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.Album.SinglePhoto>()
                        val viewModel = it.sharedViewModel<MultiAlbumViewModel, MultiAlbumViewModel.Factory>(
                            screenScope = Screens.Album,
                            creationCallback = { factory ->
                                factory.create(album = screen.album)
                            }
                        )

                        LaunchedEffect(Unit) {
                            viewModel.changeAlbum(
                                album = screen.album
                            )
                        }

                        val editId by it.savedStateHandle.getStateFlow<Long?>(
                            key = "editId",
                            initialValue = null
                        ).collectAsStateWithLifecycle()

                        SinglePhotoView(
                            window = window,
                            viewModel = viewModel,
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
                            screenScope = Screens.Favourites
                        )

                        FavouritesGridView(viewModel = viewModel)
                    }

                    composable<Screens.Favourites.SinglePhoto> {
                        val viewModel = it.sharedViewModel<FavouritesViewModel>(
                            screenScope = Screens.Favourites
                        )

                        val screen = it.toRoute<Screens.Favourites.SinglePhoto>()
                        val editId by it.savedStateHandle.getStateFlow<Long?>(
                            key = "editId",
                            initialValue = null
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
                            screenScope = Screens.Trash
                        )

                        TrashedPhotoGridView(viewModel = viewModel)
                    }

                    composable<Screens.Trash.SinglePhoto> {
                        val screen = it.toRoute<Screens.Trash.SinglePhoto>()

                        val viewModel = it.sharedViewModel<TrashViewModel>(
                            screenScope = Screens.Trash
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
                            screenScope = Screens.SecureFolder
                        )

                        SecureFolderView(window = window, viewModel = viewModel)
                    }

                    composable<Screens.SecureFolder.SinglePhoto> {
                        val viewModel = it.sharedViewModel<SecureFolderViewModel>(
                            screenScope = Screens.SecureFolder
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
                            screenScope = Screens.Immich
                        )

                        ImmichDashboardPage(viewModel = viewModel)
                    }

                    composable<Screens.Immich.Account> {
                        val viewModel = it.sharedViewModel<ImmichInfoViewModel>(
                            screenScope = Screens.Immich
                        )

                        ImmichAccountPage(viewModel = viewModel)
                    }

                    composable<Screens.Immich.Login> {
                        val viewModel = it.sharedViewModel<ImmichInfoViewModel>(
                            screenScope = Screens.Immich
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
                            screenScope = Screens.Immich
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
                        val viewModel = it.sharedViewModel<ImmichAlbumViewModel, ImmichAlbumViewModel.Factory>(
                            screenScope = Screens.Immich,
                            creationCallback = { factory ->
                                factory.create(album = screen.album)
                            }
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

                        val viewModel = it.sharedViewModel<ImmichAlbumViewModel, ImmichAlbumViewModel.Factory>(
                            screenScope = Screens.Immich,
                            creationCallback = { factory ->
                                factory.create(album = screen.album)
                            }
                        )

                        val editId by it.savedStateHandle.getStateFlow<Long?>(
                            key = "editId",
                            initialValue = null
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
                        val viewModel = it.sharedViewModel<CustomAlbumViewModel, CustomAlbumViewModel.Factory>(
                            screenScope = Screens.CustomAlbum,
                            creationCallback = { factory ->
                                factory.create(album = screen.album)
                            }
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
                        val viewModel = it.sharedViewModel<CustomAlbumViewModel, CustomAlbumViewModel.Factory>(
                            screenScope = Screens.CustomAlbum,
                            creationCallback = { factory ->
                                factory.create(album = screen.album)
                            }
                        )

                        val editId by it.savedStateHandle.getStateFlow<Long?>(
                            key = "editId",
                            initialValue = null
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
                        val viewModel = viewModel<BehaviourViewModel>(
                            factory = BehaviourViewModelFactory()
                        )

                        BehaviourSettingsPage(viewModel)
                    }

                    composable<Screens.Settings.MainPage.MemoryAndStorage> {
                        MemoryAndStorageSettingsPage()
                    }

                    composable<Screens.Settings.MainPage.Debugging> {
                        DebuggingSettingsPage()
                    }

                    composable<Screens.Settings.MainPage.PrivacyAndSecurity.ScreenLock>(
                        typeMap = mapOf(
                            typeOf<ByteArray?>() to NullableByteArrayNavType()
                        )
                    ) {
                        val screen = it.toRoute<Screens.Settings.MainPage.PrivacyAndSecurity.ScreenLock>()

                        ScreenLock(
                            action = screen.action,
                            password = screen.password,
                            salt = screen.salt
                        )
                    }

                    composable<Screens.Settings.MainPage.LookAndFeel.ColorAndStyle> {
                        val viewModel = viewModel<ThemeViewModel>(
                            factory = ThemeViewModelFactory()
                        )

                        ThemePage(viewModel = viewModel)
                    }
                }

                navigation<Screens.Settings.Misc>(
                    startDestination = Screens.Settings.Misc.DataAndBackup
                ) {
                    composable<Screens.Settings.Misc.DataAndBackup> {
                        val viewModel = viewModel<DataAndBackupViewModel>(
                            factory = DataAndBackupViewModelFactory(context)
                        )

                        DataAndBackupPage(viewModel)
                    }

                    composable<Screens.Settings.Misc.UpdatePage> {
                        val viewModel = hiltViewModel<UpdaterViewModel>()

                        val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                        val news by viewModel.news.collectAsStateWithLifecycle()
                        val showUpdateNotice by viewModel.showUpdateNotice.collectAsStateWithLifecycle()

                        UpdatesPage(
                            updateState = { updateState },
                            news = { news },
                            showUpdateNotice = { showUpdateNotice },
                            onRefresh = viewModel::refresh
                        )
                    }

                    composable<Screens.Settings.Misc.LicensesPage> {
                        LicensePage()
                    }

                    composable<Screens.Settings.Misc.ExtendedLicensePage> {
                        ExtendedLicensePage()
                    }

                    composable<Screens.Settings.Misc.AboutPage> {
                        val viewModel = viewModel<ContributorViewModel>(
                            factory = ContributorViewModelFactory(context)
                        )

                        val contributors by viewModel.contributors.collectAsStateWithLifecycle()

                        AboutPage(
                            contributors = contributors,
                            appVersion = viewModel.appVersion,
                            navController = navController
                        )
                    }
                }

                composable<Screens.AlbumGroup> {
                    val screen = it.toRoute<Screens.AlbumGroup>()

                    AlbumGroup(
                        id = screen.id
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
                        factory = EditorViewModelFactory(
                            context = context,
                            album = screen.album
                        )
                    )

                    val overwriteByDefault by viewModel.overwriteByDefault.collectAsStateWithLifecycle()
                    val exportQuality by viewModel.exportQuality.collectAsStateWithLifecycle()
                    val info by viewModel.immichInfo.collectAsStateWithLifecycle()

                    ImageEditor(
                        uri = screen.uri,
                        info = { info },
                        isFromOpenWithView = false,
                        exportQuality = { exportQuality },
                        overwriteByDefault = { overwriteByDefault },
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
                    setupNextScreen(window)

                    val screen = it.toRoute<Screens.VideoEditor>()

                    VideoEditor(
                        uri = screen.uri,
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

        PhotosApplication.appModule.scope.launch(Dispatchers.IO) {
            if (SyncManager(applicationContext).getGeneration() > 0L) {
                delay(2000.milliseconds) // so it isn't immediate on startup

                // run work manager immediately after user navigates back to app
                SyncWorker.start(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        lifecycleScope.launch(Dispatchers.IO) {
            val password = PhotosApplication.appModule.settings.permissions.getPassword().first()

            if (password != null) launch(Dispatchers.Main) {
                navController.navigate(Screens.Startup.ScreenLock)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PhotosApplication.appModule.logManager.stopRecording()
    }
}

fun setupNextScreen(window: Window) {
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}