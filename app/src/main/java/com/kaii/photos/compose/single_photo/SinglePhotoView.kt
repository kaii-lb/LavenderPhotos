package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kaii.photos.BuildConfig
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.BottomAppBarItem
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.rememberDeviceOrientation
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.multi_album.MultiAlbumViewModel
import kotlinx.coroutines.Dispatchers

// private const val TAG = "SINGLE_PHOTO_VIEW"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoView(
    navController: NavHostController,
    window: Window,
    viewModel: MultiAlbumViewModel,
    mediaItemId: Long,
    loadsFromMainViewModel: Boolean
) {
    val holderGroupedMedia =
        if (!loadsFromMainViewModel) {
            viewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)
        } else {
            mainViewModel.groupedMedia.collectAsStateWithLifecycle(initialValue = null)
        }

    if (holderGroupedMedia.value == null) return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.value!!.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    LaunchedEffect(holderGroupedMedia.value) {
        groupedMedia.value =
            holderGroupedMedia.value!!.filter { item ->
                item.type != MediaType.Section
            }
    }

    var currentMediaItemIndex by rememberSaveable {
        mutableIntStateOf(
            groupedMedia.value.indexOf(
                groupedMedia.value.first {
                    it.id == mediaItemId
                }
            )
        )
    }

    val state = rememberPagerState(
        initialPage = currentMediaItemIndex
            .coerceIn(
                0,
                (groupedMedia.value.size - 1)
                    .coerceAtLeast(0)
            )
    ) {
        groupedMedia.value.size
    }

    LaunchedEffect(key1 = state.currentPage) {
        currentMediaItemIndex = state.currentPage
    }

    val appBarsVisible = remember { mutableStateOf(true) }
    val currentMediaItem = remember {
        derivedStateOf {
            val index = state.layoutInfo.visiblePagesInfo.firstOrNull()?.index ?: 0
            if (index < groupedMedia.value.size) {
                groupedMedia.value[index]
            } else {
                MediaStoreData(
                    displayName = "Broken Media"
                )
            }
        }
    }

    val showInfoDialog = remember { mutableStateOf(false) }

    BackHandler(
        enabled = !showInfoDialog.value
    ) {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            val coroutineScope = rememberCoroutineScope()

            TopBar(
                mediaItem = currentMediaItem.value,
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
                removeIfInFavGrid = {
                    if (navController.previousBackStackEntry?.destination?.route == MultiScreenViewType.FavouritesGridView.name) {
                        sortOutMediaMods(
                            currentMediaItem.value,
                            groupedMedia,
                            coroutineScope,
                            state
                        ) {
                            navController.popBackStack()
                        }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            BottomBar(
                visible = appBarsVisible.value,
                currentItem = currentMediaItem.value,
                groupedMedia = groupedMedia,
                loadsFromMainViewModel = loadsFromMainViewModel,
                state = state,
                showEditingView = {
                    setBarVisibility(
                        visible = true,
                        window = window
                    ) {
                        appBarsVisible.value = it
                    }

                    navController.navigate(
                        Screens.EditingScreen(
                            absolutePath = currentMediaItem.value.absolutePath,
                            uri = currentMediaItem.value.uri.toString(),
                            dateTaken = currentMediaItem.value.dateTaken
                        )
                    )
                },
                onZeroItemsLeft = {
                    navController.popBackStack()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        SinglePhotoInfoDialog(
            showDialog = showInfoDialog,
            currentMediaItem = currentMediaItem.value,
            groupedMedia = groupedMedia,
            loadsFromMainViewModel = loadsFromMainViewModel,
            showMoveCopyOptions = true,
            moveCopyInsetsPadding = WindowInsets.statusBars
        )

        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalImageList(
                currentMediaItem = currentMediaItem.value,
                groupedMedia = groupedMedia.value,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    mediaItem: MediaStoreData,
    visible: Boolean,
    showInfoDialog: MutableState<Boolean>,
    removeIfInFavGrid: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val color = if (isLandscape)
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceContainer

    val vibratorManager = rememberVibratorManager()

    val favouritesViewModel: FavouritesViewModel = viewModel(
        factory = FavouritesViewModelFactory()
    )

    AnimatedVisibility(
        visible = visible,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> -width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 400
            )
        ) { width -> -width } + fadeOut(),
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = color
            ),
            navigationIcon = {
                IconButton(
                    onClick = { onBackClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "Go back to previous page",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            },
            title = {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = mediaItem.displayName,
                    fontSize = TextUnit(16f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(if (isLandscape) 300.dp else 160.dp)
                )
            },
            actions = {
                val isSelected by favouritesViewModel.isInFavourites(mediaItem.id).collectAsStateWithLifecycle()

                IconButton(
                    onClick = {
                        vibratorManager.vibrateShort()

                        if (!isSelected) {
                            favouritesViewModel.addToFavourites(mediaItem, context)
                        } else {
                            favouritesViewModel.removeFromFavourites(mediaItem.id)
                            removeIfInFavGrid()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) R.drawable.favourite_filled else R.drawable.favourite),
                        contentDescription = "favorite this media item",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(0.dp, 1.dp, 0.dp, 0.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showInfoDialog.value = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_options),
                        contentDescription = "show more options",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomBar(
    visible: Boolean,
    currentItem: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    loadsFromMainViewModel: Boolean,
    state: PagerState,
    showEditingView: () -> Unit,
    onZeroItemsLeft: () -> Unit
) {
    val isLandscape by rememberDeviceOrientation()

    val color = if (isLandscape)
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceContainer

    var showLoadingDialog by remember { mutableStateOf(false) }

    if (showLoadingDialog) {
        LoadingDialog(title = "Encrypting Files", body = "Please wait while the media is processed")
    }

    AnimatedVisibility(
        visible = visible,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 300
            )
        ) { width -> width } + fadeOut(),
    ) {
        val context = LocalContext.current
        BottomAppBar(
            containerColor = color,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentPadding = PaddingValues(0.dp),
            actions = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(12.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                    if (isLandscape)
                        Arrangement.spacedBy(
                            space = 48.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    else Arrangement.SpaceEvenly
                ) {
                    BottomAppBarItem(
                        text = "Share",
                        iconResId = R.drawable.share,
                        cornerRadius = 32.dp,
                        action = {
                            shareImage(currentItem.uri, context)
                        }
                    )

                    val showNotImplementedDialog = remember { mutableStateOf(false) }

                    if (showNotImplementedDialog.value) {
                        ExplanationDialog(
                            title = "Unimplemented",
                            explanation = "Editing videos has not been implemented yet as of version ${BuildConfig.VERSION_NAME} of Lavender Photos. This feature will be added as soon as possible, thank you for your patience.",
                            showDialog = showNotImplementedDialog
                        )
                    }

                    BottomAppBarItem(
                        text = "Edit",
                        iconResId = R.drawable.paintbrush,
                        cornerRadius = 32.dp,
                        action = if (currentItem.type == MediaType.Image) {
                            showEditingView
                        } else {
                            { showNotImplementedDialog.value = true }
                        }
                    )

                    val showDeleteDialog = remember { mutableStateOf(false) }
                    val runTrashAction = remember { mutableStateOf(false) }

                    println("CURRENT ITEM URI ${currentItem.uri}")

                    val coroutineScope = rememberCoroutineScope()
                    GetPermissionAndRun(
                        uris = listOf(currentItem.uri),
                        shouldRun = runTrashAction,
                        onGranted = {
                            mainViewModel.launch(Dispatchers.IO) {
                                setTrashedOnPhotoList(
                                    context,
                                    listOf(Pair(currentItem.uri, currentItem.absolutePath)),
                                    true
                                )

                                if (groupedMedia.value.isEmpty()) onZeroItemsLeft()

                                if (loadsFromMainViewModel) {
                                    sortOutMediaMods(
                                        currentItem,
                                        groupedMedia,
                                        coroutineScope,
                                        state
                                    ) {
                                        onZeroItemsLeft()
                                    }
                                }
                            }
                        }
                    )

                    val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)
                    BottomAppBarItem(
                        text = "Delete",
                        iconResId = R.drawable.trash,
                        cornerRadius = 32.dp,
                        dialogComposable = {
                            ConfirmationDialog(
                                showDialog = showDeleteDialog,
                                dialogTitle = "Delete this ${currentItem.type}?",
                                confirmButtonLabel = "Delete"
                            ) {
                                runTrashAction.value = true
                            }
                        },
                        action = {
                            if (confirmToDelete) showDeleteDialog.value = true
                            else runTrashAction.value = true
                        }
                    )

                    // TODO: maybe restructure this
                    val showMoveToSecureFolderDialog = remember { mutableStateOf(false) }
                    val moveToSecureFolder = remember { mutableStateOf(false) }
                    val tryGetDirPermission = remember { mutableStateOf(false) }

                    GetDirectoryPermissionAndRun(
                        absoluteDirPaths = listOf(groupedMedia.value.firstOrNull()?.absolutePath?.toRelativePath()?.getParentFromPath() ?: ""),
                        shouldRun = tryGetDirPermission,
                        onGranted = {
                        	moveToSecureFolder.value = true
                        	showLoadingDialog = true
                        },
                        onRejected = {}
                    )

                    GetPermissionAndRun(
                        uris = listOf(currentItem.uri),
                        shouldRun = moveToSecureFolder,
                        onGranted = {
                            mainViewModel.launch(Dispatchers.IO) {
                                moveImageToLockedFolder(
                                    listOf(currentItem),
                                    context
                                ) {
                                    if (groupedMedia.value.isEmpty()) onZeroItemsLeft()

                                    if (loadsFromMainViewModel) {
                                        sortOutMediaMods(
                                            currentItem,
                                            groupedMedia,
                                            coroutineScope,
                                            state
                                        ) {
                                            onZeroItemsLeft()
                                        }
                                    }

                                    showLoadingDialog = false
                                }
                            }
                        }
                    )

                    BottomAppBarItem(
                        text = "Secure",
                        iconResId = R.drawable.locked_folder,
                        cornerRadius = 32.dp,
                        dialogComposable = {
                            ConfirmationDialog(
                                showDialog = showMoveToSecureFolderDialog,
                                dialogTitle = "Move this ${currentItem.type} to Secure Folder?",
                                confirmButtonLabel = "Secure"
                            ) {
                                tryGetDirPermission.value = true

                                if (groupedMedia.value.isEmpty()) onZeroItemsLeft()
                            }
                        },
                        action = {
                            showMoveToSecureFolderDialog.value = true
                        }
                    )
                }
            }
        )
    }
}


