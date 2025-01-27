package com.kaii.photos.compose

import android.content.Intent
import android.net.Uri
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleSecuredImages
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** please only use dialogComposable for its intended purpose */
@Composable
fun BottomAppBarItem(
    text: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    buttonWidth: Dp = 64.dp,
    buttonHeight: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    textSize: Float = 14f,
    color: Color = Color.Transparent,
    contentColor: Color = CustomMaterialTheme.colorScheme.onBackground,
    showRipple: Boolean = true,
    cornerRadius: Dp = 1000.dp,
    action: (() -> Unit)? = null,
    dialogComposable: (@Composable () -> Unit)? = null
) {
    val clickModifier = if (action != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = if (!showRipple) null else LocalIndication.current
        ) {
            action()
        }
    } else {
        Modifier
    }

    if (dialogComposable != null) dialogComposable()

    Box(
        modifier = Modifier
            .width(buttonWidth)
            .height(buttonHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .then(clickModifier)
            .then(modifier),
    ) {
        Row(
            modifier = Modifier
                .height(iconSize + 8.dp)
                .width(iconSize * 2.25f)
                .clip(RoundedCornerShape(1000.dp))
                .align(Alignment.TopCenter)
                .background(color),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "button",
                tint = contentColor,
                modifier = Modifier
                    .size(iconSize)
            )
        }

        Text(
            text = text,
            fontSize = TextUnit(textSize, TextUnitType.Sp),
            color = CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SelectableBottomAppBarItem(
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    action: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                action()
            }
    ) {
        AnimatedVisibility(
            visible = selected,
            enter =
            expandHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                ),
                expandFrom = Alignment.CenterHorizontally
            ) + fadeIn(),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 150
                )
            ),
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(1000.dp))
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .width(64.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .align(Alignment.TopCenter)
                    .background(CustomMaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {}
        }

        Row(
            modifier = Modifier
                .height(32.dp)
                .width(58.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            label()
        }
    }
}

fun getAppBarContentTransition(slideLeft: Boolean) = run {
    if (slideLeft) {
        (slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            )
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) { width -> -width } + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            )
        )
    } else {
        (slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> -width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            )
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) { width -> width } + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            )
        )
    }
}

// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun MainAppTopBar(showDialog: MutableState<Boolean>) {
//     TopAppBar(
//         title = {
//             Row {
//                 Text(
//                     text = "Lavender ",
//                     fontWeight = FontWeight.Bold,
//                     fontSize = TextUnit(22f, TextUnitType.Sp)
//                 )
//                 Text(
//                     text = "Photos",
//                     fontWeight = FontWeight.Normal,
//                     fontSize = TextUnit(22f, TextUnitType.Sp)
//                 )
//             }
//         },
//         actions = {
//             IconButton(
//                 onClick = {
//                     showDialog.value = true
//                 },
//             ) {
//                 Icon(
//                     painter = painterResource(R.drawable.settings),
//                     contentDescription = "Settings Button",
//                     tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
//                     modifier = Modifier.size(24.dp)
//                 )
//             }
//         },
//         scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
//         colors = TopAppBarDefaults.topAppBarColors(
//             containerColor = CustomMaterialTheme.colorScheme.background
//         ),
//     )
// }

@Composable
fun MainAppBottomBar(
    currentView: MutableState<MainScreenViewType>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    BottomAppBar(
        containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
        contentColor = CustomMaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SelectableBottomAppBarItem(
                selected = currentView.value == MainScreenViewType.PhotosGridView,
                action = {
                    if (currentView.value != MainScreenViewType.PhotosGridView) {
                        currentView.value = MainScreenViewType.PhotosGridView
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = if (currentView.value == MainScreenViewType.PhotosGridView) R.drawable.photogrid_filled else R.drawable.photogrid),
                        contentDescription = "Navigate to photos page",
                        modifier = Modifier
                            .size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Photos",
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            )

            SelectableBottomAppBarItem(
                selected = currentView.value == MainScreenViewType.SecureFolder,
                action = {
                    if (currentView.value != MainScreenViewType.SecureFolder) {
                        currentView.value = MainScreenViewType.SecureFolder
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = if (currentView.value == MainScreenViewType.SecureFolder) R.drawable.locked_folder_filled else R.drawable.locked_folder),
                        contentDescription = "Navigate to secure folder page",
                        modifier = Modifier
                            .size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Secure",
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            )

            SelectableBottomAppBarItem(
                selected = currentView.value == MainScreenViewType.AlbumsGridView,
                action = {
                    if (currentView.value != MainScreenViewType.AlbumsGridView) {
                        currentView.value = MainScreenViewType.AlbumsGridView
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = if (currentView.value == MainScreenViewType.AlbumsGridView) R.drawable.albums_filled else R.drawable.albums),
                        contentDescription = "Navigate to albums page",
                        modifier = Modifier
                            .size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Albums",
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            )

            SelectableBottomAppBarItem(
                selected = currentView.value == MainScreenViewType.SearchPage,
                action = {
                    if (currentView.value != MainScreenViewType.SearchPage) {
                        selectedItemsList.clear()
                        currentView.value = MainScreenViewType.SearchPage
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Navigate to search page",
                        modifier = Modifier
                            .size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Search",
                        fontSize = TextUnit(14f, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            )
        }
    }
}

@Composable
fun MainAppSelectingBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section && it != MediaStoreData()
                }
            }
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
                coroutineScope.launch {
                    val hasVideos = selectedItemsWithoutSection.any {
                        it.type == MediaType.Video
                    }

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        type = if (hasVideos) "video/*" else "images/*"
                    }

                    val fileUris = ArrayList<Uri>()
                    selectedItemsWithoutSection.forEach {
                        fileUris.add(it.uri)
                    }

                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        )

        val show = remember { mutableStateOf(false) }
        var isMoving by remember { mutableStateOf(false) }
        MoveCopyAlbumListView(
            show = show,
            selectedItemsList = selectedItemsList,
            isMoving = isMoving,
            groupedMedia = null,
            insetsPadding = WindowInsets.statusBars
        )

        BottomAppBarItem(
            text = "Move",
            iconResId = R.drawable.cut,
            action = {
                isMoving = true
                show.value = true
            }
        )

        BottomAppBarItem(
            text = "Copy",
            iconResId = R.drawable.copy,
            action = {
                isMoving = false
                show.value = true
            }
        )

        val showDeleteDialog = remember { mutableStateOf(false) }
        val runDeleteAction = remember { mutableStateOf(false) }

        GetPermissionAndRun(
            uris = selectedItemsWithoutSection.map { it.uri },
            shouldRun = runDeleteAction,
            onGranted = {
                setTrashedOnPhotoList(
                    context = context,
                    list = selectedItemsWithoutSection.map { it.uri },
                    trashed = true
                )

                selectedItemsList.clear()
            }
        )

        BottomAppBarItem(
            text = "Delete",
            iconResId = R.drawable.delete,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showDeleteDialog,
                    dialogTitle = "Move these items to Trash Bin?",
                    confirmButtonLabel = "Delete"
                ) {
                    runDeleteAction.value = true
                }
            },
            action = {
                showDeleteDialog.value = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(selectedItemsList: SnapshotStateList<MediaStoreData>) {
    val groupedMedia = MainActivity.mainViewModel.groupedMedia.collectAsState(initial = emptyList())

    val selectedItemsWithoutSection by remember {
        derivedStateOf {
            selectedItemsList.filter {
                it.type != MediaType.Section && it != MediaStoreData()
            }
        }
    }

    TopAppBar(
        title = {
            SplitButton(
                primaryContentPadding = PaddingValues(16.dp, 0.dp, 12.dp, 0.dp),
                secondaryContentPadding = PaddingValues(8.dp, 8.dp, 12.dp, 8.dp),
                secondaryContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                primaryContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "clear selection button",
                        tint = CustomMaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(24.dp)
                    )
                },
                secondaryContent = {
                    Text(
                        text = selectedItemsWithoutSection.size.toString(),
                        color = CustomMaterialTheme.colorScheme.onSurface,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .animateContentSize()
                    )
                },
                primaryAction = {
                    selectedItemsList.clear()
                },
                secondaryAction = {
                    selectedItemsList.clear()
                }
            )
        },
        actions = {
            val allItemsList by remember { derivedStateOf { groupedMedia.value ?: emptyList() } }
            val isTicked by remember {
                derivedStateOf {
                    selectedItemsList.size == allItemsList.size
                }
            }

            IconButton(
                onClick = {
                    if (groupedMedia.value != null) {
                        if (isTicked) {
                            selectedItemsList.clear()
                            selectedItemsList.add(MediaStoreData())
                        } else {
                            selectedItemsList.clear()

                            selectedItemsList.addAll(allItemsList)
                        }
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(1000.dp))
                    .size(42.dp)
                    .background(if (isTicked) CustomMaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(R.drawable.checklist),
                    contentDescription = "select all items",
                    tint = if (isTicked) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                )
            }

            IconButton(
                onClick = {
                    // showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_options),
                    contentDescription = "show more options for selected items",
                    tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        ),
    )
}

@Composable
fun IsSelectingBottomAppBar(
    items: @Composable (RowScope.() -> Unit)
) {
    BottomAppBar(
        containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
        contentColor = CustomMaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumViewTopBar(
    dir: String,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    showDialog: MutableState<Boolean>,
    onBackClick: () -> Unit
) {
    val title = dir.split("/").last()
    val show by remember {
        derivedStateOf {
            selectedItemsList.size > 0
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "SingleAlbumViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
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
                    Text(
                        text = title,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            showDialog.value = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "show more options for the album view",
                            tint = CustomMaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        } else {
            IsSelectingTopBar(
                selectedItemsList = selectedItemsList
            )
        }
    }
}

@Composable
fun SingleAlbumViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section && it != MediaStoreData()
                }
            }
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
                coroutineScope.launch {
                    val hasVideos = selectedItemsWithoutSection.any {
                        it.type == MediaType.Video
                    }

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        type = if (hasVideos) "video/*" else "images/*"
                    }

                    val fileUris = ArrayList<Uri>()
                    selectedItemsWithoutSection.forEach {
                        fileUris.add(it.uri)
                    }

                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        )

        val show = remember { mutableStateOf(false) }
        var isMoving by remember { mutableStateOf(false) }
        MoveCopyAlbumListView(
            show = show,
            selectedItemsList = selectedItemsList,
            isMoving = isMoving,
            groupedMedia = null,
            insetsPadding = WindowInsets.statusBars
        )

        BottomAppBarItem(
            text = "Move",
            iconResId = R.drawable.cut,
            action = {
                isMoving = true
                show.value = true
            }
        )

        BottomAppBarItem(
            text = "Copy",
            iconResId = R.drawable.copy,
            action = {
                isMoving = false
                show.value = true
            }
        )

        val showDeleteDialog = remember { mutableStateOf(false) }
        val runTrashAction = remember { mutableStateOf(false) }

        GetPermissionAndRun(
            uris = selectedItemsWithoutSection.map { it.uri },
            shouldRun = runTrashAction,
            onGranted = {
                setTrashedOnPhotoList(
                    context = context,
                    list = selectedItemsWithoutSection.map { it.uri },
                    trashed = true
                )

                selectedItemsList.clear()
            }
        )

        BottomAppBarItem(
            text = "Delete",
            iconResId = R.drawable.delete,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showDeleteDialog,
                    dialogTitle = "Move selected items to Trash Bin?",
                    confirmButtonLabel = "Delete"
                ) {
                    runTrashAction.value = true
                }
            },
            action = {
                showDeleteDialog.value = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridViewTopBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>,
    onBackClick: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    val runEmptyTrashAction = remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(runEmptyTrashAction.value) {
        if (runEmptyTrashAction.value) {
            permanentlyDeletePhotoList(
                context = context,
                list = groupedMedia.map { it.uri }
            )

            runEmptyTrashAction.value = false
        }
    }

    ConfirmationDialogWithBody(
        showDialog = showDialog,
        dialogTitle = "Empty trash bin?",
        dialogBody = "This deletes all items in the trash bin, action cannot be undone",
        confirmButtonLabel = "Empty Out"
    ) {
        runEmptyTrashAction.value = true
    }

    val show by remember {
        derivedStateOf {
            selectedItemsList.size > 0
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "TrashedPhotoGridViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
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
                    Text(
                        text = "Trash Bin",
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            showDialog.value = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.trash),
                            contentDescription = "empty out the trash bin",
                            tint = CustomMaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            )
        } else {
            IsSelectingTopBar(selectedItemsList = selectedItemsList)
        }
    }
}

@Composable
fun TrashedPhotoGridViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
    IsSelectingBottomAppBar {

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section && it != MediaStoreData()
                }
            }
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
                coroutineScope.launch {
                    val hasVideos = selectedItemsWithoutSection.any {
                        it.type == MediaType.Video
                    }

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        type = if (hasVideos) "video/*" else "images/*"
                    }

                    val fileUris = ArrayList<Uri>()
                    selectedItemsWithoutSection.forEach {
                        fileUris.add(it.uri)
                    }

                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        )

        val showRestoreDialog = remember { mutableStateOf(false) }
        val runRestoreAction = remember { mutableStateOf(false) }

        GetPermissionAndRun(
            uris = selectedItemsWithoutSection.map { it.uri },
            shouldRun = runRestoreAction,
            onGranted = {
                setTrashedOnPhotoList(
                    context = context,
                    list = selectedItemsWithoutSection.map { it.uri },
                    trashed = false
                )

                selectedItemsList.clear()
            }
        )

        BottomAppBarItem(
            text = "Restore",
            iconResId = R.drawable.untrash,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showRestoreDialog,
                    dialogTitle = "Restore these items?",
                    confirmButtonLabel = "Restore"
                ) {
                    runRestoreAction.value = true
                }
            },
            action = {
                showRestoreDialog.value = true
            }
        )

        val showPermaDeleteDialog = remember { mutableStateOf(false) }
        val runPermaDeleteAction = remember { mutableStateOf(false) }

        LaunchedEffect(runPermaDeleteAction.value) {
            if (runPermaDeleteAction.value) {
                permanentlyDeletePhotoList(
                    context,
                    selectedItemsWithoutSection.map { it.uri }
                )

                selectedItemsList.clear()

                runPermaDeleteAction.value = false
            }
        }

        BottomAppBarItem(
            text = "Delete",
            iconResId = R.drawable.delete,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialogWithBody(
                    showDialog = showPermaDeleteDialog,
                    dialogTitle = "Permanently delete these items?",
                    dialogBody = "This action cannot be undone!",
                    confirmButtonLabel = "Delete"
                ) {
                    runPermaDeleteAction.value = true
                }
            },
            action = {
                if (selectedItemsWithoutSection.isNotEmpty()) {
                    showPermaDeleteDialog.value = true
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderViewTopAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    onBackClicked: () -> Unit
) {
    val show by remember {
        derivedStateOf {
            selectedItemsList.size > 0
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "SecureFolderGridViewBottomBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { onBackClicked() },
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
                    Text(
                        text = "Secure Folder",
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                }
            )
        } else {
            IsSelectingTopBar(selectedItemsList = selectedItemsList)
        }
    }
}

@Composable
fun SecureFolderViewBottomAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section
                }
            }
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
				shareMultipleSecuredImages(paths = selectedItemsWithoutSection, context = context)
            }
        )

        val showRestoreDialog = remember { mutableStateOf(false) }

        BottomAppBarItem(
            text = "Restore",
            iconResId = R.drawable.unlock,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showRestoreDialog,
                    dialogTitle = "Restore these items?",
                    confirmButtonLabel = "Restore"
                ) {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val newList = groupedMedia.value.toMutableList()

                            moveImageOutOfLockedFolder(selectedItemsWithoutSection, context)
                            newList.removeAll(selectedItemsWithoutSection)

                            groupedMedia.value.filter {
                                it.type == MediaType.Section
                            }.forEach {
                                val filtered = newList.filter { new ->
                                    new.getLastModifiedDay() == it.getLastModifiedDay()
                                }

                                if (filtered.size == 1) newList.remove(it)
                            }

                            selectedItemsList.clear()
                            groupedMedia.value = newList
                        }
                    }
                }
            },
            action = {
                showRestoreDialog.value = true
            }
        )

        val showPermaDeleteDialog = remember { mutableStateOf(false) }
        val runPermaDeleteAction = remember { mutableStateOf(false) }

        LaunchedEffect(runPermaDeleteAction.value) {
            if (runPermaDeleteAction.value) {
                withContext(Dispatchers.IO) {
                    val newList = groupedMedia.value.toMutableList()

                    permanentlyDeleteSecureFolderImageList(
                        list = selectedItemsWithoutSection.map { it.absolutePath }
                    )


                    selectedItemsWithoutSection.forEach {
                        newList.remove(it)
                    }

                    newList.filter {
                        it.type == MediaType.Section
                    }.forEach { item ->
                        // remove sections which no longer have any children
                        val filtered = newList.filter { newItem ->
                            newItem.getLastModifiedDay() == item.getLastModifiedDay()
                        }

                        if (filtered.size == 1) newList.remove(item)
                    }

                    selectedItemsList.clear()
                    groupedMedia.value = newList

                    runPermaDeleteAction.value = false
                }
            }
        }

        BottomAppBarItem(
            text = "Delete",
            iconResId = R.drawable.delete,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialogWithBody(
                    showDialog = showPermaDeleteDialog,
                    dialogTitle = "Permanently delete these items?",
                    dialogBody = "This action cannot be undone!",
                    confirmButtonLabel = "Delete"
                ) {
                    runPermaDeleteAction.value = true
                }
            },
            action = {
                showPermaDeleteDialog.value = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesViewTopAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    onBackClick: () -> Unit
) {
    val show by remember {
        derivedStateOf {
            selectedItemsList.size > 0
        }
    }

    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
        },
        label = "FavouritesGridViewTopBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
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
                    Text(
                        text = "Favourites",
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                }
            )
        } else {
            IsSelectingTopBar(selectedItemsList = selectedItemsList)
        }
    }
}

@Composable
fun FavouritesViewBottomAppBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>
) {
    IsSelectingBottomAppBar {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val dao = MainActivity.applicationDatabase.favouritedItemEntityDao()

        val selectedItemsWithoutSection by remember {
            derivedStateOf {
                selectedItemsList.filter {
                    it.type != MediaType.Section
                }
            }
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
                coroutineScope.launch {
                    val hasVideos = selectedItemsWithoutSection.any {
                        it.type == MediaType.Video
                    }

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        type = if (hasVideos) "video/*" else "images/*"
                    }

                    val fileUris = ArrayList<Uri>()
                    selectedItemsWithoutSection.forEach {
                        fileUris.add(it.uri)
                    }

                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        )

        val show = remember { mutableStateOf(false) }
        MoveCopyAlbumListView(
            show = show,
            selectedItemsList = selectedItemsList,
            isMoving = false,
            groupedMedia = null,
            insetsPadding = WindowInsets.statusBars
        )

        BottomAppBarItem(
            text = "Copy",
            iconResId = R.drawable.copy,
            action = {
                show.value = true
            }
        )

        val showUnFavDialog = remember { mutableStateOf(false) }
        BottomAppBarItem(
            text = "Remove",
            iconResId = R.drawable.unfavourite,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showUnFavDialog,
                    dialogTitle = "Remove selected items from favourites?",
                    confirmButtonLabel = "Remove"
                ) {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val newList = groupedMedia.value.toMutableList()
                            selectedItemsWithoutSection.forEach { item ->
                                dao.deleteEntityById(item.id)
                                newList.remove(item)
                            }

                            groupedMedia.value.filter {
                                it.type == MediaType.Section
                            }.forEach {
                                val filtered = newList.filter { new ->
                                    new.getLastModifiedDay() == it.getLastModifiedDay()
                                }

                                if (filtered.size == 1) newList.remove(it)
                            }

                            selectedItemsList.clear()
                            groupedMedia.value = newList
                        }
                    }
                }
            },
            action = {
                showUnFavDialog.value = true
            }
        )

        val showDeleteDialog = remember { mutableStateOf(false) }
        val runTrashAction = remember { mutableStateOf(false) }

        GetPermissionAndRun(
            uris = selectedItemsWithoutSection.map { it.uri },
            shouldRun = runTrashAction,
            onGranted = {
                setTrashedOnPhotoList(
                    context = context,
                    list = selectedItemsWithoutSection.map { it.uri },
                    trashed = true
                )

                selectedItemsList.clear()
            }
        )

        BottomAppBarItem(
            text = "Delete",
            iconResId = R.drawable.delete,
            cornerRadius = 16.dp,
            dialogComposable = {
                ConfirmationDialog(
                    showDialog = showDeleteDialog,
                    dialogTitle = "Move selected items to trash?",
                    confirmButtonLabel = "Delete"
                ) {
                    coroutineScope.launch {
                        selectedItemsList.forEach {
                            dao.deleteEntityById(it.id)
                        }
                        runTrashAction.value = true
                    }
                }
            },
            action = {
                showDeleteDialog.value = true
            }
        )
    }
}

fun setBarVisibility(
    visible: Boolean,
    window: Window,
    onSetBarVisible: (isVisible: Boolean) -> Unit
) {
    onSetBarVisible(visible)

    window.insetsController?.apply {
        if (visible) {
            show(WindowInsetsCompat.Type.systemBars())
        } else {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    window.setDecorFitsSystemWindows(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualFunctionTopAppBar(
    alternated: Boolean,
    title: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    alternateTitle: @Composable () -> Unit,
    alternateActions: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = @Composable {}
) {
    TopAppBar(
        navigationIcon = navigationIcon,
        title = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "Dual Function App Bar Animation"
            ) { alternate ->
                if (alternate) {
                    alternateTitle()
                } else {
                    title()
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "Dual Function App Bar Animation"
            ) { alternate ->
                if (alternate) {
                    alternateActions()
                } else {
                    actions()
                }
            }
        },
    )
}

@Composable
fun PrototypeMainTopBar(
    alternate: Boolean,
    showDialog: MutableState<Boolean>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>
) {
    DualFunctionTopAppBar(
        alternated = alternate,
        title = {
            Row {
                Text(
                    text = "Lavender ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = "Photos",
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings Button",
                    tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        alternateTitle = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        alternateActions = {
            SelectViewTopBarRightButtons(selectedItemsList = selectedItemsList)
        },
    )
}
