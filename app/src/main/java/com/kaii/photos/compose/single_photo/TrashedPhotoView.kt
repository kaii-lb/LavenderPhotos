package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.single_view.SingleViewTopBar
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.compose.dialogs.TrashDeleteDialog
import com.kaii.photos.compose.modifiers.singlePhotoBottomBarProperties
import com.kaii.photos.compose.modifiers.singlePhotoProperties
import com.kaii.photos.compose.modifiers.singlePhotoTopBarProperties
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.scrolling.retainSinglePhotoScrollState
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.toFileOperationMetadata
import com.kaii.photos.models.TrashViewModel
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import com.kaii.photos.presentation.single_photos_views.rememberDismissSinglePhotoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SingleTrashedPhotoView(
    window: Window,
    index: Int,
    viewModel: TrashViewModel
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()
    val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()
    val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
    val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
    val useCache by viewModel.useCache.collectAsStateWithLifecycle()
    val useTapToNav by viewModel.useTapToNav.collectAsStateWithLifecycle()
    val mediaData by viewModel.exifData.collectAsStateWithLifecycle()

    SingleTrashedPhotoViewImpl(
        items = items,
        navController = LocalNavController.current,
        startIndex = index,
        window = window,
        useBlackBackground = { useBlackBackground },
        topBarDetailsFormat = topBarDetailsFormat,
        blurViews = { blurViews },
        useCache = { useCache },
        useTapToNav = { useTapToNav },
        mediaData = { mediaData },
        runAction = viewModel::runAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun SingleTrashedPhotoViewImpl(
    items: LazyPagingItems<PhotoLibraryUIModel>,
    navController: NavController,
    startIndex: Int,
    window: Window,
    useBlackBackground: () -> Boolean,
    topBarDetailsFormat: TopBarDetailsFormat,
    blurViews: () -> Boolean,
    useCache: () -> Boolean,
    useTapToNav: () -> Boolean,
    mediaData: () -> Result<Map<MediaData, String>, FileOperationError>,
    runAction: (action: FileOperationAction) -> Unit
) {
    var currentIndex by rememberSaveable(startIndex) {
        mutableIntStateOf(
            startIndex
        )
    }

    var mediaItem by remember { mutableStateOf(MediaStoreData.dummyItem) }
    LaunchedEffect(currentIndex, items, items.itemSnapshotList) {
        withContext(Dispatchers.IO) {
            mediaItem =
                if (currentIndex in 0..<items.itemCount && items.itemCount != 0) {
                    ((items[currentIndex] as? PhotoLibraryUIModel.MediaImpl))?.item ?: MediaStoreData.dummyItem
                } else {
                    MediaStoreData.dummyItem
                }
        }
    }

    val state = rememberPagerState(
        initialPage = startIndex
    ) {
        items.itemCount
    }

    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    TrashDeleteDialog(
        showDialog = showDialog,
        onDelete = {
            runAction(
                FileOperationAction.Delete(
                    files = listOf(
                        mediaItem.toFileOperationMetadata()
                    ),
                    album = AlbumType.PlaceHolder
                )
            )
        },
        onDismiss = {
            showDialog = false
        }
    )

    val scrollState = retainSinglePhotoScrollState(isOpenWithView = false)
    val appBarsVisible = remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val draggableState = rememberDismissSinglePhotoState {
        scrollState.privacyMode
    }

    val (firstFR, secondFR, thirdFR) = remember { FocusRequester.createRefs() }
    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = { mediaItem },
                visible = appBarsVisible.value,
                showInfoDialog = { showInfoDialog },
                privacyMode = { scrollState.privacyMode },
                isOpenWithDefaultView = false,
                topBarDetailsFormat = topBarDetailsFormat,
                showTags = false,
                expandInfoDialog = {
                    coroutineScope.launch {
                        showInfoDialog = true
                        delay(50.milliseconds)
                        sheetState.partialExpand()
                    }
                },
                modifier = Modifier
                    .singlePhotoTopBarProperties(
                        draggableState = draggableState,
                        firstFR = firstFR,
                        secondFR = secondFR
                    )
            )
        },
        bottomBar = {
            BottomBar(
                visible = appBarsVisible.value,
                item = { mediaItem },
                privacyMode = scrollState.privacyMode,
                showDeleteDialog = {
                    showDialog = true
                },
                runAction = runAction,
                modifier = Modifier
                    .singlePhotoBottomBarProperties(
                        draggableState = draggableState,
                        secondFR = secondFR,
                        thirdFR = thirdFR
                    )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(if (useBlackBackground()) Color.Black else MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LaunchedEffect(state) {
                snapshotFlow { state.currentPage }.collect {
                    currentIndex = it
                }
            }

            LaunchedEffect(items.itemCount) {
                snapshotFlow { items.itemCount }.collectLatest {
                    delay(PhotoGridConstants.LOADING_TIME_SHORT.milliseconds)
                    if (items.itemCount == 0) launch(Dispatchers.Main) {
                        navController.popBackStack(Screens.MainPages.MainGrid.GridView::class, inclusive = false)
                    }
                }
            }

            if (showInfoDialog) {
                // use mediaItem as key since we need to refresh this when the date/name/wtv changes not just index
                LaunchedEffect(mediaItem) {
                    val item = items[currentIndex] as PhotoLibraryUIModel.MediaImpl

                    runAction(
                        FileOperationAction.LoadExifData(
                            file = item.item.toFileOperationMetadata()
                        )
                    )
                }

                SinglePhotoInfoDialog(
                    mediaItem = { mediaItem },
                    mediaData = mediaData,
                    sheetState = sheetState,
                    showMoveCopyOptions = false,
                    privacyMode = { scrollState.privacyMode },
                    album = { AlbumType.PlaceHolder },
                    dismiss = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showInfoDialog = false
                        }
                    },
                    togglePrivacyMode = scrollState::togglePrivacyMode,
                    runAction = runAction
                )
            }
        }

        HorizontalImageList(
            items = items,
            state = state,
            window = window,
            appBarsVisible = appBarsVisible,
            scrollState = scrollState,
            blurViews = blurViews,
            useBlackBackground = useBlackBackground,
            useCache = useCache,
            useTapToNav = useTapToNav,
            swipeDownProgress = { draggableState.progress },
            modifier = Modifier
                .singlePhotoProperties(
                    state = state,
                    draggableState = draggableState,
                    firstFR = firstFR,
                    secondFR = secondFR,
                    thirdFR = thirdFR,
                    isVideo = { mediaItem.type == MediaType.Video }
                )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    item: () -> MediaStoreData,
    privacyMode: Boolean,
    modifier: Modifier = Modifier,
    showDeleteDialog: () -> Unit,
    runAction: (action: FileOperationAction) -> Unit
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(4.dp, 0.dp)
            .wrapContentHeight()
            .fillMaxWidth(1f),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = scaleOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                targetScale = 0.2f
            ) + fadeOut()
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                floatingActionButton = {
                    FilledIconButton(
                        onClick = {
                            val item = item()
                            runAction(
                                FileOperationAction.Share(
                                    files = listOf(
                                        item.toFileOperationMetadata()
                                    )
                                )
                            )
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = vibrantFloatingToolbarColors().fabContentColor,
                            containerColor = vibrantFloatingToolbarColors().fabContainerColor,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shapes = IconButtonDefaults.shapes(
                            shape = IconButtonDefaults.mediumSquareShape,
                            pressedShape = IconButtonDefaults.smallPressedShape
                        ),
                        enabled = !privacyMode,
                        modifier = Modifier
                            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = "share this media"
                        )
                    }
                },
                modifier = Modifier
                    .windowInsetsPadding(windowInsets)
            ) {
                val permissionManager = rememberFilePermissionManager(
                    onGranted = {
                        runAction(
                            FileOperationAction.Trash(
                                files = listOf(
                                    item().toFileOperationMetadata()
                                ),
                                isTrashed = false,
                                album = AlbumType.PlaceHolder
                            )
                        )
                    }
                )

                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable(enabled = !privacyMode) {
                            permissionManager.get(uris = listOf(item().uri.toUri()))
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.unlock),
                        contentDescription = "Restore Image Button",
                        tint =
                            if (!privacyMode) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(8.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.media_restore),
                        fontSize = TextUnit(16f, TextUnitType.Sp),
                        textAlign = TextAlign.Center,
                        color =
                            if (!privacyMode) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth(1f)
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))
                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(3.dp))

                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable(enabled = !privacyMode) {
                            showDeleteDialog()
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.trash),
                        contentDescription = "Permanently Delete Image Button",
                        tint =
                            if (!privacyMode) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(8.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.media_delete),
                        fontSize = TextUnit(16f, TextUnitType.Sp),
                        textAlign = TextAlign.Center,
                        color =
                            if (!privacyMode) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth(1f)
                    )
                }
            }
        }
    }
}
