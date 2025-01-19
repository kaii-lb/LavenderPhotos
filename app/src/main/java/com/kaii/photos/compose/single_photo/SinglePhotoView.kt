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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.ConfirmationDialogWithBody
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.compose.SinglePhotoInfoDialog
import com.kaii.photos.compose.setBarVisibility
import com.kaii.photos.helpers.EditingScreen
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.moveImageToLockedFolder
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.favourites_grid.FavouritesViewModel
import com.kaii.photos.models.favourites_grid.FavouritesViewModelFactory
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import kotlinx.coroutines.Dispatchers

//private const val TAG = "SINGLE_PHOTO_VIEW"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoView(
    navController: NavHostController,
    window: Window,
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
) {
    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return
	val path = mainViewModel.singlePhotoPath.collectAsState(initial = null).value

	val fastLoadedGroupedMedia = mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

    val groupedMedia = remember {
        mutableStateOf(
            fastLoadedGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

	var galleryViewModel: GalleryViewModel? = null

	if (path != null) {
		galleryViewModel = viewModel(
		    factory = GalleryViewModelFactory(
		        LocalContext.current,
		        path,
		        MediaItemSortMode.DateTaken
		    )
		)

		val holderGroupedMedia by galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

		LaunchedEffect(holderGroupedMedia) {
	    	if (holderGroupedMedia.isNotEmpty()) {
		        groupedMedia.value =
		            holderGroupedMedia.filter { item ->
		                item.type != MediaType.Section
		            }
	    	}
	    }
	}

    var currentMediaItemIndex by rememberSaveable {
        mutableIntStateOf(
            groupedMedia.value.indexOf(
                mediaItem
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

    BackHandler (
        enabled = !showInfoDialog.value
    ) {
        galleryViewModel?.cancelMediaFlow()
        navController.popBackStack()
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
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
                	galleryViewModel?.cancelMediaFlow()
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            BottomBar(
                appBarsVisible.value,
                currentMediaItem.value,
                state = state,
                groupedMedia = groupedMedia,
                showEditingView = {
                    setBarVisibility(
                        visible = true,
                        window = window
                    ) {
                        appBarsVisible.value = it
                    }

                    navController.navigate(
                    	EditingScreen(
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
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) { _ ->
        SinglePhotoInfoDialog(
            showDialog = showInfoDialog,
            currentMediaItem = currentMediaItem.value,
            groupedMedia = groupedMedia,
            showMoveCopyOptions = true,
            moveCopyInsetsPadding = WindowInsets.statusBars
        )

        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(CustomMaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalImageList(
                navController,
                currentMediaItem.value,
                groupedMedia.value,
                state,
                scale,
                rotation,
                offset,
                window,
                appBarsVisible
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
	val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
    	isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val color = if (isLandscape)
        CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    else
        CustomMaterialTheme.colorScheme.surfaceContainer

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
                        tint = CustomMaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            },
            title = {
                val mediaTitle = mediaItem.displayName ?: mediaItem.type.name

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = mediaTitle,
                    fontSize = TextUnit(16f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(if (isLandscape) 300.dp else 160.dp)
                )
            },
            actions = {
                val isSelected by favouritesViewModel.isInFavourites(mediaItem.id).collectAsState()

                IconButton(
                    onClick = {
                        vibratorManager.vibrateShort()

                        if (!isSelected) {
                            favouritesViewModel.addToFavourites(mediaItem)
                        } else {
                            favouritesViewModel.removeFromFavourites(mediaItem.id)
                            removeIfInFavGrid()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) R.drawable.favourite_filled else R.drawable.favourite),
                        contentDescription = "favorite this media item",
                        tint = if (isSelected) CustomMaterialTheme.colorScheme.primary else CustomMaterialTheme.colorScheme.onBackground,
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
                        tint = CustomMaterialTheme.colorScheme.onBackground,
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
    state: PagerState,
    groupedMedia: MutableState<List<MediaStoreData>>,
    showEditingView: () -> Unit,
    onZeroItemsLeft: () -> Unit
) {
	val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
    	isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val color = if (isLandscape)
        CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    else
        CustomMaterialTheme.colorScheme.surfaceContainer

    val coroutineScope = rememberCoroutineScope()

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
            contentColor = CustomMaterialTheme.colorScheme.onBackground,
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
                    ConfirmationDialogWithBody(
                    	showDialog = showNotImplementedDialog,
                    	dialogTitle = "Unimplemented",
                    	dialogBody = "Editing videos has not been implemented yet.",
                    	confirmButtonLabel = "Okay",
                    	showCancelButton = false,
                    	action = {}
                    )

                    BottomAppBarItem(
                        text = "Edit",
                        iconResId = R.drawable.paintbrush,
                        cornerRadius = 32.dp,
                        action = if (currentItem.type == MediaType.Image) showEditingView else { { showNotImplementedDialog.value = true } }
                    )

                    val showDeleteDialog = remember { mutableStateOf(false) }
                    val runTrashAction = remember { mutableStateOf(false) }

                    GetPermissionAndRun(
                        uris = listOf(currentItem.uri),
                        shouldRun = runTrashAction,
                        onGranted = {
                            setTrashedOnPhotoList(
                                context,
                                listOf(currentItem.uri),
                                true
                            )

                            sortOutMediaMods(
                                currentItem,
                                groupedMedia,
                                coroutineScope,
                                state
                            ) {
                                onZeroItemsLeft()
                            }
                        }
                    )

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
                            showDeleteDialog.value = true
                        }
                    )

                    val showMoveToSecureFolderDialog = remember { mutableStateOf(false) }
                    val moveToSecureFolder = remember { mutableStateOf(false) }

                    GetPermissionAndRun(
                        uris = listOf(currentItem.uri),
                        shouldRun = moveToSecureFolder,
                        onGranted = {
                            moveImageToLockedFolder(
                                currentItem,
                                context
                            )

                            sortOutMediaMods(
                                currentItem,
                                groupedMedia,
                                coroutineScope,
                                state
                            ) {
                                onZeroItemsLeft()
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
                                moveToSecureFolder.value = true
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


