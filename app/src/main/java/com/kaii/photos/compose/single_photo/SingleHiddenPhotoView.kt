package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.util.Log
import android.view.Window
import android.view.WindowManager
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.SingleViewTopBar
import com.kaii.photos.compose.dialogs.ConfirmationDialog
import com.kaii.photos.compose.dialogs.ConfirmationDialogWithBody
import com.kaii.photos.compose.dialogs.LoadingDialog
import com.kaii.photos.compose.dialogs.SingleSecurePhotoInfoDialog
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.GetDirectoryPermissionAndRun
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.getDecryptCacheForFile
import com.kaii.photos.helpers.getParentFromPath
import com.kaii.photos.helpers.getSecureDecryptedVideoFile
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.helpers.shareSecuredImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getOriginalPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "com.kaii.photos.compose.single_photo.SingleHiddenPhotoView"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SingleHiddenPhotoView(
    mediaItemId: Long,
    window: Window
) {
    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val mainViewModel = LocalMainViewModel.current

    var lastLifecycleState by rememberSaveable {
        mutableStateOf(Lifecycle.State.STARTED)
    }
    var hideSecureFolder by rememberSaveable {
        mutableStateOf(false)
    }
    val isGettingPermissions = rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(hideSecureFolder) {
        if (hideSecureFolder
            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.SecureFolder.name
        ) {
            navController.navigate(MultiScreenViewType.MainScreen.name)
        }
    }

    DisposableEffect(key1 = lifecycleOwner.lifecycle.currentStateAsState().value, isGettingPermissions.value) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->

                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        if (navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.SecureFolder.name
                            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                            && !isGettingPermissions.value
                        ) {
                            lastLifecycleState = Lifecycle.State.DESTROYED
                        }
                    }

                    Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START, Lifecycle.Event.ON_CREATE -> {
                        if (lastLifecycleState == Lifecycle.State.DESTROYED && navController.currentBackStackEntry != null && !isGettingPermissions.value) {
                            lastLifecycleState = Lifecycle.State.STARTED

                            hideSecureFolder = true
                        }
                    }

                    else -> {}
                }
            }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    if (hideSecureFolder) return

    val holderGroupedMedia =
        mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

    val mediaItem by remember {
        derivedStateOf {
            holderGroupedMedia.find {
                it.id == mediaItemId
            }
        }
    }

    if (mediaItem == null) return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    val appBarsVisible = remember { mutableStateOf(true) }
    val state = rememberPagerState {
        groupedMedia.value.size
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

    var showInfoDialog by remember { mutableStateOf(false) }

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
                visible = appBarsVisible.value,
                item = currentMediaItem,
                groupedMedia = groupedMedia,
                state = state,
                isGettingPermissions = isGettingPermissions
            ) {
                navController.popBackStack()
            }
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
                appBarsVisible = appBarsVisible,
                isHidden = true
            )
        }

        if (showInfoDialog) {
            SingleSecurePhotoInfoDialog(
                currentMediaItem = currentMediaItem
            ) {
                showInfoDialog = false
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = mediaItem) {
        coroutineScope.launch {
            state.scrollToPage(
                if (groupedMedia.value.indexOf(mediaItem) >= 0) groupedMedia.value.indexOf(mediaItem) else 0
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    state: PagerState,
    isGettingPermissions: MutableState<Boolean>,
    popBackStack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current
    val applicationDatabase = LocalAppDatabase.current

    val showRestoreDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val runRestoreAction = remember { mutableStateOf(false) }

    var showLoadingDialog by remember { mutableStateOf(false) }
    if (showLoadingDialog) {
        LoadingDialog(title = "Restoring Files", body = "Please wait while the media is processed")
    }

    GetDirectoryPermissionAndRun(
        absoluteDirPaths = listOf(item.bytes?.getOriginalPath()?.getParentFromPath() ?: context.appRestoredFilesDir),
        shouldRun = runRestoreAction,
        onGranted = { _ ->
            mainViewModel.launch(Dispatchers.IO) {
                moveImageOutOfLockedFolder(
                    list = listOf(item),
                    context = context,
                    applicationDatabase = applicationDatabase
                ) {
                    isGettingPermissions.value = false
                    showLoadingDialog = false
                }

                sortOutMediaMods(
                    item,
                    groupedMedia,
                    coroutineScope,
                    state
                ) {
                    popBackStack()
                }
            }
        },
        onRejected = {
            isGettingPermissions.value = false
            showLoadingDialog = false
        }
    )

    ConfirmationDialog(
        showDialog = showRestoreDialog,
        dialogTitle = "Move item out of Secure Folder?",
        confirmButtonLabel = "Move"
    ) {
        isGettingPermissions.value = true
        runRestoreAction.value = true
        showLoadingDialog = true
    }

    ConfirmationDialogWithBody(
        showDialog = showDeleteDialog,
        dialogTitle = "Permanently delete this item?",
        dialogBody = "This action cannot be undone!",
        confirmButtonLabel = "Delete"
    ) {
        mainViewModel.launch(Dispatchers.IO) {
            permanentlyDeleteSecureFolderImageList(
                list = listOf(item.absolutePath),
                context = context
            )

            sortOutMediaMods(
                item,
                groupedMedia,
                coroutineScope,
                state
            ) {
                popBackStack()
            }
        }
    }

    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(4.dp, 0.dp)
            .wrapContentHeight()
            .fillMaxWidth(1f),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible || showLoadingDialog,
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
                            coroutineScope.launch(Dispatchers.IO) {
                                showLoadingDialog = true

                                val iv = applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(item.absolutePath)
                                if (iv == null) {
                                    Log.e(TAG, "IV for ${item.displayName} was null, aborting")
                                    return@launch
                                }

                                val originalFile = File(item.absolutePath)

                                val cachedFile =
                                    if (item.type == MediaType.Video) {
                                        getSecureDecryptedVideoFile(originalFile.name, context)
                                    } else {
                                        getDecryptCacheForFile(originalFile, context)
                                    }

                                if (!cachedFile.exists()) {
                                    if (item.type == MediaType.Video) {
                                        EncryptionManager.decryptVideo(
                                            absolutePath = originalFile.absolutePath,
                                            context = context,
                                            iv = iv,
                                            progress = {}
                                        )
                                    } else {
                                        EncryptionManager.decryptInputStream(
                                            inputStream = originalFile.inputStream(),
                                            outputStream = cachedFile.outputStream(),
                                            iv = iv
                                        )
                                    }
                                }

                                showLoadingDialog = false

                                shareSecuredImage(
                                    absolutePath = cachedFile.absolutePath,
                                    context = context
                                )
                            }
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
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable {
                            showRestoreDialog.value = true
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
                        text = "Restore",
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
                            showDeleteDialog.value = true
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
                        text = "Delete",
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