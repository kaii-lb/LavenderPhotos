package com.kaii.photos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.lavender.immichintegration.state_managers.LocalApiClient
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
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SecureFolderView
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.immich.ImmichAlbumPage
import com.kaii.photos.compose.immich.ImmichMainPage
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.settings.AboutPage
import com.kaii.photos.compose.settings.BehaviourSettingsPage
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
import com.kaii.photos.compose.single_photo.SecurePhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Behaviour
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.datastore.Storage
import com.kaii.photos.datastore.User
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.LogManager
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appStorageDir
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.startupUpdateCheck
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.tryGetAllAlbums
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.immich_album.ImmichAlbumViewModel
import com.kaii.photos.models.immich_album.ImmichAlbumViewModelFactory
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.models.secure_folder.SecureFolderViewModel
import com.kaii.photos.models.secure_folder.SecureFolderViewModelFactory
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.reflect.typeOf

private const val TAG = "com.kaii.photos.MainActivity"

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
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val applicationDatabase = MediaDatabase.getInstance(applicationContext)

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val mainViewModel = ViewModelProvider.create(
            store = viewModelStore,
            factory = MainViewModelFactory(applicationContext, emptyList())
        )[MainViewModel::class]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mainViewModel.settings.Permissions.setIsMediaManager(MediaStore.canManageMedia(applicationContext))
        }

        // Manifest.permission.MANAGE_MEDIA is optional
        mainViewModel.startupPermissionCheck(applicationContext)

        val initialFollowDarkTheme = runBlocking {
            mainViewModel.settings.LookAndFeel.getFollowDarkMode().first()
        }

        WorkManager.getInstance(applicationContext)
            .enqueue(OneTimeWorkRequest.Builder(SyncWorker::class).build())

        setContent {
            val continueToApp = remember {
                mutableStateOf(
                    mainViewModel.checkCanPass()
                )
            }

            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(
                    initialValue = initialFollowDarkTheme
                )

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
                val apiClient = remember {
                    ApiClient()
                }

                CompositionLocalProvider(
                    LocalNavController provides navControllerLocal,
                    LocalMainViewModel provides mainViewModel,
                    LocalAppDatabase provides applicationDatabase,
                    LocalApiClient provides apiClient
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
                            PermissionHandler(continueToApp)
                        } else {
                            SetContentForActivity()
                        }
                    }
                }
            }

            val hasClearedCache by mainViewModel.settings.Versions.getHasClearedGlideCache().collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(hasClearedCache) {
                if (!hasClearedCache) {
                    mainViewModel.settings.Storage.clearThumbnailCache()
                    mainViewModel.settings.Versions.setHasClearedGlideCache(true)
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
            stateSaver = BottomBarTab.TabSaver
        ) { mutableStateOf(defaultTab) }

        val showDialog = remember { mutableStateOf(false) }

        val selectedItemsList = remember { SnapshotStateList<PhotoLibraryUIModel>() }

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

        val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
        val currentSortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

        val mainPhotosPaths by mainViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()
        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(mainPhotosPaths),
                sortMode = currentSortMode,
                displayDateFormat = displayDateFormat
            )
        )

        // TODO: split into nav composables with createGraph and use a bottom nav bar
        // TODO: actually make separators function
        val searchViewModel: SearchViewModel = viewModel(
            factory = SearchViewModelFactory(
                context = context,
                sortMode = currentSortMode,
                format = displayDateFormat,
                separators = true
            )
        )

        // TODO: remove with search moving to its own nav page
        LaunchedEffect(currentView.value) {
            if (currentView.value != DefaultTabs.TabTypes.search) {
                searchViewModel.clear()
            }
        }

        val navController = LocalNavController.current

        // TODO: move to settings, only update when it actually changes
        // update main photos view albums list
        // LaunchedEffect(mainPhotosPaths) {
        //     Log.d(TAG, "query ALBUMS $mainPhotosPaths")
        //
        //     Log.d(TAG, "Refreshing main photos view")
        //     Log.d(TAG, "In view model: ${multiAlbumViewModel.albumInfo.paths} new: $mainPhotosPaths")
        //     multiAlbumViewModel.reinitDataSource(
        //         context = context,
        //         album = AlbumInfo.createPathOnlyAlbum(mainPhotosPaths),
        //         sortMode = currentSortMode
        //     )
        // }
        //
        LaunchedEffect(currentSortMode) {
            multiAlbumViewModel.update(
                sortMode = currentSortMode
            )
            // customAlbumViewModel.changeSortMode(context = context, sortMode = currentSortMode)

            // searchViewModel.restart(
            //     context = context,
            //     sortMode = currentSortMode
            // )
            // searchViewModel.setMedia(
            //     context = context,
            //     media = searchViewModel.groupedMedia.value.mapNotNull { (it as? PhotoLibraryUIModel.Media)?.item },
            //     sortMode = currentSortMode,
            //     displayDateFormat = displayDateFormat
            // )
        }
        //
        // LaunchedEffect(displayDateFormat) {
        //     Log.d(
        //         TAG,
        //         "Changing display date format from: ${multiAlbumViewModel.format} to: $displayDateFormat"
        //     )
        //     multiAlbumViewModel.changeDisplayDateFormat(context = context, format = displayDateFormat)
        //     customAlbumViewModel.changeDisplayDateFormat(context = context, displayDateFormat = displayDateFormat)
        //
        //     searchViewModel.restart(
        //         context = context,
        //         displayDateFormat = displayDateFormat
        //     )
        //     searchViewModel.setMedia(
        //         context = context,
        //         media = searchViewModel.groupedMedia.value.mapNotNull { (it as? PhotoLibraryUIModel.Media)?.item },
        //         sortMode = currentSortMode,
        //         displayDateFormat = displayDateFormat
        //     )
        // }

        val snackbarHostState = remember {
            LavenderSnackbarHostState()
        }

        val immichInfo by mainViewModel.settings.Immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

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
                    setupNextScreen(
                        selectedItemsList = selectedItemsList,
                        window = window
                    )

                    Content(
                        currentView = currentView,
                        showDialog = showDialog,
                        selectedItemsList = selectedItemsList,
                        multiAlbumViewModel = multiAlbumViewModel,
                        searchViewModel = searchViewModel
                    )
                }

                composable<Screens.SinglePhotoView>(
                    typeMap = mapOf(
                        typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType,
                        typeOf<List<String>>() to NavType.StringListType
                    )
                ) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.SinglePhotoView = it.toRoute()

                    // TODO: move search single photo view into nav graphs
                    SinglePhotoView(
                        navController = navController,
                        viewModel = searchViewModel,
                        window = window,
                        mediaItemId = screen.mediaItemId,
                        nextMediaItemId = screen.nextMediaItemId,
                        albumInfo = screen.albumInfo
                    )
                }

                navigation<Screens.Album>(
                    startDestination = Screens.Album.GridView::class
                ) {
                    composable<Screens.Album.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.Album.GridView = it.toRoute()
                        multiAlbumViewModel.update(album = screen.albumInfo) // TODO: move to nav-local multialbumviewmodel

                        SingleAlbumView(
                            albumInfo = screen.albumInfo,
                            selectedItemsList = selectedItemsList,
                            viewModel = multiAlbumViewModel
                        )
                    }

                    composable<Screens.Album.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen: Screens.Album.SinglePhoto = it.toRoute()

                        SinglePhotoView(
                            window = window,
                            viewModel = multiAlbumViewModel,
                            index = screen.index,
                            albumInfo = screen.albumInfo,
                            nextMediaItemId = screen.nextMediaItemId
                        )
                    }
                }

                navigation<Screens.Favourites>(
                    startDestination = Screens.Favourites.GridView
                ) {
                    composable<Screens.Favourites.GridView> {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Favourites)
                        }
                        val viewModel = viewModel<FavouritesViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = FavouritesViewModelFactory(
                                context = context,
                                sortMode = currentSortMode,
                                displayDateFormat = displayDateFormat
                            )
                        )

                        FavouritesGridView(
                            selectedItemsList = selectedItemsList,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.Favourites.SinglePhoto> {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Favourites)
                        }
                        val viewModel = viewModel<FavouritesViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = FavouritesViewModelFactory(
                                context = context,
                                sortMode = currentSortMode,
                                displayDateFormat = displayDateFormat
                            )
                        )

                        val screen = it.toRoute<Screens.Favourites.SinglePhoto>()
                        SinglePhotoView(
                            navController = navController,
                            viewModel = viewModel,
                            window = window,
                            mediaItemId = screen.mediaItemId,
                            nextMediaItemId = screen.nextMediaItemId
                        )
                    }

                    composable<Screens.Favourites.MigrationPage> {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        FavouritesMigrationPage()
                    }
                }

                navigation<Screens.Trash>(
                    startDestination = Screens.Trash.GridView
                ) {
                    composable<Screens.Trash.GridView> {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Trash)
                        }
                        val viewModel = viewModel<TrashViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = TrashViewModelFactory(
                                context = context,
                                sortMode = currentSortMode,
                                format = displayDateFormat
                            )
                        )

                        TrashedPhotoGridView(
                            selectedItemsList = selectedItemsList,
                            viewModel = viewModel
                        )
                    }

                    composable<Screens.Trash.SinglePhoto> {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen = it.toRoute<Screens.Trash.SinglePhoto>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Trash)
                        }
                        val viewModel = viewModel<TrashViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = TrashViewModelFactory(
                                context = context,
                                sortMode = currentSortMode,
                                format = displayDateFormat
                            )
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
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.SecureFolder)
                        }

                        val viewModel = viewModel<SecureFolderViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = SecureFolderViewModelFactory(
                                context = context,
                                sortMode = currentSortMode,
                                format = displayDateFormat
                            )
                        )

                        SecureFolderView(window = window, viewModel = viewModel)
                    }

                    composable<Screens.SecureFolder.SinglePhoto> {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.SecureFolder)
                        }
                        val viewModel = viewModel<SecureFolderViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = SecureFolderViewModelFactory(
                                context = context,
                                sortMode = currentSortMode,
                                format = displayDateFormat
                            )
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
                    startDestination = Screens.Immich.GridView::class
                ) {
                    composable<Screens.Immich.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen = it.toRoute<Screens.Immich.GridView>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.Immich)
                        }
                        val viewModel = viewModel<ImmichAlbumViewModel>(
                            viewModelStoreOwner = storeOwner,
                            factory = ImmichAlbumViewModelFactory(
                                immichId = screen.albumInfo.immichId,
                                info = immichInfo,
                                sortMode = currentSortMode,
                                format = displayDateFormat,
                                apiClient = LocalApiClient.current
                            )
                        )

                        ImmichAlbumPage(
                            albumInfo = screen.albumInfo,
                            selectedItemsList = selectedItemsList,
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
                                immichId = screen.albumInfo.immichId,
                                info = immichInfo,
                                sortMode = currentSortMode,
                                format = displayDateFormat,
                                apiClient = LocalApiClient.current
                            )
                        )

                        SinglePhotoView(
                            navController = navController,
                            viewModel = viewModel,
                            window = window,
                            index = screen.index,
                            nextMediaItemId = screen.nextMediaItemId,
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
                        val screen = it.toRoute<Screens.CustomAlbum.GridView>()

                        val storeOwner = remember(it) {
                            navController.getBackStackEntry(Screens.CustomAlbum)
                        }
                        val viewModel: CustomAlbumViewModel = viewModel(
                            viewModelStoreOwner = storeOwner,
                            factory = CustomAlbumViewModelFactory(
                                context = context,
                                albumInfo = screen.albumInfo,
                                sortBy = currentSortMode,
                                displayDateFormat = displayDateFormat
                            )
                        )

                        SingleAlbumView(
                            albumInfo = screen.albumInfo,
                            selectedItemsList = selectedItemsList,
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
                                albumInfo = screen.albumInfo,
                                sortBy = currentSortMode,
                                displayDateFormat = displayDateFormat
                            )
                        )

                        SinglePhotoView(
                            navController = navController,
                            window = window,
                            viewModel = viewModel,
                            mediaItemId = screen.mediaItemId,
                            nextMediaItemId = screen.nextMediaItemId,
                            albumInfo = screen.albumInfo
                        )
                    }
                }

                composable(MultiScreenViewType.AboutAndUpdateView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    AboutPage {
                        navController.popBackStack()
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
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen: Screens.ImageEditor = it.toRoute()

                    ImageEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        isFromOpenWithView = false,
                        albumInfo = screen.albumInfo,
                        screenType = screen.type
                    )
                }

                composable(MultiScreenViewType.SettingsMainView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    MainSettingsPage()
                }

                composable(MultiScreenViewType.SettingsDebuggingView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    DebuggingSettingsPage()
                }

                composable(MultiScreenViewType.SettingsGeneralView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    GeneralSettingsPage(currentTab = currentView)
                }

                composable(MultiScreenViewType.SettingsMemoryAndStorageView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    MemoryAndStorageSettingsPage()
                }

                composable(MultiScreenViewType.SettingsLookAndFeelView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    LookAndFeelSettingsPage()
                }

                composable(MultiScreenViewType.SettingsBehaviourView.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    BehaviourSettingsPage()
                }

                composable(MultiScreenViewType.UpdatesPage.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    UpdatesPage()
                }

                composable(MultiScreenViewType.DataAndBackup.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    DataAndBackupPage()
                }

                composable(MultiScreenViewType.PrivacyAndSecurity.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    PrivacyAndSecurityPage()
                }

                composable(MultiScreenViewType.ImmichMainPage.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    ImmichMainPage()
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
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    val screen = it.toRoute<Screens.VideoEditor>()

                    VideoEditor(
                        uri = screen.uri.toUri(),
                        absolutePath = screen.absolutePath,
                        albumInfo = screen.albumInfo,
                        window = window,
                        isFromOpenWithView = false,
                        screenType = screen.type
                    )
                }

                composable(MultiScreenViewType.LicensePage.name) {
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    LicensePage()
                }

                composable(MultiScreenViewType.ExtendedLicensePage.name) {
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

        // TODO: fix not working
        val firstStartup by mainViewModel.settings.User.getFirstStartup()
            .collectAsStateWithLifecycle(initialValue = false)

        if (firstStartup) {
            val showFirstStartupDialog = remember { mutableStateOf(false) }
            val isLoading = remember { mutableStateOf(true) }
            val findingAlbumsOnDevice = stringResource(id = R.string.finding_albums_on_device)

            ConfirmationDialogWithBody(
                dialogTitle = stringResource(id = R.string.first_startup_dialog_title),
                dialogBody = stringResource(id = R.string.first_startup_dialog_body),
                showDialog = showFirstStartupDialog,
                confirmButtonLabel = stringResource(id = R.string.first_startup_dialog_confirm_title)
            ) {
                mainViewModel.launch(Dispatchers.IO) {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.LoadingEvent(
                            message = findingAlbumsOnDevice,
                            icon = R.drawable.art_track,
                            isLoading = isLoading
                        )
                    )

                    mainViewModel.settings.AlbumsList.add(list = tryGetAllAlbums(context = context))
                    isLoading.value = false
                    mainViewModel.settings.User.setFirstStartup(false)
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

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun Content(
        currentView: MutableState<BottomBarTab>,
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
        multiAlbumViewModel: MultiAlbumViewModel,
        searchViewModel: SearchViewModel
    ) {
        val mainViewModel = LocalMainViewModel.current
        val mediaStoreData =
            multiAlbumViewModel.mediaFlow.collectAsLazyPagingItems()

        val tabList by mainViewModel.settings.DefaultTabs.getTabList()
            .collectAsStateWithLifecycle(initialValue = mainViewModel.settings.DefaultTabs.defaultTabList)

        val scrollBehaviour = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
            exitDirection = FloatingToolbarExitDirection.Bottom
        )

        Scaffold(
            topBar = {
                TopBar(
                    showDialog = showDialog,
                    selectedItemsList = selectedItemsList,
                    media = mediaStoreData,
                    currentView = currentView
                )
            },
            bottomBar = {
                MainAppBottomBar(
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabList,
                    scrollBehaviour = scrollBehaviour
                )
            },
            modifier = Modifier
                .fillMaxSize(1f)
                .nestedScroll(
                    object : NestedScrollConnection {
                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                            scrollBehaviour.onPostScroll(
                                consumed,
                                available,
                                source
                            )

                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                            scrollBehaviour.onPreScroll(
                                available,
                                source
                            )

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                            scrollBehaviour.onPostFling(
                                consumed,
                                available
                            )

                        override suspend fun onPreFling(available: Velocity): Velocity =
                            scrollBehaviour.onPreFling(available)
                    }
                )
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

            val navController = LocalNavController.current
            val exitImmediately by mainViewModel.settings.Behaviour.getExitImmediately().collectAsStateWithLifecycle(initialValue = false)
            val mainPage by mainViewModel.settings.DefaultTabs.getDefaultTab().collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)
            BackHandler(
                enabled = navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
                        && !exitImmediately
                        && currentView.value != mainPage
            ) {
                currentView.value = mainPage
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
                MainPages(
                    currentView = currentView,
                    multiAlbumViewModel = multiAlbumViewModel,
                    searchViewModel = searchViewModel,
                    selectedItemsList = selectedItemsList,
                    isMediaPicker = false
                )
            }
        }
    }

    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
        media: LazyPagingItems<PhotoLibraryUIModel>,
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
            media = media,
            currentView = currentView
        )
    }
}

@Composable
fun MainPages(
    currentView: MutableState<BottomBarTab>,
    multiAlbumViewModel: MultiAlbumViewModel,
    searchViewModel: SearchViewModel,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    isMediaPicker: Boolean
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val tabList by mainViewModel.settings.DefaultTabs.getTabList()
        .collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)

    val mediaStoreData = multiAlbumViewModel.mediaFlow.collectAsLazyPagingItems()

    AnimatedContent(
        targetState = currentView.value,
        transitionSpec = {
            if (tabList.indexOf(targetState) > tabList.indexOf(initialState)) {
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
                    LaunchedEffect(stateValue.albumPaths) {
                        multiAlbumViewModel.update(album = stateValue.toAlbumInfo())
                    }

                    LaunchedEffect(Unit) {
                        selectedItemsList.clear()
                    }

                    var hasFiles by remember { mutableStateOf(true) }
                    // TODO
                    // LaunchedEffect(mediaStoreData.value) {
                    //     if (stateValue.albumPaths.isNotEmpty()) {
                    //         withContext(Dispatchers.IO) {
                    //             hasFiles = stateValue.albumPaths.any { path ->
                    //                 Path(path).checkHasFiles(
                    //                     basePath = path.toBasePath()
                    //                 ) == true
                    //             }
                    //         }
                    //     }
                    // }

                    PhotoGrid(
                        pagingItems = mediaStoreData,
                        albumInfo = stateValue.toAlbumInfo(),
                        viewProperties = ViewProperties.Album,
                        selectedItemsList = selectedItemsList,
                        hasFiles = hasFiles,
                        isMainPage = true,
                        isMediaPicker = isMediaPicker
                    )
                }

                stateValue == DefaultTabs.TabTypes.photos -> {
                    val mainPhotosPaths by mainViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()

                    LaunchedEffect(mainPhotosPaths) {
                        multiAlbumViewModel.update(
                            album = stateValue
                                .copy(albumPaths = mainPhotosPaths)
                                .toAlbumInfo()
                        )
                    }

                    LaunchedEffect(Unit) {
                        selectedItemsList.clear()
                    }

                    var hasFiles by remember { mutableStateOf(true) }
                    LaunchedEffect(mediaStoreData) {
                        if (mainPhotosPaths.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                hasFiles = mainPhotosPaths.any { path ->
                                    Path(path).checkHasFiles(
                                        basePath = path.toBasePath()
                                    ) == true
                                }
                            }
                        }
                    }

                    PhotoGrid(
                        pagingItems = mediaStoreData,
                        albumInfo =
                            stateValue
                                .copy(albumPaths = mainPhotosPaths)
                                .toAlbumInfo(),
                        viewProperties = ViewProperties.Album,
                        selectedItemsList = selectedItemsList,
                        hasFiles = hasFiles,
                        isMainPage = true,
                        isMediaPicker = isMediaPicker
                    )
                }

                stateValue == DefaultTabs.TabTypes.secure -> LockedFolderEntryView()

                stateValue == DefaultTabs.TabTypes.albums -> {
                    AlbumsGridView(currentView)
                }

                stateValue == DefaultTabs.TabTypes.search -> {
                    SearchPage(
                        selectedItemsList = selectedItemsList,
                        searchViewModel = searchViewModel,
                        isMediaPicker = isMediaPicker
                    )
                }

                stateValue == DefaultTabs.TabTypes.favourites -> {
                    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()
                    val favouritesViewModel: FavouritesViewModel = viewModel(
                        factory = FavouritesViewModelFactory(
                            context = context,
                            sortMode = sortMode,
                            displayDateFormat = displayDateFormat
                        )
                    )

                    LaunchedEffect(Unit) {
                        selectedItemsList.clear()
                    }

                    val mediaStoreData = favouritesViewModel.mediaFlow.collectAsLazyPagingItems()
                    var hasFiles by remember { mutableStateOf(true) }
                    // TODO
                    // LaunchedEffect(mediaStoreData.value) {
                    //     delay(PhotoGridConstants.LOADING_TIME)
                    //     hasFiles = mediaStoreData.value.isNotEmpty()
                    // }

                    PhotoGrid(
                        pagingItems = mediaStoreData,
                        albumInfo = AlbumInfo.Empty,
                        selectedItemsList = selectedItemsList,
                        viewProperties = ViewProperties.Favourites,
                        hasFiles = hasFiles,
                        isMainPage = true,
                        isMediaPicker = isMediaPicker
                    )
                }

                stateValue == DefaultTabs.TabTypes.trash -> {
                    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

                    val trashViewModel: TrashViewModel = viewModel(
                        factory = TrashViewModelFactory(
                            context = context,
                            sortMode = sortMode,
                            format = displayDateFormat
                        )
                    )

                    val trashStoreData = trashViewModel.mediaFlow.collectAsLazyPagingItems()

                    var hasFiles by remember { mutableStateOf(true) }
                    // TODO
                    // LaunchedEffect(trashStoreData.value) {
                    //     delay(PhotoGridConstants.LOADING_TIME)
                    //     hasFiles = trashStoreData.value.isNotEmpty()
                    // }

                    PhotoGrid(
                        pagingItems = trashStoreData,
                        albumInfo = AlbumInfo.Empty,
                        selectedItemsList = selectedItemsList,
                        viewProperties = ViewProperties.Trash,
                        hasFiles = hasFiles,
                        isMainPage = true,
                        isMediaPicker = isMediaPicker
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

fun setupNextScreen(
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    window: Window
) {
    selectedItemsList.clear()
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    setBarVisibility(
        visible = true,
        window = window
    ) {}
}
