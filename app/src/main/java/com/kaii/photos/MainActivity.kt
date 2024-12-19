package com.kaii.photos

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
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
import com.kaii.photos.compose.settings.AboutPage
import com.kaii.photos.compose.IsSelectingTopBar
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
import com.kaii.photos.compose.settings.DebuggingSettingsPage
import com.kaii.photos.compose.settings.GeneralSettingsPage
import com.kaii.photos.compose.settings.MainSettingsPage
import com.kaii.photos.compose.single_photo.EditingView
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.addToAlbumsList
import com.kaii.photos.datastore.getAlbumsList
import com.kaii.photos.datastore.getIsV083FirstStart
import com.kaii.photos.datastore.setAlbumsList
import com.kaii.photos.datastore.setIsV083FirstStart
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.EditingScreen
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.getBaseInternalStorageDirectory
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val LocalNavController: ProvidableCompositionLocal<NavHostController?> = compositionLocalOf {
   null
}

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var applicationDatabase: MediaDatabase
        lateinit var mainViewModel: MainViewModel

        lateinit var startForResult: ActivityResultLauncher<Intent>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val mediaDatabase = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).build()
        applicationDatabase = mediaDatabase

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        setContent {
            mainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext)
            )

            mainViewModel.startupPermissionCheck(applicationContext)
            val continueToApp = remember {
                mutableStateOf(
                    // Manifest.permission.MANAGE_MEDIA is optional
                    mainViewModel.checkCanPass()
                )
            }

            PhotosTheme {
                if (!continueToApp.value) {
                    PermissionHandler(continueToApp)
                } else {
                    SetContentForActivity()
                }
            }
        }
    }

    @Composable
    private fun SetContentForActivity() {
        window.decorView.setBackgroundColor(CustomMaterialTheme.colorScheme.background.toArgb())

        val navControllerLocal = rememberNavController()

        val currentView =
            rememberSaveable { mutableStateOf(MainScreenViewType.PhotosGridView) }

        val showDialog = remember { mutableStateOf(false) }
        val windowInsetsController = window.insetsController
        val scale = remember { mutableFloatStateOf(1f) }
        val rotation = remember { mutableFloatStateOf(0f) }
        val offset = remember { mutableStateOf(Offset.Zero) }
        val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }

        val context = LocalContext.current

        val logPath = "${getBaseInternalStorageDirectory()}LavenderPhotos/log.txt"
        try {
            java.io.File(logPath).delete()
        } catch (e: Throwable) {
            // ignore
        }

        val canRecordLogs = mainViewModel.settingsLogs.recordLogs.collectAsStateWithLifecycle(initialValue = false)
        if (canRecordLogs.value) {
            Runtime.getRuntime().exec("logcat -f $logPath");
        }

        // TODO: please make it not hang lol
        runBlocking {
            if (context.datastore.getIsV083FirstStart(context)) {
                context.datastore.setIsV083FirstStart(false)

                val list = context.datastore.getAlbumsList(true)
                context.datastore.setAlbumsList(list)
            }

            context.datastore.addToAlbumsList("DCIM/Camera")
            // context.datastore.addToAlbumsList("Pictures/Screenshot")
            // context.datastore.addToAlbumsList("Pictures/Whatsapp")
            // context.datastore.addToAlbumsList("Pictures/100PINT/Pins")
            // context.datastore.addToAlbumsList("Movies")
            // context.datastore.addToAlbumsList("Download")
            // context.datastore.addToAlbumsList("Pictures/Instagram")
        }

		val localConfig = LocalConfiguration.current
		var orientation by remember { mutableStateOf(localConfig.orientation) }

		LaunchedEffect(localConfig) {
			orientation = localConfig.orientation
		}

        CompositionLocalProvider(LocalNavController provides navControllerLocal) {
            NavHost(
                navController = navControllerLocal,
                startDestination = MultiScreenViewType.MainScreen.name,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .background(CustomMaterialTheme.colorScheme.background),
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
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle =
                        if (!isSystemInDarkTheme()) {
                            SystemBarStyle.light(
                                CustomMaterialTheme.colorScheme.background.toArgb(),
                                CustomMaterialTheme.colorScheme.background.toArgb()
                            )
                        } else {
                            SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb())
                        }
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    Content(currentView, navControllerLocal, showDialog, selectedItemsList)
                }

                composable(
                    route = MultiScreenViewType.SinglePhotoView.name,
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { height -> -height } + fadeIn(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        )
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { height -> height } + fadeOut(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        )
                    }
                ) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(
                            CustomMaterialTheme.colorScheme.surfaceContainer.copy(
                                alpha = 0.2f
                            ).toArgb()
                        ),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
                            CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    SinglePhotoView(navControllerLocal, window, scale, rotation, offset)
                }

                composable(MultiScreenViewType.SingleAlbumView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surface.toArgb(),
                            CustomMaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    SingleAlbumView(navControllerLocal, selectedItemsList)
                }

                composable(MultiScreenViewType.SingleTrashedPhotoView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surface.toArgb(),
                            CustomMaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    SingleTrashedPhotoView(navControllerLocal, window, scale, rotation, offset)
                }

                composable(MultiScreenViewType.TrashedPhotoView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surface.toArgb(),
                            CustomMaterialTheme.colorScheme.surface.toArgb()
                        )
                    )

                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    TrashedPhotoGridView(navControllerLocal, selectedItemsList)
                }

                composable(MultiScreenViewType.LockedFolderView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surface.toArgb(),
                            CustomMaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    LockedFolderView(navControllerLocal, window)
                }

                composable(MultiScreenViewType.SingleHiddenPhotoVew.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surface.toArgb(),
                            CustomMaterialTheme.colorScheme.surface.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    SingleHiddenPhotoView(navControllerLocal, window, scale, rotation, offset)
                }

                composable(MultiScreenViewType.AboutAndUpdateView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.background.toArgb(),
                            CustomMaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    AboutPage(navControllerLocal)
                }

                composable(MultiScreenViewType.FavouritesGridView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.background.toArgb(),
                            CustomMaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    FavouritesGridView(
                        navController = navControllerLocal,
                        selectedItemsList = selectedItemsList
                    )
                }

                composable<EditingScreen>(
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
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.surfaceContainer.toArgb(),
                            CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    val screen: EditingScreen = it.toRoute()
                    EditingView(
                        navController = navControllerLocal,
                        absolutePath = screen.absolutePath,
                        dateTaken = screen.dateTaken,
                        uri = screen.uri.toUri()
                    )
                }

                composable(MultiScreenViewType.SettingsMainView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.background.toArgb(),
                            CustomMaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        context,
                        windowInsetsController,
                        selectedItemsList,
                        window,
                        orientation
                    )

                    MainSettingsPage()
                }

                composable(MultiScreenViewType.SettingsDebuggingView.name) {
                   enableEdgeToEdge(
                       navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                       statusBarStyle = SystemBarStyle.auto(
                           CustomMaterialTheme.colorScheme.background.toArgb(),
                           CustomMaterialTheme.colorScheme.background.toArgb()
                       )
                   )
                   setupNextScreen(
                       context,
                       windowInsetsController,
                       selectedItemsList,
                       window,
                       orientation
                   )

                   DebuggingSettingsPage()
                }

                composable(MultiScreenViewType.SettingsGeneralView.name) {
                   enableEdgeToEdge(
                       navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                       statusBarStyle = SystemBarStyle.auto(
                           CustomMaterialTheme.colorScheme.background.toArgb(),
                           CustomMaterialTheme.colorScheme.background.toArgb()
                       )
                   )
                   setupNextScreen(
                       context,
                       windowInsetsController,
                       selectedItemsList,
                       window,
                       orientation
                   )

                   GeneralSettingsPage()
                }
            }
        }
    }

    @Composable
    private fun Content(
        currentView: MutableState<MainScreenViewType>,
        navController: NavHostController,
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
    ) {
        val galleryViewModel: GalleryViewModel = viewModel(
            factory = GalleryViewModelFactory(
                LocalContext.current,
                stringResource(id = R.string.default_homepage_photogrid_dir),
                MediaItemSortMode.DateTaken
            )
        )

        val mediaStoreData =
            galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

        val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }

        LaunchedEffect(mediaStoreData.value) {
            groupedMedia.value = mediaStoreData.value
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar(showDialog, selectedItemsList, navController)
            },
            bottomBar = {
                BottomBar(currentView, selectedItemsList, navController)
            }
        ) { padding ->
            BackHandler(
                enabled = currentView.value != MainScreenViewType.PhotosGridView && currentView.value != MainScreenViewType.SearchPage && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name && selectedItemsList.size == 0
            ) {
                currentView.value = MainScreenViewType.PhotosGridView
            }

            Column(
                modifier = Modifier
                    .padding(
                        0.dp,
                        padding.calculateTopPadding(),
                        0.dp,
                        padding.calculateBottomPadding()
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                    MainAppDialog(showDialog, currentView, navController, selectedItemsList)

                    AnimatedContent(
                        targetState = currentView.value,
                        transitionSpec = {
                            if (targetState.index > initialState.index) {
                                (slideInHorizontally { height -> height } + fadeIn()).togetherWith(
                                    slideOutHorizontally { height -> -height } + fadeOut())
                            } else {
                                (slideInHorizontally { height -> -height } + fadeIn()).togetherWith(
                                    slideOutHorizontally { height -> height } + fadeOut())
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
                                    navController = navController,
                                    path = stringResource(id = R.string.default_homepage_photogrid_dir),
                                    viewProperties = ViewProperties.Album,
                                    selectedItemsList = selectedItemsList,
                                )
                            }

                            MainScreenViewType.SecureFolder -> LockedFolderEntryView(navController)
                            MainScreenViewType.AlbumsGridView -> {
                                AlbumsGridView(navController)
                            }

                            MainScreenViewType.SearchPage -> {
                                SearchPage(navController, selectedItemsList, currentView)
                            }
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
        navController: NavHostController
    ) {
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
            label = "MainTopBarAnimatedContentView"
        ) { target ->
            if (!target) {
                MainAppTopBar(showDialog = showDialog)
            } else {
                IsSelectingTopBar(selectedItemsList = selectedItemsList)
            }
        }
    }

    @Composable
    private fun BottomBar(
        currentView: MutableState<MainScreenViewType>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        navController: NavHostController
    ) {
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
    context: Context,
    windowInsetsController: WindowInsetsController?,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    window: Window,
    orientation: Int
) {
    selectedItemsList.clear()
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    window.setDecorFitsSystemWindows(false)
}
