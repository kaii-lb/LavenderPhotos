package com.kaii.photos.compose

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.grids.albums.AlbumGroup
import com.kaii.photos.compose.grids.albums.SingleAlbumView
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.pages.PermissionHandler
import com.kaii.photos.compose.pages.PrivacyModeActivePage
import com.kaii.photos.compose.pages.ScreenLock
import com.kaii.photos.compose.pages.StartupLoadingPage
import com.kaii.photos.compose.pages.main.MainPages
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.rememberSingleJobRunner
import com.kaii.photos.models.CustomAlbumViewModel
import com.kaii.photos.models.FavouritesViewModel
import com.kaii.photos.models.ImmichAlbumViewModel
import com.kaii.photos.models.MainGridViewModel
import com.kaii.photos.models.MultiAlbumViewModel
import com.kaii.photos.models.PrivacyModeActiveViewModel
import com.kaii.photos.models.SAFAlbumViewModel
import com.kaii.photos.models.SearchViewModel
import com.kaii.photos.models.TrashViewModel
import com.kaii.photos.models.permissions.PermissionsViewModel
import com.kaii.photos.models.permissions.PermissionsViewModelFactory
import com.kaii.photos.permissions.StartupManager
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.screens.isMultiSelect
import com.kaii.photos.screens.retainMediaPickerState
import com.kaii.photos.setupNextScreen
import com.kaii.photos.ui.theme.PhotosTheme
import com.kaii.photos.widgets.ExpressivePINFieldState
import dagger.hilt.android.AndroidEntryPoint
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarBox
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

@AndroidEntryPoint
class MediaPicker : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isCheckingCredentials = true
        splashScreen.setKeepOnScreenCondition { isCheckingCredentials }

        val incomingIntent = intent
        val settings = PhotosApplication.appModule.settings
        val startupManager = StartupManager(
            context = applicationContext,
            settings = settings.permissions
        )

        lifecycleScope.launch {
            Glide.get(applicationContext).setMemoryCategory(MemoryCategory.HIGH)
        }

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
                navController = navControllerLocal

                CompositionLocalProvider(
                    LocalNavController provides navControllerLocal
                ) {
                    window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

                    val snackbarHostState = remember { LavenderSnackbarHostState() }
                    LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
                        Content(
                            incomingIntent = incomingIntent,
                            startupManager = startupManager,
                            startupPage = when (startupManager.state) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        PhotosApplication.appModule.logManager.stopRecording()
    }

    override fun onRestart() {
        super.onRestart()

        lifecycleScope.launch(Dispatchers.IO) {
            val password = PhotosApplication.appModule.settings.permissions.getPassword().first()

            if (password != null && navController.currentDestination?.hasRoute(Screens.Startup.ScreenLock::class) != true) {
                launch(Dispatchers.Main) {
                    navController.navigate(Screens.Startup.ScreenLock)
                }
            }
        }
    }

    @Composable
    private fun Content(
        incomingIntent: Intent,
        startupManager: StartupManager,
        startupPage: Screens
    ) {
        val navController = LocalNavController.current
        NavHost(
            navController = navController,
            startDestination = startupPage,
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
                    setupNextScreen(window)

                    val deviceAlbums by PhotosApplication.appModule.albumGridState.albums.collectAsStateWithLifecycle()

                    val viewModel = hiltViewModel<MainGridViewModel>()
                    val searchViewModel = hiltViewModel<SearchViewModel>()

                    MainPages(
                        viewModel = viewModel,
                        searchViewModel = searchViewModel,
                        deviceAlbums = { deviceAlbums },
                        window = window,
                        incomingIntent = incomingIntent,
                        refreshAlbums = PhotosApplication.appModule.albumGridState::refresh
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
                    setupNextScreen(window)

                    val screen = it.toRoute<Screens.Album.GridView>()
                    val viewModel = hiltViewModel<MultiAlbumViewModel, MultiAlbumViewModel.Factory>(
                        creationCallback = { factory ->
                            factory.create(album = screen.album)
                        }
                    )

                    SingleAlbumView(
                        album = screen.album,
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
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
                    setupNextScreen(window)

                    val screen = it.toRoute<Screens.CustomAlbum.GridView>()

                    val viewModel = hiltViewModel<CustomAlbumViewModel, CustomAlbumViewModel.Factory>(
                        creationCallback = { factory ->
                            factory.create(album = screen.album)
                        }
                    )

                    SingleAlbumView(
                        album = screen.album,
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
                    )
                }
            }

            navigation<Screens.Favourites>(
                startDestination = Screens.Favourites.GridView
            ) {
                composable<Screens.Favourites.GridView> {
                    setupNextScreen(window)

                    val viewModel = hiltViewModel<FavouritesViewModel>()
                    FavouritesGridView(
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
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
                    setupNextScreen(window)

                    val trashViewModel = hiltViewModel<TrashViewModel>()

                    TrashedPhotoGridView(
                        viewModel = trashViewModel,
                        incomingIntent = incomingIntent
                    )
                }
            }

            navigation<Screens.Immich>(
                startDestination = Screens.Immich.GridView::class
            ) {
                composable<Screens.Immich.GridView>(
                    typeMap = mapOf(
                        typeOf<AlbumType.Cloud>() to AlbumType.Cloud.NavType()
                    )
                ) {
                    setupNextScreen(window = window)

                    val screen = it.toRoute<Screens.Immich.GridView>()
                    val viewModel = hiltViewModel<ImmichAlbumViewModel, ImmichAlbumViewModel.Factory>(
                        creationCallback = { factory ->
                            factory.create(album = screen.album)
                        }
                    )

                    SingleAlbumView(
                        album = screen.album,
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
                    )
                }
            }

            navigation<Screens.SAFFolder>(
                startDestination = Screens.SAFFolder.GridView::class
            ) {
                composable<Screens.SAFFolder.GridView>(
                    typeMap = mapOf(
                        typeOf<AlbumType.SAFFolder>() to AlbumType.SAFFolder.NavType()
                    )
                ) {
                    setupNextScreen(window = window)

                    val screen = it.toRoute<Screens.SAFFolder.GridView>()
                    val viewModel = hiltViewModel<SAFAlbumViewModel, SAFAlbumViewModel.Factory>(
                        creationCallback = { factory ->
                            factory.create(album = screen.album)
                        }
                    )

                    SingleAlbumView(
                        album = screen.album,
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
                    )
                }
            }

            composable<Screens.AlbumGroup> {
                val screen = it.toRoute<Screens.AlbumGroup>()

                AlbumGroup(
                    id = screen.id
                )
            }
        }
    }
}

@Composable
fun MediaPickerConfirmButton(
    incomingIntent: Intent,
    selectionManager: SelectionManager
) {
    val state = retainMediaPickerState(incomingIntent)
    val selectedItemsList by selectionManager.selection.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(incomingIntent) {
        selectionManager.setSingleSelectModeActive(
            active = !incomingIntent.isMultiSelect
        )
    }

    BackHandler(
        enabled = state.isLoading,
        onBack = { /* block while downloading media */ }
    )

    val context = LocalContext.current
    val resources = LocalResources.current
    val runner = rememberSingleJobRunner()

    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(72.dp)
            .background(Color.Transparent)
            .padding(bottom = 16.dp)
    ) {
        Button(
            onClick = {
                runner.run {
                    val itemCount = selectedItemsList.size

                    val body = mutableStateOf(resources.getString(R.string.media_picker_processing_items_body, 0, itemCount))
                    val percentage = mutableFloatStateOf(0f)

                    launch {
                        LavenderSnackbarController.pushEvent(
                            event = LavenderSnackbarEvent.ProgressEvent(
                                message = resources.getString(R.string.media_picker_processing_items),
                                body = body,
                                icon = R.drawable.data,
                                percentage = percentage
                            )
                        )
                    }

                    launch {
                        // kinda funky state management but wtv
                        state.processedCount.collect {
                            body.value = resources.getString(R.string.media_picker_processing_items_body, it, itemCount)
                            percentage.floatValue = it.toFloat() / itemCount
                        }
                    }

                    state.shareWithApp(
                        items = selectedItemsList
                    )

                    // percentage.floatValue = 1f
                    (context as Activity).finish()
                }
            },
            shape = CircleShape,
            elevation = ButtonDefaults.elevatedButtonElevation(),
            enabled = selectedItemsList.isNotEmpty() && !state.isLoading,
            modifier = Modifier
                .width(160.dp)
                .height(52.dp)
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = stringResource(id = R.string.media_confirm),
                fontSize = TextUnit(18f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}