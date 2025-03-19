package com.kaii.photos.compose

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.createDirectoryPicker
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareMultipleSecuredImages
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "APP_BARS"

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
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
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
            color = MaterialTheme.colorScheme.onBackground,
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
            Box (
                modifier = Modifier
                    .height(32.dp)
                    .width(64.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
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

@Composable
fun MainAppTopBar(
    alternate: Boolean,
    showDialog: MutableState<Boolean>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
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
            AnimatedVisibility(
                visible = currentView.value == DefaultTabs.TabTypes.albums,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                val activityLauncher = createDirectoryPicker { path ->
                    if (path != null) mainViewModel.settings.AlbumsList.addToAlbumsList(path)
                }

                IconButton(
                    onClick = {
                        activityLauncher.launch(null)
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Add album",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings Button",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        alternateTitle = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        alternateActions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            )
        },
    )
}

@Composable
fun MainAppBottomBar(
    currentView: MutableState<BottomBarTab>,
    tabs: List<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                SelectableBottomAppBarItem(
                    selected = currentView.value == tab,
                    action = {
                        if (currentView.value != tab) {
                            selectedItemsList.clear()
                            currentView.value = tab
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = if (currentView.value == tab) tab.icon.filled else tab.icon.nonFilled),
                            contentDescription = "Navigate to ${tab.name} page",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.name,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .wrapContentSize()
                        )
                    }
                )
            }
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

        val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)
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
            	if (confirmToDelete) showDeleteDialog.value = true
            	else runDeleteAction.value = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
) {
    TopAppBar(
        title = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        actions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
    )
}

@Composable
fun IsSelectingBottomAppBar(
    items: @Composable (RowScope.() -> Unit)
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
    currentView: MutableState<BottomBarTab>,
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        } else {
            IsSelectingTopBar(
                selectedItemsList = selectedItemsList,
                currentView = currentView
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

        val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)
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
            	if (confirmToDelete) showDeleteDialog.value = true
            	else runTrashAction.value = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridViewTopBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    onBackClick: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    val runEmptyTrashAction = remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(runEmptyTrashAction.value) {
        if (runEmptyTrashAction.value) {
            permanentlyDeletePhotoList(
                context = context,
                list = groupedMedia.filter { it.type != MediaType.Section }.map { it.uri }
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            )
        } else {
            IsSelectingTopBar(selectedItemsList = selectedItemsList, currentView = currentView)
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
    currentView: MutableState<BottomBarTab>,
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { onBackClicked() },
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
            IsSelectingTopBar(selectedItemsList = selectedItemsList, currentView = currentView)
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

        var showLoadingDialog by remember { mutableStateOf(false) }
        var loadingDialogTitle by remember { mutableStateOf("Decrypting Files") }

        if (showLoadingDialog) {
            LoadingDialog(title = loadingDialogTitle, body = "Please wait while the media is processed")
        }

        BottomAppBarItem(
            text = "Share",
            iconResId = R.drawable.share,
            action = {
            	coroutineScope.launch(Dispatchers.IO) {
            		async {
            			loadingDialogTitle = "Decrypting Files"
                        showLoadingDialog = true

                        val cachedPaths = emptyList<Pair<String, MediaType>>().toMutableList()

                        selectedItemsWithoutSection.forEach { item ->
                            val iv = applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(item.absolutePath)
                            if (iv == null) {
                            	Log.e(TAG, "IV for ${item.displayName} was null, aborting decrypt")
                            	return@async
                            }

                            val originalFile = File(item.absolutePath)
                            val cachedFile = File(context.cacheDir, item.displayName ?: item.id.toString())

                            EncryptionManager.decryptInputStream(
                                inputStream = originalFile.inputStream(),
                                outputStream = cachedFile.outputStream(),
                                iv = iv
                            )

                            cachedFile.deleteOnExit()
	                   		cachedPaths.add(Pair(cachedFile.absolutePath, item.type))
	            		}

                        showLoadingDialog = false

						shareMultipleSecuredImages(paths = cachedPaths, context = context)
            		}.await()
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
                  	loadingDialogTitle = "Restoring Files"
                    showLoadingDialog = true

                    coroutineScope.launch {
                        async {
                            val newList = groupedMedia.value.toMutableList()

                            moveImageOutOfLockedFolder(
                            	list = selectedItemsWithoutSection,
                            	context = context
                           	) {
                           		showLoadingDialog = false
                           	}

                            newList.removeAll(selectedItemsList)

                            selectedItemsList.clear()
                            groupedMedia.value = newList
                        }.await()
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
            	loadingDialogTitle = "Deleting Files"
                showLoadingDialog = true

                withContext(Dispatchers.IO) {
                    async {
                        val newList = groupedMedia.value.toMutableList()

                        permanentlyDeleteSecureFolderImageList(
                            list = selectedItemsWithoutSection.map { it.absolutePath },
                            context = context
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

                        showLoadingDialog = false
                        runPermaDeleteAction.value = false
                    }.await()
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
    currentView: MutableState<BottomBarTab>,
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
                            tint = MaterialTheme.colorScheme.onBackground,
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
            IsSelectingTopBar(selectedItemsList = selectedItemsList, currentView = currentView)
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
        val dao = applicationDatabase.favouritedItemEntityDao()

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
        val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete().collectAsStateWithLifecycle(initialValue = true)

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
            	if (confirmToDelete) {
            		showDeleteDialog.value = true
				}
            	else {
            		coroutineScope.launch {
            		    selectedItemsList.forEach {
            		        dao.deleteEntityById(it.id)
            		    }
            		    runTrashAction.value = true
            		}
            	}
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

    // window.setDecorFitsSystemWindows(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualFunctionTopAppBar(
    alternated: Boolean,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    alternateTitle: @Composable () -> Unit,
    alternateActions: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = @Composable {}
) {
    TopAppBarDefaults.topAppBarColors()
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
                	Row(
               			verticalAlignment = Alignment.CenterVertically,
               			horizontalArrangement = Arrangement.SpaceEvenly
                	) {
                		actions()
                	}
                }
            }
        },
    )
}
