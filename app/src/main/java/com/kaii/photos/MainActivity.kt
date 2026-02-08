package com.kaii.photos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
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
import com.kaii.lavender.immichintegration.clients.ApiClient
import com.kaii.lavender.immichintegration.state_managers.LocalApiClient
import com.kaii.lavender.snackbars.LavenderSnackbarBox
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.lavender.snackbars.LavenderSnackbarHostState
import com.kaii.photos.compose.PermissionHandler
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.SecureFolderView
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.immich.ImmichAlbumPage
import com.kaii.photos.compose.immich.ImmichMainPage
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.pages.main.MainPages
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
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.sync.SyncWorker
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Debugging
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
import com.kaii.photos.helpers.startupUpdateCheck
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

        mainViewModel.startupPermissionCheck(applicationContext)

        val initialFollowDarkTheme = runBlocking {
            mainViewModel.settings.LookAndFeel.getFollowDarkMode().first()
        }

        // TODO: uncook this
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                SyncWorker::class.java.name,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(SyncWorker::class).build()
            )

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

        val navController = LocalNavController.current
        val immichInfo by mainViewModel.settings.Immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)
        val mainPhotosPaths by mainViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()
        val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
        val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

        val multiAlbumViewModel = viewModel<MultiAlbumViewModel>(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(mainPhotosPaths),
                info = immichInfo,
                sortMode = sortMode,
                format = displayDateFormat
            )
        )
        val searchViewModel: SearchViewModel = viewModel(
            factory = SearchViewModelFactory(
                context = context,
                info = immichInfo,
                sortMode = sortMode,
                format = displayDateFormat
            )
        )

        val snackbarHostState = remember {
            LavenderSnackbarHostState()
        }

        LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = Screens.MainPages,
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
                navigation<Screens.MainPages>(
                    startDestination = Screens.MainPages.MainGrid.GridView(
                        albumInfo = AlbumInfo.createPathOnlyAlbum(mainPhotosPaths)
                    )
                ) {
                    composable<Screens.MainPages.MainGrid.GridView>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen = it.toRoute<Screens.MainPages.MainGrid.GridView>()

                        multiAlbumViewModel.update(
                            album = screen.albumInfo,
                            sortMode = sortMode,
                            format = displayDateFormat,
                            accessToken = immichInfo.accessToken
                        )

                        MainPages(
                            selectedItemsList = selectedItemsList,
                            mainPhotosPaths = mainPhotosPaths,
                            multiAlbumViewModel = multiAlbumViewModel,
                            searchViewModel = searchViewModel,
                            window = window,
                            incomingIntent = null
                        )
                    }

                    composable<Screens.MainPages.MainGrid.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen = it.toRoute<Screens.MainPages.MainGrid.SinglePhoto>()
                        multiAlbumViewModel.update(
                            album = screen.albumInfo,
                            sortMode = sortMode,
                            format = displayDateFormat,
                            accessToken = immichInfo.accessToken
                        )

                        SinglePhotoView(
                            window = window,
                            viewModel = multiAlbumViewModel,
                            index = screen.index,
                            albumInfo = screen.albumInfo,
                            nextMediaItemId = screen.nextMediaItemId
                        )
                    }

                    composable<Screens.MainPages.Search.SinglePhoto>(
                        typeMap = mapOf(
                            typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                        )
                    ) {
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen = it.toRoute<Screens.MainPages.Search.SinglePhoto>()
                        searchViewModel.update(
                            sortMode = sortMode,
                            format = displayDateFormat,
                            accessToken = immichInfo.accessToken
                        )

                        SinglePhotoView(
                            window = window,
                            viewModel = searchViewModel,
                            index = screen.index,
                            albumInfo = AlbumInfo.Empty,
                            nextMediaItemId = screen.nextMediaItemId
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
                        setupNextScreen(
                            selectedItemsList,
                            window
                        )

                        val screen = it.toRoute<Screens.Album.GridView>()
                        multiAlbumViewModel.update(
                            album = screen.albumInfo,
                            sortMode = sortMode,
                            format = displayDateFormat,
                            accessToken = immichInfo.accessToken
                        )

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

                        val screen = it.toRoute<Screens.Album.SinglePhoto>()
                        multiAlbumViewModel.update(
                            album = screen.albumInfo,
                            sortMode = sortMode,
                            format = displayDateFormat,
                            accessToken = immichInfo.accessToken
                        )

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
                                info = immichInfo,
                                sortMode = sortMode,
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
                                info = immichInfo,
                                sortMode = sortMode,
                                displayDateFormat = displayDateFormat
                            )
                        )

                        val screen = it.toRoute<Screens.Favourites.SinglePhoto>()
                        SinglePhotoView(
                            viewModel = viewModel,
                            window = window,
                            index = screen.index,
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
                                info = immichInfo,
                                sortMode = sortMode,
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
                                info = immichInfo,
                                sortMode = sortMode,
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
                                info = immichInfo,
                                sortMode = sortMode,
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
                                info = immichInfo,
                                sortMode = sortMode,
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
                                sortMode = sortMode,
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
                                sortMode = sortMode,
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
                                info = immichInfo,
                                sortBy = sortMode,
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
                                info = immichInfo,
                                sortBy = sortMode,
                                displayDateFormat = displayDateFormat
                            )
                        )

                        SinglePhotoView(
                            albumInfo = screen.albumInfo,
                            viewModel = viewModel,
                            index = screen.index,
                            window = window,
                            nextMediaItemId = screen.nextMediaItemId
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

                    GeneralSettingsPage()
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
