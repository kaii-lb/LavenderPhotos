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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.SingleViewTopBar
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.compose.dialogs.TrashDeleteDialog
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.scrolling.rememberSinglePhotoScrollState
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.permissions.files.rememberFilePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SingleTrashedPhotoView(
    window: Window,
    index: Int,
    viewModel: TrashViewModel
) {
    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    SingleTrashedPhotoViewImpl(
        items = items,
        startIndex = index,
        window = window
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun SingleTrashedPhotoViewImpl(
    items: LazyPagingItems<PhotoLibraryUIModel>,
    startIndex: Int,
    window: Window
) {
    var currentIndex by rememberSaveable(startIndex) {
        mutableIntStateOf(
            startIndex
        )
    }

    var mediaItem by remember { mutableStateOf(MediaStoreData.dummyItem) }
    LaunchedEffect(currentIndex) {
        withContext(Dispatchers.IO) {
            mediaItem =
                if (currentIndex in 0..<items.itemCount && items.itemCount != 0) {
                    ((items[currentIndex] as? PhotoLibraryUIModel.MediaImpl))?.item ?: MediaStoreData.dummyItem
                } else {
                    MediaStoreData.dummyItem
                }
        }
    }

    val runPermaDeleteAction = remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(runPermaDeleteAction.value) {
        if (runPermaDeleteAction.value) withContext(Dispatchers.IO) {
            permanentlyDeletePhotoList(context, listOf(mediaItem.uri.toUri()))

            runPermaDeleteAction.value = false
        }
    }

    val state = rememberPagerState(
        initialPage = startIndex
    ) {
        items.itemCount
    }

    val coroutineScope = rememberCoroutineScope()
    fun onMoveMedia() {
        coroutineScope.launch {
            state.animateScrollToPage(
                page = (currentIndex + 1) % items.itemCount,
                animationSpec = AnimationConstants.expressiveTween(
                    durationMillis = AnimationConstants.DURATION
                )
            )
            delay(AnimationConstants.DURATION_SHORT.toLong())
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    TrashDeleteDialog(
        showDialog = showDialog,
        onDelete = {
            onMoveMedia()
            runPermaDeleteAction.value = true
        },
        onDismiss = {
            showDialog = false
        }
    )

    val scrollState = rememberSinglePhotoScrollState(isOpenWithView = false)
    val appBarsVisible = remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = mediaItem,
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
                privacyMode = scrollState.privacyMode,
                isOpenWithDefaultView = false,
                expandInfoDialog = {
                    showInfoDialog = true
                }
            )
        },
        bottomBar = {
            BottomBar(
                visible = appBarsVisible.value,
                item = mediaItem,
                privacyMode = scrollState.privacyMode,
                showDeleteDialog = {
                    showDialog = true
                },
                onMoveMedia = {
                    onMoveMedia()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        val useBlackBackground by LocalMainViewModel.current.useBlackViewBackgroundColor.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LaunchedEffect(state) {
                snapshotFlow { state.currentPage }.collect {
                    currentIndex = it
                }
            }

            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )
            if (showInfoDialog) {
                SinglePhotoInfoDialog(
                    currentMediaItem = mediaItem,
                    sheetState = sheetState,
                    showMoveCopyOptions = false,
                    isTouchLocked = scrollState.privacyMode,
                    dismiss = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showInfoDialog = false
                        }
                    },
                    onMoveMedia = {
                        onMoveMedia()
                    },
                    togglePrivacyMode = scrollState::togglePrivacyMode
                )
            }

            HorizontalImageList(
                items = items,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible,
                scrollState = scrollState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    item: MediaStoreData,
    privacyMode: Boolean,
    showDeleteDialog: () -> Unit,
    onMoveMedia: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
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
                            shareImage(
                                uri = item.uri.toUri(),
                                context = context,
                                mimeType = item.mimeType
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
                val mainViewModel = LocalMainViewModel.current
                val permissionManager = rememberFilePermissionManager(
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            onMoveMedia()

                            setTrashedOnPhotoList(
                                context = context,
                                list = listOf(item.uri.toUri()),
                                trashed = false
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable(enabled = !privacyMode) {
                            permissionManager.get(uris = listOf(item.uri.toUri()))
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
