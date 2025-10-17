package com.kaii.photos.compose.grids

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SingleAlbumViewBottomBar
import com.kaii.photos.compose.app_bars.SingleAlbumViewTopBar
import com.kaii.photos.compose.dialogs.SingleAlbumDialog
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.custom_album.CustomAlbumViewModel
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.io.path.Path

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    viewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    val allAlbums by mainViewModel.allAvailableAlbums.collectAsStateWithLifecycle()
    val normalAlbums by mainViewModel.settings.AlbumsList.getNormalAlbums().collectAsStateWithLifecycle(initialValue = emptyList())

    if (allAlbums.isEmpty()) return

    var lastDynamicAlbum by remember {
        mutableStateOf(
            albumInfo
        )
    }
    val dynamicAlbum by remember {
        derivedStateOf {
            (normalAlbums.firstOrNull { it.id == albumInfo.id } ?: allAlbums.firstOrNull { it.id == albumInfo.id } ?: lastDynamicAlbum).let {
                lastDynamicAlbum = it
                lastDynamicAlbum
            }
        }
    }

    Log.d("SINGLE_ALBUM_VIEW", "Dynamic album ${dynamicAlbum.name} is pinned? ${dynamicAlbum.isPinned}")

    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        viewModel.cancelMediaFlow()
        navController.popBackStack()
    }

    val mediaStoreData by viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(mediaStoreData) }

    LaunchedEffect(mediaStoreData) {
        if (mediaStoreData.isEmpty()) {
            delay(PhotoGridConstants.UPDATE_TIME)
            groupedMedia.value = mediaStoreData
        } else {
            groupedMedia.value = mediaStoreData
        }
    }

    SingleAlbumViewCommon(
        groupedMedia = groupedMedia,
        albumInfo = dynamicAlbum,
        selectedItemsList = selectedItemsList,
        navController = navController,
        incomingIntent = incomingIntent
    ) {
        if (viewModel.albumInfo.paths.toSet() != dynamicAlbum.paths.toSet()) {
            viewModel.reinitDataSource(
                context = context,
                album = dynamicAlbum
            )
        }
    }
}

@Composable
fun SingleAlbumView(
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    customViewModel: CustomAlbumViewModel,
    multiViewModel: MultiAlbumViewModel,
    incomingIntent: Intent? = null
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    val allAlbums by mainViewModel.allAvailableAlbums.collectAsStateWithLifecycle()
    val normalAlbums by mainViewModel.settings.AlbumsList.getNormalAlbums().collectAsStateWithLifecycle(initialValue = emptyList())

    if (allAlbums.isEmpty()) return

    var lastDynamicAlbum by remember {
        mutableStateOf(
            albumInfo
        )
    }
    val dynamicAlbum by remember {
        derivedStateOf {
            (normalAlbums.firstOrNull { it.id == albumInfo.id } ?: allAlbums.firstOrNull { it.id == albumInfo.id } ?: lastDynamicAlbum).let {
                lastDynamicAlbum = it
                lastDynamicAlbum
            }
        }
    }

    BackHandler(
        enabled = selectedItemsList.isEmpty()
    ) {
        customViewModel.cancelMediaFlow()
        multiViewModel.cancelMediaFlow()

        navController.popBackStack()
    }

    val customMediaStoreData by customViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val multiMediaStoreData by multiViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    val groupedMedia = remember { mutableStateOf(customMediaStoreData + multiMediaStoreData) }

    LaunchedEffect(customMediaStoreData, multiMediaStoreData) {
        val mixed = (customMediaStoreData + multiMediaStoreData).distinctBy { it.uri }

        if (mixed.isEmpty()) {
            delay(PhotoGridConstants.UPDATE_TIME)
            groupedMedia.value = mixed
        } else {
            groupedMedia.value = mixed
        }
    }

    SingleAlbumViewCommon(
        groupedMedia = groupedMedia,
        albumInfo = dynamicAlbum,
        selectedItemsList = selectedItemsList,
        navController = navController,
        incomingIntent = incomingIntent
    ) {
        if (customViewModel.albumInfo != dynamicAlbum) {
            customViewModel.reinitDataSource(
                context = context,
                album = dynamicAlbum
            )
        }

        if (multiViewModel.albumInfo != dynamicAlbum) {
            multiViewModel.reinitDataSource(
                context = context,
                album = dynamicAlbum
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleAlbumViewCommon(
    groupedMedia: MutableState<List<MediaStoreData>>,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    navController: NavHostController,
    incomingIntent: Intent?,
    reinitDataSource: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    LaunchedEffect(albumInfo) {
        reinitDataSource()
    }

    var hasFiles by remember { mutableStateOf(true) }
    LaunchedEffect(groupedMedia.value.lastOrNull()) {
        withContext(Dispatchers.IO) {
            hasFiles = if (!albumInfo.isCustomAlbum) {
                var result: Boolean? = null

                albumInfo.paths.any { path ->
                    val basePath = path.toBasePath()

                    result = Path(path).checkHasFiles(basePath = basePath)
                    result == true
                }

                result == true
            } else groupedMedia.value.isNotEmpty()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                albumInfo = albumInfo,
                media = groupedMedia.value,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog,
                isMediaPicker = incomingIntent != null
            ) {
                navController.popBackStack()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedItemsList.isNotEmpty(),
                enter = fadeIn() + slideInHorizontally(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = fadeOut() + slideOutHorizontally(
                    animationSpec = AnimationConstants.expressiveTween()
                )
            ) {
                SingleAlbumViewBottomBar(
                    albumInfo = albumInfo,
                    selectedItemsList = selectedItemsList,
                    incomingIntent = incomingIntent
                )
            }
        }
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
                .fillMaxSize(1f)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albumInfo = albumInfo,
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.Album,
                isMediaPicker = incomingIntent != null,
                hasFiles = hasFiles
            )

            SingleAlbumDialog(
                showDialog = showDialog,
                album = albumInfo,
                navController = navController,
                selectedItemsList = selectedItemsList,
                itemCount = groupedMedia.value.filter { it.type != MediaType.Section }.size
            )
        }
    }
}


