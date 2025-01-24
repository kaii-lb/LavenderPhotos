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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
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
import com.kaii.photos.compose.IsSelectingTopBar
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.MainAppBottomBar
import com.kaii.photos.compose.MainAppDialog
import com.kaii.photos.compose.MainAppSelectingBottomBar
import com.kaii.photos.compose.PermissionHandler
import com.kaii.photos.compose.PrototypeMainTopBar
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
import com.kaii.photos.compose.single_photo.EditingView
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.Migration3to4
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Debugging
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.Versions
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.EditingScreen
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.getAppStorageDir
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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
            addMigrations(Migration3to4(applicationContext))
        }.build()
        applicationDatabase = mediaDatabase

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        setContent {
            mainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext)
            )

			val appDataDir = getAppStorageDir()
            val logPath = "$appDataDir/log.txt"

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
        window.decorView.setBackgroundColor(CustomMaterialTheme.colorScheme.background.toArgb())

        val navControllerLocal = rememberNavController()

        val currentView =
            rememberSaveable { mutableStateOf(MainScreenViewType.PhotosGridView) }

        val context = LocalContext.current
        val showDialog = remember { mutableStateOf(false) }

        val scale = remember { mutableFloatStateOf(1f) }
        val rotation = remember { mutableFloatStateOf(0f) }
        val offset = remember { mutableStateOf(Offset.Zero) }
        val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }

        val isV083FirstStart by mainViewModel.settings.Versions.getIsV083FirstStart(context).collectAsStateWithLifecycle(initialValue = false)
        LaunchedEffect(isV083FirstStart) {
	        if (isV083FirstStart) {
	            mainViewModel.settings.Versions.setIsV083FirstStart(false)

                mainViewModel.settings.AlbumsList.getAlbumsList(true).collectLatest {
                    mainViewModel.settings.AlbumsList.setAlbumsList(it)
                }
	        }
        }

        mainViewModel.settings.AlbumsList.addToAlbumsList("DCIM/Camera")

        val listOfDirs = mainViewModel.settings.AlbumsList.getAlbumsList().collectAsStateWithLifecycle(initialValue = emptyList()).value.toMutableList()

        listOfDirs.sortByDescending {
            File("$baseInternalStorageDirectory$it").lastModified()
        }
        listOfDirs.find { it == "DCIM/Camera" }?.let { cameraItem ->
            listOfDirs.remove(cameraItem)
            listOfDirs.add(0, cameraItem)
        }


		// TODO: make absolute path to uri function and take permission here
		// also check if already in persistable uris
   //      listOfDirs.forEach { dir ->
			// context.contentResolver.takePersistableUriPermission(dir)
   //      }

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
                        selectedItemsList = selectedItemsList,
                        window = window
                    )

                    Content(currentView, showDialog, selectedItemsList, listOfDirs)
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
                        selectedItemsList,
                        window
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
                        selectedItemsList,
                        window
                    )

                    SingleAlbumView(selectedItemsList)
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
                        selectedItemsList,
                        window
                    )

                    SingleTrashedPhotoView(window, scale, rotation, offset)
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
                        selectedItemsList,
                        window
                    )

                    TrashedPhotoGridView(selectedItemsList)
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
                        selectedItemsList,
                        window
                    )

                    LockedFolderView(window)
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
                        selectedItemsList,
                        window
                    )

                    SingleHiddenPhotoView(window, scale, rotation, offset)
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
                        selectedItemsList,
                        window
                    )

                    AboutPage {
                        navControllerLocal.popBackStack()
                    }
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
                        selectedItemsList,
                        window
                    )

                    FavouritesGridView(
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
                        selectedItemsList,
                        window
                    )

                    val screen: EditingScreen = it.toRoute()
                    EditingView(
                        absolutePath = screen.absolutePath,
                        dateTaken = screen.dateTaken,
                        uri = screen.uri.toUri(),
                        window = window
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
                        selectedItemsList,
                        window
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
                        selectedItemsList,
                        window
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
                        selectedItemsList,
                        window
                    )

                    GeneralSettingsPage()
                }

                composable(MultiScreenViewType.SettingsMemoryAndStorageView.name) {
                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.background.toArgb(),
                            CustomMaterialTheme.colorScheme.background.toArgb()
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
                        navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                        statusBarStyle = SystemBarStyle.auto(
                            CustomMaterialTheme.colorScheme.background.toArgb(),
                            CustomMaterialTheme.colorScheme.background.toArgb()
                        )
                    )
                    setupNextScreen(
                        selectedItemsList,
                        window
                    )

                    LookAndFeelSettingsPage()
                }
            }
        }
    }

    @Composable
    private fun Content(
        currentView: MutableState<MainScreenViewType>,
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        listOfDirs: List<String>
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
            topBar = {
                TopBar(showDialog, selectedItemsList, groupedMedia.value)
            },
            bottomBar = {
                BottomBar(currentView, selectedItemsList)
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
                                path = stringResource(id = R.string.default_homepage_photogrid_dir),
                                viewProperties = ViewProperties.Album,
                                selectedItemsList = selectedItemsList,
                            )
                        }

                        MainScreenViewType.SecureFolder -> LockedFolderEntryView(currentView)
                        MainScreenViewType.AlbumsGridView -> {
                            AlbumsGridView(listOfDirs, currentView)
                        }

                        MainScreenViewType.SearchPage -> {
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
        groupedMedia: List<MediaStoreData>
    ) {
		val navController = LocalNavController.current
		val show by remember {
		    derivedStateOf {
		        selectedItemsList.size > 0
		    }
		}

        PrototypeMainTopBar(
            alternate = show,
            showDialog = showDialog,
            selectedItemsList = selectedItemsList,
            groupedMedia = groupedMedia
        )

        // AnimatedContent(
        //     targetState = show && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
        //     transitionSpec = {
        //         getAppBarContentTransition(show)
        //     },
        //     label = "MainTopBarAnimatedContentView"
        // ) { target ->
        //     if (!target) {
        //         MainAppTopBar(showDialog = showDialog)
        //     } else {
        //         IsSelectingTopBar(selectedItemsList = selectedItemsList)
        //     }
        // }
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
