package com.kaii.photos.compose

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainPages
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.MainAppBottomBar
import com.kaii.photos.compose.app_bars.MainAppTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.grids.FavouritesGridView
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.pages.FavouritesMigrationPage
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.Migration3to4
import com.kaii.photos.database.Migration4to5
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Behaviour
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Immich
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import com.kaii.photos.models.loading.mapToMediaItems
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.setupNextScreen
import com.kaii.photos.ui.theme.PhotosTheme
import kotlin.reflect.typeOf

private const val TAG = "com.kaii.photos.compose.MediaPicker"

class MediaPicker : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val applicationDatabase = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).apply {
            addMigrations(Migration3to4(applicationContext), Migration4to5(applicationContext))
        }.build()

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)

        val incomingIntent = intent

        setContent {
            val selectedItemsList = remember { mutableStateListOf<PhotoLibraryUIModel>() }
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext, emptyList())
            )

            val initial =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }
            val followDarkTheme by mainViewModel.settings.LookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = initial)

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
                    LocalNavController provides navControllerLocal,
                    LocalMainViewModel provides mainViewModel,
                    LocalAppDatabase provides applicationDatabase
                ) {
                    window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())
                    Content(
                        mainViewModel = mainViewModel,
                        selectedItemsList = selectedItemsList,
                        incomingIntent = incomingIntent
                    )
                }
            }
        }
    }

    @Composable
    private fun Content(
        mainViewModel: MainViewModel,
        selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
        incomingIntent: Intent
    ) {
        val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

        val currentSortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()
        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptySet())

        val context = LocalContext.current
        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(albumsList),
                sortMode = currentSortMode,
                displayDateFormat = displayDateFormat
            )
        )

        val searchViewModel: SearchViewModel = viewModel(
            factory = SearchViewModelFactory(
                context = context,
                sortMode = currentSortMode,
                format = displayDateFormat,
                separators = true
            )
        )

        val navController = LocalNavController.current
        val immichInfo by mainViewModel.settings.Immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

        // TODO
        // LaunchedEffect(albumsList) {
        //     if (navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
        //         || multiAlbumViewModel.albumInfo.paths.toSet() == albumsList
        //     ) return@LaunchedEffect
        //
        //     Log.d(TAG, "Refreshing main photos view")
        //     Log.d(TAG, "In view model: ${multiAlbumViewModel.albumInfo.paths} new: $albumsList")
        //     multiAlbumViewModel.reinitDataSource(
        //         context = context,
        //         album = AlbumInfo.createPathOnlyAlbum(albumsList),
        //         sortMode = currentSortMode
        //     )
        // }

        val currentView = rememberSaveable(
            stateSaver = BottomBarTab.TabSaver
        ) {
            mutableStateOf(DefaultTabs.TabTypes.photos)
        }

        LaunchedEffect(currentView.value) {
            if (currentView.value != DefaultTabs.TabTypes.search) {
                searchViewModel.clear()
            }
        }

        NavHost(
            navController = navController,
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
                setupNextScreen(
                    selectedItemsList = selectedItemsList,
                    window = window
                )

                val tabs by mainViewModel.settings.DefaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)
                MainWindow(
                    multiAlbumViewModel = multiAlbumViewModel,
                    searchViewModel = searchViewModel,
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabs,
                    incomingIntent = incomingIntent
                )
            }

            navigation<Screens.Album>(
                startDestination = Screens.Favourites.GridView::class
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
                        viewModel = multiAlbumViewModel,
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
                    val screen = it.toRoute<Screens.CustomAlbum.GridView>()

                    val viewModel: CustomAlbumViewModel = viewModel(
                        factory = CustomAlbumViewModelFactory(
                            context = context,
                            albumInfo = screen.albumInfo,
                            info = immichInfo,
                            sortBy = currentSortMode,
                            displayDateFormat = displayDateFormat
                        )
                    )

                    SingleAlbumView(
                        albumInfo = screen.albumInfo,
                        selectedItemsList = selectedItemsList,
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
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

                    val viewModel = viewModel<FavouritesViewModel>(
                        factory = FavouritesViewModelFactory(
                            context = context,
                            info = immichInfo,
                            sortMode = currentSortMode,
                            displayDateFormat = displayDateFormat
                        )
                    )

                    FavouritesGridView(
                        selectedItemsList = selectedItemsList,
                        viewModel = viewModel,
                        incomingIntent = incomingIntent
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

                    val trashViewModel = viewModel<TrashViewModel>(
                        factory = TrashViewModelFactory(
                            context = context,
                            sortMode = currentSortMode,
                            format = displayDateFormat
                        )
                    )

                    TrashedPhotoGridView(
                        selectedItemsList = selectedItemsList,
                        viewModel = trashViewModel,
                        incomingIntent = incomingIntent
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun MainWindow(
        currentView: MutableState<BottomBarTab>,
        tabs: List<BottomBarTab>,
        selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
        multiAlbumViewModel: MultiAlbumViewModel,
        searchViewModel: SearchViewModel,
        incomingIntent: Intent
    ) {
        val mainViewModel = LocalMainViewModel.current

        val mediaStoreData = multiAlbumViewModel.mediaFlow.collectAsLazyPagingItems()

        val showDialog = rememberSaveable { mutableStateOf(false) }
        val scrollBehaviour = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
            exitDirection = FloatingToolbarExitDirection.Bottom
        )

        Scaffold(
            topBar = {
                TopBar(
                    showDialog = showDialog,
                    selectedItemsList = selectedItemsList,
                    pagingItems = mediaStoreData,
                    currentView = currentView
                )
            },
            modifier = Modifier
                .fillMaxSize(1f)
                .nestedScroll(
                    object : NestedScrollConnection {
                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                            scrollBehaviour.onPostScroll(
                                if (selectedItemsList.isEmpty()) consumed else Offset.Zero,
                                if (selectedItemsList.isEmpty()) available else Offset.Zero,
                                source
                            )

                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                            scrollBehaviour.onPreScroll(
                                if (selectedItemsList.isEmpty()) available else Offset.Zero,
                                source
                            )

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                            scrollBehaviour.onPostFling(
                                if (selectedItemsList.isEmpty()) consumed else Velocity.Zero,
                                available
                            )

                        override suspend fun onPreFling(available: Velocity): Velocity =
                            scrollBehaviour.onPreFling(if (selectedItemsList.isEmpty()) available else Velocity.Zero)
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

            Box {
                Column(
                    modifier = Modifier
                        .padding(
                            safeDrawingPadding.first,
                            padding.calculateTopPadding(),
                            safeDrawingPadding.second,
                            0.dp
                        )
                ) {
                    MainPages(
                        currentView = currentView,
                        multiAlbumViewModel = multiAlbumViewModel,
                        searchViewModel = searchViewModel,
                        selectedItemsList = selectedItemsList,
                        isMediaPicker = true
                    )
                }

                val navController = LocalNavController.current
                AnimatedContent(
                    targetState = selectedItemsList.isNotEmpty() && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
                    transitionSpec = {
                        getAppBarContentTransition(selectedItemsList.isNotEmpty())
                    },
                    label = "MainBottomBarAnimatedContentView",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) { state ->
                    if (!state) {
                        MainAppBottomBar(
                            currentView = currentView,
                            tabs = tabs.fastFilter { it != DefaultTabs.TabTypes.secure },
                            selectedItemsList = selectedItemsList,
                            scrollBehaviour = scrollBehaviour
                        )
                    } else {
                        MediaPickerConfirmButton(
                            incomingIntent = incomingIntent,
                            selectedItemsList = selectedItemsList,
                            contentResolver = contentResolver
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
        pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
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
            media = pagingItems,
            currentView = currentView,
            isFromMediaPicker = true
        )
    }
}

@Composable
fun MediaPickerConfirmButton(
    incomingIntent: Intent,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
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
                    val uris = selectedItemsList.mapToMediaItems().map { it.uri.toUri() }

                    Log.d(TAG, "Selected items are $selectedItemsList")

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
                    val first = (selectedItemsList.first { it is PhotoLibraryUIModel.MediaImpl } as PhotoLibraryUIModel.MediaImpl).item
                    val resultIntent = Intent().apply {
                        data = first.uri.toUri()
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    Log.d(TAG, "Selected item is $first")

                    activity.setResult(RESULT_OK, resultIntent)
                }

                (context as Activity).finish()
            },
            shape = CircleShape,
            elevation = ButtonDefaults.elevatedButtonElevation(),
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