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
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.SingleViewTopBar
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.compose.dialogs.TrashDeleteDialog
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.mapSync
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.trash_bin.TrashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SingleTrashedPhotoView(
    window: Window,
    mediaItemId: Long,
    viewModel: TrashViewModel
) {
    val mediaStoreData = viewModel.mediaFlow.mapSync {
        it.filter { item ->
            item.type != MediaType.Section
        }
    }.collectAsStateWithLifecycle()

    val startIndex = remember(mediaStoreData.value.isEmpty()) {
        mediaStoreData.value.indexOfFirst { item ->
            item.id == mediaItemId
        }
    }

    if (mediaStoreData.value.isNotEmpty()) {
        SingleTrashedPhotoViewImpl(
            mediaStoreData = mediaStoreData,
            startIndex = startIndex,
            window = window
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun SingleTrashedPhotoViewImpl(
    mediaStoreData: State<List<MediaStoreData>>,
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
                if (currentIndex in 0..mediaStoreData.value.size && mediaStoreData.value.isNotEmpty()) {
                    mediaStoreData.value[currentIndex]
                } else {
                    MediaStoreData.dummyItem
                }
        }
    }

    val runPermaDeleteAction = remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(runPermaDeleteAction.value) {
        if (runPermaDeleteAction.value) withContext(Dispatchers.IO) {
            permanentlyDeletePhotoList(context, listOf(mediaItem.uri))

            runPermaDeleteAction.value = false
        }
    }

    val state = rememberPagerState(
        initialPage = currentIndex.coerceIn(0, mediaStoreData.value.size)
    ) {
        mediaStoreData.value.size
    }

    val coroutineScope = rememberCoroutineScope()
    fun onMoveMedia() {
        coroutineScope.launch {
            state.animateScrollToPage(
                page = (currentIndex + 1) % mediaStoreData.value.size,
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

    val appBarsVisible = remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = mediaItem,
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
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
                    dismiss = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showInfoDialog = false
                        }
                    },
                    onMoveMedia = {
                        onMoveMedia()
                    }
                )
            }

            HorizontalImageList(
                groupedMedia = mediaStoreData.value,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    item: MediaStoreData,
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
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = {
                            shareImage(
                                uri = item.uri,
                                context = context,
                                mimeType = item.mimeType
                            )
                        }
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
                val runRestoreAction = remember { mutableStateOf(false) }
                val mainViewModel = LocalMainViewModel.current
                val applicationDatabase = LocalAppDatabase.current

                GetPermissionAndRun(
                    uris = listOf(item.uri),
                    shouldRun = runRestoreAction,
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            onMoveMedia()

                            setTrashedOnPhotoList(
                                context = context,
                                list = listOf(item),
                                trashed = false,
                                appDatabase = applicationDatabase
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable {
                            runRestoreAction.value = true
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.unlock),
                        contentDescription = "Restore Image Button",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        .clickable {
                            showDeleteDialog()
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.trash),
                        contentDescription = "Permanently Delete Image Button",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth(1f)
                    )
                }
            }
        }
    }
}
