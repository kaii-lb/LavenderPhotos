package com.kaii.photos.compose

import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.setTrashedOnPhotoList
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
    buttonWidth: Dp = 64.dp,
    buttonHeight: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    textSize: Float = 14f,
    color: Color = Color.Transparent,
    showRipple: Boolean = true,
    cornerRadius: Dp = 1000.dp,
    action: (() -> Unit)? = null,
    dialogComposable: (@Composable () -> Unit)? = null
) {
    val modifier = if (action != null) {
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
                modifier = Modifier
                    .size(iconSize)
            )
        }

        Text(
            text = text,
            fontSize = TextUnit(textSize, TextUnitType.Sp),
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
                    durationMillis = 25
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppTopBar(showDialog: MutableState<Boolean>) {
    TopAppBar(
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
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        ),
    )
}

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
        MoveCopyAlbumListView(show, selectedItemsList, isMoving)

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
        var runDeleteAction by remember { mutableStateOf(false) }

        if (runDeleteAction) {
            setTrashedOnPhotoList(
                context,
                list = selectedItemsWithoutSection.map { it.uri },
                trashed = true
            )

            selectedItemsList.clear()
            runDeleteAction = false
        }

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
                    runDeleteAction = true
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .width(48.dp)
                        .clip(RoundedCornerShape(100.dp, 6.dp, 6.dp, 100.dp))
                        .background(CustomMaterialTheme.colorScheme.primary)
                        .padding(4.dp, 0.dp, 0.dp, 0.dp)
                        .clickable {
                            selectedItemsList.clear()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "clear selection button",
                        tint = CustomMaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    modifier = Modifier
                        .height(43.dp)
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(6.dp, 100.dp, 100.dp, 6.dp))
                        .background(CustomMaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp, 0.dp, 16.dp, 0.dp)
                        .animateContentSize()
                        .clickable {
                            selectedItemsList.clear()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = selectedItemsWithoutSection.size.toString(),
                        color = CustomMaterialTheme.colorScheme.onSurface,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                    )
                }
            }
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

                            for (item in allItemsList) {
                                selectedItemsList.add(item)
                            }
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
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
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
                }
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
        MoveCopyAlbumListView(show, selectedItemsList, isMoving)

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
        var runTrashAction by remember { mutableStateOf(false) }

        if (runTrashAction) {
            setTrashedOnPhotoList(
                context,
                selectedItemsWithoutSection.map { it.uri },
                true
            )

            selectedItemsList.clear()
            runTrashAction = false
        }

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
                    runTrashAction = true
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

    var runEmptyTrashAction by remember { mutableStateOf(false) }

    if (runEmptyTrashAction) {
        permanentlyDeletePhotoList(LocalContext.current, list = groupedMedia.map { it.uri })
        runEmptyTrashAction = false
    }

    ConfirmationDialogWithBody(
        showDialog = showDialog,
        dialogTitle = "Empty trash bin?",
        dialogBody = "This deletes all items in the trash bin, action cannot be undone",
        confirmButtonLabel = "Empty Out"
    ) {
        runEmptyTrashAction = true
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

                    IconButton(
                        onClick = { /* TODO */ },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "show more options for the trash bin",
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
        var runRestoreAction by remember { mutableStateOf(false) }

        if (runRestoreAction) {
            setTrashedOnPhotoList(
                context,
                selectedItemsWithoutSection.map { it.uri },
                false
            )

            selectedItemsList.clear()
            runRestoreAction = false
        }

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
                    runRestoreAction = true
                }
            },
            action = {
                showRestoreDialog.value = true
            }
        )

        val showPermaDeleteDialog = remember { mutableStateOf(false) }
        var runPermaDeleteAction by remember { mutableStateOf(false) }

        if (runPermaDeleteAction) {
            permanentlyDeletePhotoList(
                context,
                selectedItemsWithoutSection.map { it.uri }
            )

            selectedItemsList.clear()
            runPermaDeleteAction = false
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
                    runPermaDeleteAction = true
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
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO */ },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "show more options for the locked folder",
                            tint = CustomMaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
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

                            selectedItemsWithoutSection.forEach { item ->
                                moveImageOutOfLockedFolder(item.absolutePath)

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
                showRestoreDialog.value = true
            }
        )

        val showPermaDeleteDialog = remember { mutableStateOf(false) }

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
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val newList = groupedMedia.value.toMutableList()

                            permanentlyDeletePhotoList(context, selectedItemsWithoutSection.map { it.uri })

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
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO */ },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "show more options for the favourites folder",
                            tint = CustomMaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
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
        MoveCopyAlbumListView(show, selectedItemsList, false)

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
        var runTrashAction by remember { mutableStateOf(false) }
        if (runTrashAction) {
            setTrashedOnPhotoList(
                context,
                selectedItemsWithoutSection.map { it.uri },
                true
            )

            selectedItemsList.clear()
            runTrashAction = false
        }

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
                        runTrashAction = true
                    }
                }
            },
            action = {
                showDeleteDialog.value = true
            }
        )
    }
}
