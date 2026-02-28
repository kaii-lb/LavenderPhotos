package com.kaii.photos.compose

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.pages.main.MainPages
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.state.rememberAlbumGridState
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.main_grid.MainGridViewModel
import com.kaii.photos.models.main_grid.MainGridViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.setupNextScreen
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.reflect.typeOf

// private const val TAG = "com.kaii.photos.compose.MediaPicker"

class MediaPicker : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val incomingIntent = intent
        val settings = applicationContext.appModule.settings

        setContent {
            val initialFollowDarkTheme = runBlocking {
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
        val multiAlbumViewModel = viewModel<MultiAlbumViewModel>(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.Empty
            )
        )

        val navController = LocalNavController.current
        val deviceAlbums = rememberAlbumGridState().albums.collectAsStateWithLifecycle()

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
                        typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                    )
                ) {
                    setupNextScreen(window)

                    val storeOwner = remember(it) {
                        navController.getBackStackEntry(Screens.MainPages)
                    }
                    val viewModel = viewModel<MainGridViewModel>(
                        viewModelStoreOwner = storeOwner,
                        factory = MainGridViewModelFactory(context = context)
                    )

                    MainPages(
                        multiAlbumViewModel = multiAlbumViewModel,
                        searchViewModel = searchViewModel,
                        mainGridViewModel = viewModel,
                        deviceAlbums = deviceAlbums,
                        window = window,
                        incomingIntent = null,
                        blur = false
                    )
                }
            }

            navigation<Screens.Album>(
                startDestination = Screens.Favourites.GridView::class
            ) {
                composable<Screens.Album.GridView>(
                    typeMap = mapOf(
                        typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
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
                            albumInfo = screen.albumInfo
                        )
                    )

                    SingleAlbumView(
                        albumInfo = screen.albumInfo,
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
                        typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType
                    )
                ) {
                    setupNextScreen(window)

                    val screen = it.toRoute<Screens.CustomAlbum.GridView>()

                    val viewModel: CustomAlbumViewModel = viewModel(
                        factory = CustomAlbumViewModelFactory(
                            context = context,
                            albumInfo = screen.albumInfo
                        )
                    )

                    SingleAlbumView(
                        albumInfo = screen.albumInfo,
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
                        factory = FavouritesViewModelFactory(
                            context = context
                        )
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
        }
    }
}

@Composable
fun MediaPickerConfirmButton(
    incomingIntent: Intent,
    uris: List<Uri>,
    contentResolver: ContentResolver
) {
    val context = LocalContext.current
    val activity = remember(context) { context as Activity }

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
                if (incomingIntent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                    || incomingIntent.action == Intent.ACTION_OPEN_DOCUMENT
                ) {
                    val resultIntent = Intent().apply {
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val clipData = ClipData.newUri(contentResolver, "Media", uris.first())
                        for (i in 1 until uris.size) {
                            clipData.addItem(ClipData.Item(uris[i]))
                        }
                        setClipData(clipData)
                    }

                    activity.setResult(RESULT_OK, resultIntent)
                } else {
                    val resultIntent = Intent().apply {
                        data = uris.first()
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    activity.setResult(RESULT_OK, resultIntent)
                }

                (context as Activity).finish()
            },
            shape = CircleShape,
            elevation = ButtonDefaults.elevatedButtonElevation(),
            enabled = uris.isNotEmpty(),
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