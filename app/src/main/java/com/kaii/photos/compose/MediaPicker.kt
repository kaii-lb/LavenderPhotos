package com.kaii.photos.compose

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.kaii.photos.compose.pages.main.MainPages
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.rememberSingleJobRunner
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
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.screens.retainMediaPickerState
import com.kaii.photos.setupNextScreen
import com.kaii.photos.ui.theme.PhotosTheme
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.reflect.typeOf

class MediaPicker : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val incomingIntent = intent
        val settings = PhotosApplication.appModule.settings

        setContent {
            val initialFollowDarkTheme = runBlocking(Dispatchers.IO) {
                settings.lookAndFeel.getFollowDarkMode().first()
            }
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
                    window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

                    Content(incomingIntent = incomingIntent)
                }
            }
        }
    }

    @Composable
    private fun Content(
        incomingIntent: Intent
    ) {
        val context = LocalContext.current
        val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(context = context))

        val navController = LocalNavController.current
        NavHost(
            navController = navController,
            startDestination = Screens.MainPages,
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
                    val storeOwner = remember(it) {
                        navController.getBackStackEntry(Screens.MainPages)
                    }
                    val viewModel = viewModel<MainGridViewModel>(
                        viewModelStoreOwner = storeOwner,
                        factory = MainGridViewModelFactory(context = context)
                    )

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
                    val storeOwner = remember(it) {
                        navController.getBackStackEntry(Screens.Album)
                    }
                    val viewModel = viewModel<MultiAlbumViewModel>(
                        viewModelStoreOwner = storeOwner,
                        factory = MultiAlbumViewModelFactory(
                            context = context,
                            album = screen.album
                        )
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

                    val viewModel: CustomAlbumViewModel = viewModel(
                        factory = CustomAlbumViewModelFactory(
                            context = context,
                            album = screen.album
                        )
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

                    val viewModel = viewModel<FavouritesViewModel>(
                        factory = FavouritesViewModelFactory(context)
                    )

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

                    val trashViewModel = viewModel<TrashViewModel>(
                        factory = TrashViewModelFactory(
                            context = context
                        )
                    )

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
                    val viewModel = viewModel<ImmichAlbumViewModel>(
                        factory = ImmichAlbumViewModelFactory(
                            context = context,
                            album = screen.album
                        )
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
    items: () -> List<SelectionManager.SelectedItem>
) {
    val state = retainMediaPickerState(incomingIntent)

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
                    val itemCount = items().size

                    val body = mutableStateOf(resources.getString(R.string.media_picker_processing_items_body, 0, itemCount))
                    val percentage = mutableFloatStateOf(0f)

                    LavenderSnackbarController.pushEvent(
                        event = LavenderSnackbarEvent.ProgressEvent(
                            message = resources.getString(R.string.media_picker_processing_items),
                            body = body,
                            icon = R.drawable.data,
                            percentage = percentage
                        )
                    )

                    launch {
                        // kinda funky state management but wtv
                        state.processedCount.collect {
                            body.value = resources.getString(R.string.media_picker_processing_items_body, it, itemCount)
                            percentage.floatValue = it.toFloat() / itemCount
                        }
                    }

                    state.shareWithApp(
                        items = items()
                    )

                    percentage.floatValue = 1f
                    (context as Activity).finish()
                }
            },
            shape = CircleShape,
            elevation = ButtonDefaults.elevatedButtonElevation(),
            enabled = items().isNotEmpty(),
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