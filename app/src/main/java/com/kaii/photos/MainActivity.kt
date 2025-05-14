package com.kaii.photos

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.lavender_snackbars.LavenderSnackbarBox
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.lavender_snackbars.LavenderSnackbarHostState
import com.kaii.photos.compose.ErrorPage
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.PermissionHandler
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.MainAppBottomBar
import com.kaii.photos.compose.app_bars.MainAppSelectingBottomBar
import com.kaii.photos.compose.app_bars.MainAppTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.MainAppDialog
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.LockedFolderView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.rememberDeviceOrientation
import com.kaii.photos.compose.settings.AboutPage
import com.kaii.photos.compose.settings.DataAndBackupPage
import com.kaii.photos.compose.settings.DebuggingSettingsPage
import com.kaii.photos.compose.settings.GeneralSettingsPage
import com.kaii.photos.compose.settings.LookAndFeelSettingsPage
import com.kaii.photos.compose.settings.MainSettingsPage
import com.kaii.photos.compose.settings.MemoryAndStorageSettingsPage
import com.kaii.photos.compose.settings.PrivacyAndSecurityPage
import com.kaii.photos.compose.settings.UpdatesPage
import com.kaii.photos.compose.single_photo.EditingView
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.Migration3to4
import com.kaii.photos.database.Migration4to5
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumInfoNavType
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.BottomBarTabSaver
import com.kaii.photos.helpers.CheckUpdateState
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

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
            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(
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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun SetContentForActivity() {
        window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

        val navControllerLocal = rememberNavController()

        val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)
        val currentView = rememberSaveable(
            inputs = arrayOf(defaultTab),
            stateSaver = BottomBarTabSaver
        ) { mutableStateOf(defaultTab) }

        val context = LocalContext.current
        val showDialog = remember { mutableStateOf(false) }

        val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }

        val logPath = "${context.appStorageDir}/log.txt"
        Log.d(TAG, "Log save path is $logPath")

        val canRecordLogs by mainViewModel.settings.Debugging.getRecordLogs()
            .collectAsStateWithLifecycle(initialValue = false)

        LaunchedEffect(canRecordLogs) {
            if (canRecordLogs) {
                val logManager = LogManager(context = context)
                logManager.startRecording()
            }
        }

//        mainViewModel.settings.AlbumsList.addToAlbumsList("DCIM/Camera")

        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val currentSortMode by mainViewModel.settings.PhotoGrid.getSortMode()
            .collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)

        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(albumsList),
                sortBy = currentSortMode
            )
        )

        val customAlbumViewModel: CustomAlbumViewModel = viewModel(
            factory = CustomAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                sortBy = currentSortMode
            )
        )

        // update main photos view albums list
        LaunchedEffect(albumsList) {
            if (navControllerLocal.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                || multiAlbumViewModel.albumInfo.paths.toSet() == albumsList
            ) return@LaunchedEffect

            Log.d(TAG, "Refreshing main photos view")
            Log.d(TAG, "In view model: ${multiAlbumViewModel.albumInfo.paths} new: $albumsList")
            multiAlbumViewModel.reinitDataSource(
                context = context,
                album = AlbumInfo.createPathOnlyAlbum(albumsList),
                sortMode = currentSortMode
            )
        }

        LaunchedEffect(currentSortMode) {
            if (multiAlbumViewModel.sortBy == currentSortMode) return@LaunchedEffect

            Log.d(
                TAG,
                "Changing sort mode from: ${multiAlbumViewModel.sortBy} to: $currentSortMode"
            )
            multiAlbumViewModel.changeSortMode(context = context, sortMode = currentSortMode)
            customAlbumViewModel.changeSortMode(context = context, sortMode = currentSortMode)
        }

        val snackbarHostState = remember {
            LavenderSnackbarHostState()
        }

        CompositionLocalProvider(LocalNavController provides navControllerLocal) {
            LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
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

                        Content(currentView, showDialog, selectedItemsList, multiAlbumViewModel)
                    }

                    composable<Screens.SinglePhotoView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfoNavType,
                            typeOf<List<String>>() to NavType.StringListType
                        )
                    ) {
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

                        if (!screen.hasSameAlbumsAs(other = multiAlbumViewModel.albumInfo.paths)) {
                            multiAlbumViewModel.reinitDataSource(
                                context = context,
                                album = screen.albumInfo,
                                sortMode = multiAlbumViewModel.sortBy
                            )
                        }

                        SinglePhotoView(
                            navController = navControllerLocal,
                            window = window,
                            viewModel = multiAlbumViewModel,
                            mediaItemId = screen.mediaItemId,
                            loadsFromMainViewModel = screen.loadsFromMainViewModel
                        )
                    }

                    composable<Screens.SingleAlbumView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfoNavType,
                            typeOf<List<String>>() to NavType.StringListType
                        )
                    ) {
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

                        if (!screen.albumInfo.isCustomAlbum) {
                            if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                                multiAlbumViewModel.reinitDataSource(
                                    context = context,
                                    album = screen.albumInfo,
                                    sortMode = multiAlbumViewModel.sortBy
                                )
                            }

                            SingleAlbumView(
                                albumInfo = screen.albumInfo,
                                selectedItemsList = selectedItemsList,
                                currentView = currentView,
                                viewModel = multiAlbumViewModel
                            )
                        } else {
                            if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                                customAlbumViewModel.reinitDataSource(
                                    context = context,
                                    album = screen.albumInfo,
                                    sortMode = customAlbumViewModel.sortBy
                                )
                            }

                            SingleAlbumView(
                                albumInfo = screen.albumInfo,
                                selectedItemsList = selectedItemsList,
                                currentView = currentView,
                                viewModel = customAlbumViewModel
                            )
                        }
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

                    composable(MultiScreenViewType.SecureFolder.name) {
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
                            window = window
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
                        val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault()
                            .collectAsStateWithLifecycle(initialValue = false)

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

                        GeneralSettingsPage(currentTab = currentView)
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

                    composable(MultiScreenViewType.DataAndBackup.name) {
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

                        DataAndBackupPage()
                    }

                    composable(MultiScreenViewType.PrivacyAndSecurity.name) {
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

                        PrivacyAndSecurityPage()
                    }
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
        val checkForUpdatesOnStartup by mainViewModel.settings.Versions.getCheckUpdatesOnStartup()
            .collectAsStateWithLifecycle(initialValue = false)

        // so it only checks once
        LaunchedEffect(checkForUpdatesOnStartup) {
            if (checkForUpdatesOnStartup) {
                mainViewModel.updater.refresh { state ->
                    Log.d(TAG, "Checking for app updates...")

                    when (state) {
                        CheckUpdateState.Succeeded -> {
                            if (mainViewModel.updater.hasUpdates.value) {
                                Log.d(TAG, "Update found! Notifying user...")

                                coroutineScope.launch {
                                    LavenderSnackbarController.pushEvent(
                                        LavenderSnackbarEvents.ActionEvent(
                                            message = "New app version available!",
                                            iconResId = R.drawable.error_2,
                                            duration = SnackbarDuration.Short,
                                            actionIconResId = R.drawable.download,
                                            action = {
                                                navControllerLocal.navigate(MultiScreenViewType.UpdatesPage.name)
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        else -> {
                            Log.d(TAG, "No update found.")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Content(
        currentView: MutableState<BottomBarTab>,
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        multiAlbumViewModel: MultiAlbumViewModel,
    ) {
        val context = LocalContext.current
        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val mediaStoreData =
            multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
        val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }

        val tabList by mainViewModel.settings.DefaultTabs.getTabList()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)

        // faster loading if no custom tabs are present
        LaunchedEffect(tabList) {
            if (!tabList.any { it.isCustom } && currentView.value.albumPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                multiAlbumViewModel.reinitDataSource(
                    context = context,
                    album = AlbumInfo(
                        id = currentView.value.id,
                        name = currentView.value.name,
                        paths = currentView.value.albumPaths,
                        isCustomAlbum = currentView.value.isCustom
                    ),
                    sortMode = multiAlbumViewModel.sortBy
                )

                groupedMedia.value = mediaStoreData.value
            }
        }

        Scaffold(
            topBar = {
                TopBar(
                    showDialog = showDialog,
                    selectedItemsList = selectedItemsList,
                    currentView = currentView
                )
            },
            bottomBar = {
                BottomBar(
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabList
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize(1f)
        ) { padding ->
            val isLandscape by rememberDeviceOrientation()

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
                        0.dp // Remove bottom padding to allow content to be visible behind the bottom bar
                    )
                    .fillMaxSize()
            ) {
                MainAppDialog(showDialog, currentView, selectedItemsList)

                AnimatedContent(
                    targetState = currentView.value,
                    transitionSpec = {
                        if (targetState.index > initialState.index) {
                            (slideInHorizontally { width -> width } + fadeIn(initialAlpha = 0f)).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut(targetAlpha = 0f))
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(initialAlpha = 0f)).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut(targetAlpha = 0f))
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "MainAnimatedContentView",
                    modifier = Modifier.background(Color.Transparent)
                ) { stateValue ->
                    if (stateValue in tabList || stateValue == DefaultTabs.TabTypes.secure) {
                        when {
                            stateValue.isCustom -> {
                                if (stateValue.albumPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                                    multiAlbumViewModel.reinitDataSource(
                                        context = context,
                                        album = AlbumInfo(
                                            id = stateValue.id,
                                            name = stateValue.name,
                                            paths = stateValue.albumPaths,
                                            isCustomAlbum = true
                                        ),
                                        sortMode = multiAlbumViewModel.sortBy
                                    )
                                }

                                LaunchedEffect(mediaStoreData.value) {
                                    groupedMedia.value = mediaStoreData.value
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = AlbumInfo(
                                        id = stateValue.id,
                                        name = stateValue.name,
                                        paths = stateValue.albumPaths,
                                        isCustomAlbum = true
                                    ),
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.photos -> {
                                if (albumsList.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                                    multiAlbumViewModel.reinitDataSource(
                                        context = context,
                                        album = AlbumInfo(
                                            id = stateValue.id,
                                            name = stateValue.name,
                                            paths = albumsList,
                                            isCustomAlbum = false
                                        ),
                                        sortMode = multiAlbumViewModel.sortBy
                                    )
                                }

                                selectedItemsList.clear()

                                LaunchedEffect(mediaStoreData.value) {
                                    groupedMedia.value = mediaStoreData.value
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = multiAlbumViewModel.albumInfo,
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.secure -> LockedFolderEntryView(
                                currentView
                            )

                            stateValue == DefaultTabs.TabTypes.albums -> {
                                AlbumsGridView(currentView)
                            }

                            stateValue == DefaultTabs.TabTypes.search -> {
                                selectedItemsList.clear()

                                SearchPage(selectedItemsList, currentView)
                            }
                        }
                    } else {
                        ErrorPage(
                            message = "This tab doesn't exist!",
                            iconResId = R.drawable.error
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        currentView: MutableState<BottomBarTab>
    ) {
        val show by remember {
            derivedStateOf {
                selectedItemsList.isNotEmpty()
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
        currentView: MutableState<BottomBarTab>,
        tabs: List<BottomBarTab>,
        selectedItemsList: SnapshotStateList<MediaStoreData>
    ) {
        val navController = LocalNavController.current
        val show by remember {
            derivedStateOf {
                selectedItemsList.isNotEmpty()
            }
        }

        AnimatedContent(
            targetState = show && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
            transitionSpec = {
                getAppBarContentTransition(show)
            },
            label = "MainBottomBarAnimatedContentView",
            modifier = Modifier.background(Color.Transparent)
        ) { state ->
            if (!state) {
                MainAppBottomBar(
                    currentView = currentView,
                    tabs = tabs,
                    selectedItemsList = selectedItemsList
                )
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

    // window.setDecorFitsSystemWindows(false)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}
