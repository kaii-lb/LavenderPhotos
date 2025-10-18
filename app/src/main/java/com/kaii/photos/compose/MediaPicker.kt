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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.MainAppBottomBar
import com.kaii.photos.compose.app_bars.MainAppTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.Migration3to4
import com.kaii.photos.database.Migration4to5
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Behaviour
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.custom_album.CustomAlbumViewModelFactory
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.main_activity.MainViewModel
import com.kaii.photos.models.main_activity.MainViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModelFactory
import com.kaii.photos.models.multi_album.groupPhotosBy
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import com.kaii.photos.setupNextScreen
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
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
            val selectedItemsList = remember { mutableStateListOf<MediaStoreData>() }
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
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        incomingIntent: Intent
    ) {
        val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()

        val currentSortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()
        val albumsList by mainViewModel.settings.MainPhotosView.getAlbums()
            .collectAsStateWithLifecycle(initialValue = emptyList())

        val context = LocalContext.current
        val multiAlbumViewModel: MultiAlbumViewModel = viewModel(
            factory = MultiAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(albumsList),
                sortBy = currentSortMode,
                displayDateFormat = displayDateFormat
            )
        )

        val customAlbumViewModel: CustomAlbumViewModel = viewModel(
            factory = CustomAlbumViewModelFactory(
                context = context,
                albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                sortBy = currentSortMode,
                displayDateFormat = displayDateFormat
            )
        )

        val navController = LocalNavController.current
        LaunchedEffect(albumsList) {
            if (navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
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

        val currentView = rememberSaveable(
            stateSaver = BottomBarTab.TabSaver
        ) {
            mutableStateOf(DefaultTabs.TabTypes.photos)
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

                val tabs by mainViewModel.settings.DefaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)
                MainWindow(
                    multiAlbumViewModel = multiAlbumViewModel,
                    currentView = currentView,
                    selectedItemsList = selectedItemsList,
                    tabs = tabs,
                    incomingIntent = incomingIntent
                )
            }

            composable<Screens.SingleAlbumView>(
                typeMap = mapOf(
                    typeOf<AlbumInfo>() to AlbumInfo.AlbumNavType,
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
                            sortMode = multiAlbumViewModel.sortMode
                        )
                    }

                    SingleAlbumView(
                        albumInfo = screen.albumInfo,
                        selectedItemsList = selectedItemsList,
                        viewModel = multiAlbumViewModel,
                        incomingIntent = incomingIntent
                    )
                } else {
                    if (screen.albumInfo != multiAlbumViewModel.albumInfo) {
                        customAlbumViewModel.reinitDataSource(
                            context = context,
                            album = screen.albumInfo,
                            sortMode = customAlbumViewModel.sortMode
                        )
                    }

                    SingleAlbumView(
                        albumInfo = screen.albumInfo,
                        selectedItemsList = selectedItemsList,
                        customViewModel = customAlbumViewModel,
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
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        multiAlbumViewModel: MultiAlbumViewModel,
        incomingIntent: Intent
    ) {
        val context = LocalContext.current
        val mainViewModel = LocalMainViewModel.current

        val mediaStoreData =
            multiAlbumViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
        val groupedMedia = remember { mutableStateOf(mediaStoreData.value) }

        val tabList by mainViewModel.settings.DefaultTabs.getTabList()
            .collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)

        val showDialog = rememberSaveable { mutableStateOf(false) }
        val scrollBehaviour = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
            exitDirection = FloatingToolbarExitDirection.Bottom
        )

        Scaffold(
            topBar = {
                TopBar(
                    showDialog = showDialog,
                    selectedItemsList = selectedItemsList,
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

            val exitImmediately by mainViewModel.settings.Behaviour.getExitImmediately().collectAsStateWithLifecycle(initialValue = false)
            val mainPage by mainViewModel.settings.DefaultTabs.getDefaultTab().collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)
            val navController = LocalNavController.current

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
                            padding.calculateBottomPadding()
                        )
                ) {
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
                        Log.d(TAG, "Tab needed is $stateValue")
                        if (stateValue in tabList || stateValue == DefaultTabs.TabTypes.secure) {
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
                                            sortMode = multiAlbumViewModel.sortMode
                                        )
                                    }

                                    LaunchedEffect(Unit) {
                                        selectedItemsList.clear()
                                    }

                                    var hasFiles by remember { mutableStateOf(true) }
                                    LaunchedEffect(mediaStoreData.value.size) {
                                        withContext(Dispatchers.IO) {
                                            hasFiles = stateValue.albumPaths.any { path ->
                                                Path(path).checkHasFiles(
                                                    basePath = path.toBasePath()
                                                ) == true
                                            }
                                        }

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
                                        isMediaPicker = true,
                                        hasFiles = hasFiles
                                    )
                                }

                                stateValue == DefaultTabs.TabTypes.photos -> {
                                    val mainPhotosPaths by mainViewModel.mainPhotosAlbums.collectAsStateWithLifecycle()

                                    LaunchedEffect(mainPhotosPaths) {
                                        Log.d(TAG, "Main photos paths are $mainPhotosPaths")
                                        if (mainPhotosPaths.toSet() != multiAlbumViewModel.albumInfo.paths.toSet()) {
                                            multiAlbumViewModel.reinitDataSource(
                                                context = context,
                                                album = AlbumInfo(
                                                    id = stateValue.id,
                                                    name = stateValue.name,
                                                    paths = mainPhotosPaths,
                                                    isCustomAlbum = false
                                                ),
                                                sortMode = multiAlbumViewModel.sortMode
                                            )
                                        }
                                    }

                                    LaunchedEffect(Unit) {
                                        selectedItemsList.clear()
                                    }

                                    var hasFiles by remember { mutableStateOf(true) }
                                    LaunchedEffect(mediaStoreData.value) {
                                        if (mainPhotosPaths.isNotEmpty()) {
                                            withContext(Dispatchers.IO) {
                                                hasFiles = mainPhotosPaths.any { path ->
                                                    Path(path).checkHasFiles(
                                                        basePath = path.toBasePath()
                                                    ) == true
                                                }
                                            }
                                        }

                                        groupedMedia.value = mediaStoreData.value
                                    }

                                    PhotoGrid(
                                        groupedMedia = groupedMedia,
                                        albumInfo = multiAlbumViewModel.albumInfo,
                                        viewProperties = ViewProperties.Album,
                                        selectedItemsList = selectedItemsList,
                                        isMediaPicker = true,
                                        hasFiles = hasFiles
                                    )
                                }

                                stateValue == DefaultTabs.TabTypes.albums -> {
                                    AlbumsGridView(
                                        currentView = currentView,
                                        isMediaPicker = true
                                    )
                                }

                                stateValue == DefaultTabs.TabTypes.search -> {
                                    LaunchedEffect(Unit) {
                                        selectedItemsList.clear()

                                        if (multiAlbumViewModel.albumInfo.paths.isNotEmpty()) {
                                            multiAlbumViewModel.reinitDataSource(
                                                context = context,
                                                album = AlbumInfo.createPathOnlyAlbum(emptyList())
                                            )
                                        }
                                    }

                                    SearchPage(selectedItemsList)
                                }

                                stateValue == DefaultTabs.TabTypes.favourites -> {
                                    val appDatabase = LocalAppDatabase.current
                                    val favouritesViewModel: FavouritesViewModel = viewModel(
                                        factory = FavouritesViewModelFactory(appDatabase)
                                    )

                                    val mediaStoreData by favouritesViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

                                    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                                    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

                                    val groupedMedia = remember {
                                        mutableStateOf(
                                            groupPhotosBy(
                                                mediaStoreData,
                                                if (sortMode == MediaItemSortMode.Disabled) sortMode else MediaItemSortMode.LastModified,
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
                                                    if (sortMode == MediaItemSortMode.Disabled) sortMode else MediaItemSortMode.LastModified,
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
                                    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                                    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

                                    val trashViewModel: TrashViewModel = viewModel(
                                        factory = TrashViewModelFactory(
                                            context = context,
                                            sortMode = sortMode,
                                            displayDateFormat = displayDateFormat
                                        )
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

                val navController = LocalNavController.current
                AnimatedContent(
                    targetState = selectedItemsList.isNotEmpty() && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
                    transitionSpec = {
                        getAppBarContentTransition(selectedItemsList.isNotEmpty())
                    },
                    label = "MainBottomBarAnimatedContentView",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) { state ->
                    if (!state) {
                        MainAppBottomBar(
                            currentView = currentView,
                            tabs = tabs.fastFilter { it != DefaultTabs.TabTypes.secure },
                            selectedItemsList = selectedItemsList,
                            scrollBehaviour = scrollBehaviour,
                            groupedMedia = groupedMedia
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
            currentView = currentView,
            isFromMediaPicker = true
        )
    }
}

@Composable
fun MediaPickerConfirmButton(
    incomingIntent: Intent,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
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
    ) {
        Button(
            onClick = {
                if (incomingIntent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                    || incomingIntent.action == Intent.ACTION_OPEN_DOCUMENT
                ) {
                    val uris = selectedItemsList.filter { it.type != MediaType.Section }.map { it.uri }

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
                    val resultIntent = Intent().apply {
                        data = selectedItemsList.first { it.type != MediaType.Section }.uri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    Log.d(TAG, "Selected item is ${selectedItemsList.first { it.type != MediaType.Section }}")

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