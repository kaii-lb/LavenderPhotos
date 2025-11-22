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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.SingleViewTopBar
import com.kaii.photos.compose.dialogs.SinglePhotoInfoDialog
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.trash_bin.TrashViewModel
import com.kaii.photos.models.trash_bin.TrashViewModelFactory
import kotlinx.coroutines.Dispatchers

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SingleTrashedPhotoView(
    window: Window,
    mediaItemId: Long,
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
    val sortMode by mainViewModel.sortMode.collectAsStateWithLifecycle()

    val trashViewModel: TrashViewModel = viewModel(
        factory = TrashViewModelFactory(
            context = context,
            sortMode = sortMode,
            displayDateFormat = displayDateFormat
        )
    )
    val holderGroupedMedia by trashViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

    if (holderGroupedMedia.isEmpty()) return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    LaunchedEffect(holderGroupedMedia) {
        groupedMedia.value =
            holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
    }

    val appBarsVisible = remember { mutableStateOf(true) }
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
        initialPage = currentMediaItemIndex.coerceAtLeast(0)
    ) {
        groupedMedia.value.size
    }

    LaunchedEffect(key1 = state.currentPage) {
        currentMediaItemIndex = state.currentPage
    }

    val resources = LocalResources.current
    val currentMediaItem by remember {
        derivedStateOf {
            val index = state.layoutInfo.visiblePagesInfo.firstOrNull()?.index ?: 0
            if (index != groupedMedia.value.size) {
                groupedMedia.value[index]
            } else {
                MediaStoreData(
                    displayName = resources.getString(R.string.media_broken)
                )
            }
        }
    }

    val showDialog = remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val runPermaDeleteAction = remember { mutableStateOf(false) }

    LaunchedEffect(runPermaDeleteAction.value) {
        if (runPermaDeleteAction.value) {
            permanentlyDeletePhotoList(context, listOf(currentMediaItem.uri))

            runPermaDeleteAction.value = false
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false

                        runPermaDeleteAction.value = true
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.media_delete),
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.media_delete_permanently_confirm),
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.media_cancel),
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }

    Scaffold(
        topBar = {
            SingleViewTopBar(
                mediaItem = currentMediaItem,
                visible = appBarsVisible.value,
                showInfoDialog = showInfoDialog,
                expandInfoDialog = {
                    showInfoDialog = true
                }
            )
        },
        bottomBar = {
            BottomBar(
                appBarsVisible.value,
                currentMediaItem,
                showDialog
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
            HorizontalImageList(
                currentMediaItem = currentMediaItem,
                groupedMedia = groupedMedia.value,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible
            )
        }

        if (showInfoDialog) {
            SinglePhotoInfoDialog(
                currentMediaItem = currentMediaItem,
                showMoveCopyOptions = false
            ) {
                showInfoDialog = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    item: MediaStoreData,
    showDialog: MutableState<Boolean>
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
                            showDialog.value = true
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
