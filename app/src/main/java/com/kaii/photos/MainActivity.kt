package com.kaii.photos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
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
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.lavender.snackbars.LavenderSnackbarBox
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.lavender.snackbars.LavenderSnackbarHostState
import com.kaii.photos.compose.ErrorPage
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.PermissionHandler
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.MainAppBottomBar
import com.kaii.photos.compose.app_bars.MainAppTopBar
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.MainAppDialog
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.LockedFolderView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.immich.ImmichAlbumPage
import com.kaii.photos.compose.immich.ImmichMainPage
import com.kaii.photos.compose.settings.AboutPage
import com.kaii.photos.compose.settings.DataAndBackupPage
import com.kaii.photos.compose.settings.DebuggingSettingsPage
import com.kaii.photos.compose.settings.ExtendedLicensePage
import com.kaii.photos.compose.settings.GeneralSettingsPage
import com.kaii.photos.compose.settings.LicensePage
import com.kaii.photos.compose.settings.LookAndFeelSettingsPage
import com.kaii.photos.compose.settings.MainSettingsPage
import com.kaii.photos.compose.settings.MemoryAndStorageSettingsPage
import com.kaii.photos.compose.settings.PrivacyAndSecurityPage
import com.kaii.photos.compose.settings.UpdatesPage
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumInfoNavType
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.datastore.User
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.BottomBarTabSaver
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.helpers.startupUpdateCheck
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.immich.ImmichViewModel
import com.kaii.photos.models.immich.ImmichViewModelFactory
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.DisplayDateFormat
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.multi_album.groupPhotosBy
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf
import kotlin.uuid.ExperimentalUuidApi

private const val TAG = "MAIN_ACTIVITY"

val LocalNavController = compositionLocalOf<NavHostController> {
    throw IllegalStateException("CompositionLocal LocalNavController not present")
}

val LocalMainViewModel = compositionLocalOf<MainViewModel> {
    throw IllegalStateException("CompositionLocal LocalMainViewModel not present")
}

val LocalAppDatabase = compositionLocalOf<MediaDatabase> {
    throw IllegalStateException("CompositionLocal LocalAppDatabase not present")
}

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var immichViewModel: ImmichViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val applicationDatabase = MediaDatabase.getInstance(applicationContext)

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext, emptyList())
            )
            immichViewModel = viewModel(
                factory = ImmichViewModelFactory(
                    application = application,
                    immichSettings = mainViewModel.settings.Immich,
                    albumsSettings = mainViewModel.settings.AlbumsList
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mainViewModel.settings.Permissions.setIsMediaManager(MediaStore.canManageMedia(applicationContext))
            }

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
                theme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val navControllerLocal = rememberNavController()
                CompositionLocalProvider(
                    LocalNavController provides navControllerLocal,
                    LocalMainViewModel provides mainViewModel,
                    LocalAppDatabase provides applicationDatabase
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
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun SetContentForActivity() {
        window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

        val mainViewModel = LocalMainViewModel.current

        val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)
        val currentView = rememberSaveable(
            inputs = arrayOf(defaultTab),
            stateSaver = BottomBarTabSaver
        ) { mutableStateOf(defaultTab) }

        val showDialog = remember { mutableStateOf(false) }

        val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }

        val context = LocalContext.current
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

        val displayDateFormat by mainViewModel.settings.LookAndFeel.getDisplayDateFormat()
            .collectAsStateWithLifecycle(initialValue = null)

        LaunchedEffect(displayDateFormat) {
            if (displayDateFormat != null) mainViewModel.setDisplayDateFormat(displayDateFormat!!)
        }

        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())

        val shouldShowEverything by mainViewModel.showAllInMain.collectAsStateWithLifecycle()
        val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect()
            .collectAsStateWithLifecycle(initialValue = true)
        val applicationDatabase = LocalAppDatabase.current
        val allAlbums by if (autoDetectAlbums) {
            mainViewModel.settings.AlbumsList.getAutoDetectedAlbums(displayDateFormat ?: DisplayDateFormat.Default, applicationDatabase)
                .collectAsStateWithLifecycle(initialValue = emptyList())
        } else {
            mainViewModel.settings.AlbumsList.getNormalAlbums()
                .collectAsStateWithLifecycle(initialValue = emptyList())
        }

        val currentSortMode by mainViewModel.settings.PhotoGrid.getSortMode()
            .collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)

        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(
                    if (shouldShowEverything) {
                        allAlbums.flatMap { album -> album.paths.fastMap { it.removeSuffix("/") } } - albumsList
                    } else {
                        albumsList
                    }
                ),
                sortBy = currentSortMode,
                displayDateFormat = displayDateFormat ?: DisplayDateFormat.Default,
                database = applicationDatabase
            )
        )

        val customAlbumViewModel: CustomAlbumViewModel = viewModel(
            factory = CustomAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                sortBy = currentSortMode,
                displayDateFormat = displayDateFormat ?: DisplayDateFormat.Default
            )
        )

        val navController = LocalNavController.current
        // update main photos view albums list
        LaunchedEffect(albumsList, allAlbums) {
            val albums =
                if (shouldShowEverything) {
                    allAlbums.flatMap { album -> album.paths.fastMap { it.removeSuffix("/") } } - albumsList
                } else {
                    albumsList
                }

            Log.d(TAG, "query ALBUMS $albums")

            if (navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                || multiAlbumViewModel.albumInfo.paths.toSet() == albums.toSet()
            ) return@LaunchedEffect

            Log.d(TAG, "Refreshing main photos view")
            Log.d(TAG, "In view model: ${multiAlbumViewModel.albumInfo.paths} new: $albums")
            multiAlbumViewModel.reinitDataSource(
                context = context,
                album = AlbumInfo.createPathOnlyAlbum(albums),
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

        LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = MultiScreenViewType.MainScreen.name,
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
                composable(MultiScreenViewType.MainScreen.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
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

                    if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                        multiAlbumViewModel.reinitDataSource(
                            context = context,
                            album = screen.albumInfo,
                            sortMode = multiAlbumViewModel.sortBy
                        )
                    }

                    if (!screen.albumInfo.isCustomAlbum) {
                        SinglePhotoView(
                            navController = navController,
                            window = window,
                            multiAlbumViewModel = multiAlbumViewModel,
                            mediaItemId = screen.mediaItemId,
                            loadsFromMainViewModel = screen.loadsFromMainViewModel,
                        )
                    } else {
                        if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                            customAlbumViewModel.reinitDataSource(
                                context = context,
                                album = screen.albumInfo,
                                sortMode = customAlbumViewModel.sortBy
                            )
                        }

                        SinglePhotoView(
                            navController = navController,
                            window = window,
                            multiAlbumViewModel = multiAlbumViewModel,
                            customAlbumViewModel = customAlbumViewModel,
                            mediaItemId = screen.mediaItemId,
                            loadsFromMainViewModel = screen.loadsFromMainViewModel,
                        )
                    }
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

                    if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                        multiAlbumViewModel.reinitDataSource(
                            context = context,
                            album = screen.albumInfo,
                            sortMode = multiAlbumViewModel.sortBy
                        )
                    }

                    if (!screen.albumInfo.isCustomAlbum) {
                        SingleAlbumView(
                            albumInfo = screen.albumInfo,
                            selectedItemsList = selectedItemsList,
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
                            customViewModel = customAlbumViewModel,
                            multiViewModel = multiAlbumViewModel
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
                        selectedItemsList = selectedItemsList
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

                    LockedFolderView(window = window)
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
                        navController.popBackStack()
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
                        selectedItemsList = selectedItemsList
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
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.ImageEditor = it.toRoute()

                    ImageEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        isFromOpenWithView = false
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

                composable(MultiScreenViewType.ImmichMainPage.name) {
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

                    ImmichMainPage()
                }

                composable<Screens.ImmichAlbumPage>(
                    typeMap = mapOf(
                        typeOf<AlbumInfo>() to AlbumInfoNavType
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

                    val screen: Screens.ImmichAlbumPage = it.toRoute()
                    if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                        multiAlbumViewModel.reinitDataSource(
                            context = context,
                            album = screen.albumInfo,
                            sortMode = multiAlbumViewModel.sortBy
                        )
                    }

                    ImmichAlbumPage(
                        albumInfo = screen.albumInfo,
                        multiAlbumViewModel = multiAlbumViewModel
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
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen = it.toRoute<Screens.VideoEditor>()

                    VideoEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        window = window,
                        isFromOpenWithView = false
                    )
                }

                composable(MultiScreenViewType.LicensePage.name) {
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

                    LicensePage()
                }

                composable(MultiScreenViewType.ExtendedLicensePage.name) {
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

                    ExtendedLicensePage()
                }
            }
        }

        val coroutineScope = rememberCoroutineScope()
        val checkForUpdatesOnStartup by mainViewModel.settings.Versions.getCheckUpdatesOnStartup()
            .collectAsStateWithLifecycle(initialValue = false)

        val firstStartup by mainViewModel.settings.User.getFirstStartup()
            .collectAsStateWithLifecycle(initialValue = true)

        if (firstStartup) {
            val showFirstStartupDialog = remember { mutableStateOf(false) }
            val isLoading = remember { mutableStateOf(false) }
            val findingAlbumsOnDevice = stringResource(id = R.string.finding_albums_on_device)
            val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

            ConfirmationDialogWithBody(
                dialogTitle = stringResource(id = R.string.first_startup_dialog_title),
                dialogBody = stringResource(id = R.string.first_startup_dialog_body),
                showDialog = showFirstStartupDialog,
                confirmButtonLabel = stringResource(id = R.string.first_startup_dialog_confirm_title)
            ) {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        @OptIn(ExperimentalUuidApi::class)
                        LavenderSnackbarEvents.LoadingEvent(
                            message = findingAlbumsOnDevice,
                            icon = R.drawable.art_track,
                            isLoading = isLoading
                        )
                    )

                    mainViewModel.settings.AlbumsList.getAllAlbumsOnDevice(displayDateFormat = displayDateFormat, appDatabase = applicationDatabase)
                        .cancellable()
                        .collectLatest { list ->
                            mainViewModel.settings.AlbumsList.setAlbumsList(list)
                            mainViewModel.settings.AlbumsList.setAutoDetect(true)

                            Log.d(TAG, "Albums on device are $list")
                            isLoading.value = false

                            mainViewModel.settings.User.setFirstStartup(false)
                        }
                }
            }
        }

        // so it only checks once
        val resources = LocalResources.current
        LaunchedEffect(checkForUpdatesOnStartup) {
            if (checkForUpdatesOnStartup) {
                startupUpdateCheck(
                    text = resources.getString(R.string.updates_new_version_available),
                    coroutineScope = coroutineScope,
                    navController = navController,
                    mainViewModel = mainViewModel
                )
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
        val mainViewModel = LocalMainViewModel.current

        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())

        val shouldShowEverything by mainViewModel.showAllInMain.collectAsStateWithLifecycle()
        val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect()
            .collectAsStateWithLifecycle(initialValue = true)

        val applicationDatabase = LocalAppDatabase.current
        val displayDateFormat by mainViewModel.settings.LookAndFeel.getDisplayDateFormat().collectAsStateWithLifecycle(initialValue = DisplayDateFormat.Default)

        val allAlbums by if (autoDetectAlbums) {
            mainViewModel.settings.AlbumsList.getAutoDetectedAlbums(displayDateFormat, applicationDatabase)
                .collectAsStateWithLifecycle(initialValue = emptyList())
        } else {
            mainViewModel.settings.AlbumsList.getNormalAlbums()
                .collectAsStateWithLifecycle(initialValue = emptyList())
        }

        val mediaStoreData =
            multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
        val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }

        val tabList by mainViewModel.settings.DefaultTabs.getTabList()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)

        val navController = LocalNavController.current

        // faster loading if no custom tabs are present
        LaunchedEffect(tabList, shouldShowEverything, navController.currentBackStackEntry?.destination?.route) {
            val showEverything =
                shouldShowEverything
                        && currentView.value == DefaultTabs.TabTypes.photos
                        && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name

            if (!tabList.any { it.isCustom }
                && (currentView.value.albumPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()
                        || multiAlbumViewModel.ignorePaths != showEverything)
            ) {
                multiAlbumViewModel.reinitDataSource(
                    context = context,
                    album = AlbumInfo(
                        id = currentView.value.id,
                        name = currentView.value.name,
                        paths = currentView.value.albumPaths,
                        isCustomAlbum = currentView.value.isCustom
                    ),
                    sortMode = multiAlbumViewModel.sortBy,
                    ignorePaths = showEverything
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
                MainAppBottomBar(
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabList
                )
            },
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
                        start = safeDrawingPadding.first,
                        top = padding.calculateTopPadding(),
                        end = safeDrawingPadding.second,
                        bottom = 0.dp
                    )
            ) {
                MainAppDialog(
                    showDialog = showDialog,
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    mainViewModel = mainViewModel
                )

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
                    if (stateValue in tabList || stateValue == DefaultTabs.TabTypes.secure) {
                        Log.d(TAG, "Tab needed is $stateValue")
                        when {
                            stateValue.isCustom -> {
                                if (stateValue.albumPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                                    groupedMedia.value = emptyList()
                                    multiAlbumViewModel.reinitDataSource(
                                        context = context,
                                        album = AlbumInfo(
                                            id = stateValue.id,
                                            name = stateValue.name,
                                            paths = stateValue.albumPaths,
                                            isCustomAlbum = false
                                        ),
                                        sortMode = multiAlbumViewModel.sortBy
                                    )
                                }

                                LaunchedEffect(Unit) {
                                    selectedItemsList.clear()
                                }

                                var hasFiles by remember { mutableStateOf(true) }
                                LaunchedEffect(mediaStoreData.value.size) {
                                    if (mediaStoreData.value.isNotEmpty()) {
                                        delay(PhotoGridConstants.UPDATE_TIME)
                                        groupedMedia.value = mediaStoreData.value
                                    } else {
                                        delay(PhotoGridConstants.LOADING_TIME)
                                        groupedMedia.value = emptyList()
                                        hasFiles = false
                                    }

                                    delay(PhotoGridConstants.LOADING_TIME)
                                    hasFiles = groupedMedia.value.isNotEmpty()
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = AlbumInfo(
                                        id = stateValue.id,
                                        name = stateValue.name,
                                        paths = stateValue.albumPaths,
                                        isCustomAlbum = false
                                    ),
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                    hasFiles = hasFiles,
                                    isMainPage = true
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.photos -> {
                                val albums =
                                    if (shouldShowEverything) {
                                        allAlbums.flatMap { album -> album.paths.fastMap { it.removeSuffix("/") } } - albumsList
                                    } else {
                                        albumsList
                                    }

                                if (albums.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()
                                    || multiAlbumViewModel.ignorePaths != shouldShowEverything
                                ) {
                                    multiAlbumViewModel.reinitDataSource(
                                        context = context,
                                        album = AlbumInfo(
                                            id = stateValue.id,
                                            name = stateValue.name,
                                            paths = albums,
                                            isCustomAlbum = false
                                        ),
                                        sortMode = multiAlbumViewModel.sortBy,
                                        ignorePaths = shouldShowEverything
                                    )
                                }

                                LaunchedEffect(Unit) {
                                    selectedItemsList.clear()
                                }

                                var hasFiles by remember { mutableStateOf(true) }

                                LaunchedEffect(mediaStoreData.value.size) {
                                    if (mediaStoreData.value.isNotEmpty()) {
                                        delay(PhotoGridConstants.UPDATE_TIME)
                                        groupedMedia.value = mediaStoreData.value
                                    } else {
                                        delay(PhotoGridConstants.LOADING_TIME)
                                        groupedMedia.value = emptyList()
                                        hasFiles = false
                                    }

                                    delay(PhotoGridConstants.LOADING_TIME)
                                    hasFiles = groupedMedia.value.isNotEmpty()
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = multiAlbumViewModel.albumInfo,
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                    hasFiles = hasFiles,
                                    isMainPage = true
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.secure -> LockedFolderEntryView(
                                currentView
                            )

                            stateValue == DefaultTabs.TabTypes.albums -> {
                                AlbumsGridView(currentView)
                            }

                            stateValue == DefaultTabs.TabTypes.search -> {
                                LaunchedEffect(Unit) {
                                    selectedItemsList.clear()
                                }

                                SearchPage(selectedItemsList, currentView)
                            }

                            stateValue == DefaultTabs.TabTypes.favourites -> {
                                val appDatabase = LocalAppDatabase.current
                                val favouritesViewModel: FavouritesViewModel = viewModel(
                                    factory = FavouritesViewModelFactory(appDatabase)
                                )

                                val mediaStoreData by favouritesViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

                                val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                                val groupedMedia = remember {
                                    mutableStateOf(
                                        groupPhotosBy(
                                            mediaStoreData,
                                            MediaItemSortMode.LastModified,
                                            displayDateFormat,
                                            context
                                        )
                                    )
                                }

                                var hasFiles by remember { mutableStateOf(true) }

                                LaunchedEffect(mediaStoreData) {
                                    if (mediaStoreData.isNotEmpty()) {
                                        delay(PhotoGridConstants.UPDATE_TIME)
                                        groupedMedia.value =
                                            groupPhotosBy(
                                                mediaStoreData,
                                                MediaItemSortMode.LastModified,
                                                displayDateFormat,
                                                context
                                            )
                                    } else {
                                        delay(PhotoGridConstants.LOADING_TIME)
                                        groupedMedia.value = emptyList()
                                        hasFiles = false
                                    }

                                    delay(PhotoGridConstants.LOADING_TIME)
                                    hasFiles = mediaStoreData.isNotEmpty()

                                    Log.d(TAG, "Grouped media size: ${groupedMedia.value.size}")
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                                    selectedItemsList = selectedItemsList,
                                    viewProperties = ViewProperties.Favourites,
                                    hasFiles = hasFiles
                                )
                            }

                            stateValue == DefaultTabs.TabTypes.trash -> {
                                val appDatabase = LocalAppDatabase.current
                                val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

                                val trashViewModel: TrashViewModel = viewModel(
                                    factory = TrashViewModelFactory(context = context, displayDateFormat = displayDateFormat, appDatabase = appDatabase)
                                )

                                val mediaStoreData =
                                    trashViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

                                val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
                                var hasFiles by remember { mutableStateOf(true) }

                                LaunchedEffect(mediaStoreData.value) {
                                    groupedMedia.value = mediaStoreData.value

                                    delay(PhotoGridConstants.LOADING_TIME)
                                    hasFiles = groupedMedia.value.isNotEmpty()
                                }

                                PhotoGrid(
                                    groupedMedia = groupedMedia,
                                    albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                                    selectedItemsList = selectedItemsList,
                                    viewProperties = ViewProperties.Trash,
                                    hasFiles = hasFiles
                                )
                            }
                        }
                    } else {
                        ErrorPage(
                            message = stringResource(id = R.string.tab_non_existent),
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
}

fun setupNextScreen(
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
