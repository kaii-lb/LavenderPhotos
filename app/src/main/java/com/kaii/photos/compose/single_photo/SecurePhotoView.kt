package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.single_view.SingleViewTopBar
import com.kaii.photos.compose.dialogs.SingleSecurePhotoInfoDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialog
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.managers.GenericFileManager
import com.kaii.photos.helpers.PhotoGridConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureVideoCacheDir
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.parent
import com.kaii.photos.helpers.scrolling.retainSinglePhotoScrollState
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getOriginalPath
import com.kaii.photos.models.secure_folder.SecureFolderViewModel
import com.kaii.photos.permissions.files.rememberDirectoryPermissionManager
import com.kaii.photos.presentation.single_photos_views.DismissDragState.Companion.barScaleModifier
import com.kaii.photos.presentation.single_photos_views.rememberDismissSinglePhotoState
import io.github.kaii_lb.lavender.immichintegration.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "RestrictedApi")
@Composable
fun SecurePhotoView(
    viewModel: SecureFolderViewModel,
    index: Int,
    window: Window
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()

    val isGettingPermissions = rememberSaveable { mutableStateOf(false) }

    val allowScreenCapture by context.appModule.settings.permissions
        .getAllowSecureFolderScreenCapture()
        .collectAsStateWithLifecycle(initialValue = false)

    // apply/clear FLAG_SECURE reactively so toggling the setting takes effect immediately
    LaunchedEffect(allowScreenCapture) {
        if (allowScreenCapture) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    DisposableEffect(lifecycleState) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        // if not navigating to grid view
                        if (navController.currentBackStackEntry?.destination?.hasRoute(Screens.SecureFolder.GridView::class) != true
                            && !isGettingPermissions.value
                        ) {
                            if (event == Lifecycle.Event.ON_DESTROY) context.appModule.scope.launch(Dispatchers.IO) {
                                File(context.appSecureVideoCacheDir).listFiles()?.forEach {
                                    it.delete()
                                }
                            }

                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            navController.popBackStack(route = Screens.MainPages.MainGrid.GridView::class, inclusive = false)
                        }
                    }

                    else -> {
                        if (!allowScreenCapture) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
            }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val items = viewModel.mediaFlow.collectAsLazyPagingItems()

    val appBarsVisible = remember { mutableStateOf(true) }
    val state = rememberPagerState(
        initialPage = index
    ) {
        items.itemCount
    }

    val resources = LocalResources.current
    val currentMediaItem by remember {
        derivedStateOf {
            if (state.currentPage in 0..<items.itemCount && items.itemCount != 0) {
                items[state.currentPage] as PhotoLibraryUIModel.SecuredMedia
            } else {
                PhotoLibraryUIModel.SecuredMedia(
                    item = MediaStoreData.dummyItem.copy(
                        displayName = resources.getString(R.string.media_broken)
                    ),
                    bytes = ByteArray(0),
                    auth = Auth.None,
                    endpoint = null
                )
            }
        }
    }

    LaunchedEffect(items.itemCount) {
        snapshotFlow { items.itemCount }.collectLatest {
            delay(PhotoGridConstants.LOADING_TIME_SHORT.milliseconds)
            if (items.itemCount == 0) launch(Dispatchers.Main) {
                navController.popBackStack(Screens.SecureFolder::class, inclusive = false)
            }
        }
    }

    val scrollState = retainSinglePhotoScrollState(isOpenWithView = false)
    var showInfoDialog by remember { mutableStateOf(false) }

    val draggableState = rememberDismissSinglePhotoState()

    Scaffold(
        topBar = {
            val topBarDetailsFormat by viewModel.topBarDetailsFormat.collectAsStateWithLifecycle()

            SingleViewTopBar(
                mediaItem = { currentMediaItem.item },
                visible = appBarsVisible.value,
                showInfoDialog = { showInfoDialog },
                isOpenWithDefaultView = false,
                privacyMode = { scrollState.privacyMode },
                topBarDetailsFormat = topBarDetailsFormat,
                showTags = false,
                expandInfoDialog = {
                    showInfoDialog = true
                },
                modifier = Modifier
                    .barScaleModifier(draggableState)
            )
        },
        bottomBar = {
            BottomBar(
                visible = appBarsVisible.value,
                securedMedia = currentMediaItem,
                privacyMode = scrollState.privacyMode,
                isGettingPermissions = isGettingPermissions,
                getMediaCount = {
                    items.itemCount
                },
                process = { action ->
                    viewModel.runAction(
                        context = context,
                        action = action
                    )
                },
                modifier = Modifier
                    .barScaleModifier(draggableState)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        val blurViews by viewModel.blurViews.collectAsStateWithLifecycle()
        val useBlackBackground by viewModel.useBlackBackground.collectAsStateWithLifecycle()
        val useCache by viewModel.useCache.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalImageList(
                items = items,
                state = state,
                window = window,
                appBarsVisible = appBarsVisible,
                isSecuredMedia = true,
                scrollState = scrollState,
                blurViews = blurViews,
                useBlackBackground = useBlackBackground,
                useCache = useCache,
                swipeDownProgress = { draggableState.progress }
            )
        }

        if (showInfoDialog) {
            SingleSecurePhotoInfoDialog(
                currentMediaItem = currentMediaItem,
                privacyMode = scrollState.privacyMode,
                dismiss = {
                    showInfoDialog = false
                },
                togglePrivacyMode = scrollState::togglePrivacyMode
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(
    visible: Boolean,
    securedMedia: PhotoLibraryUIModel.SecuredMedia,
    privacyMode: Boolean,
    isGettingPermissions: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    getMediaCount: () -> Int,
    process: (action: GenericFileManager.Action) -> Unit
) {
    val context = LocalContext.current

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val navController = LocalNavController.current
    if (getMediaCount() == 0) {
        navController.popBackStack()
    }

    val permissionManager = rememberDirectoryPermissionManager(
        onGranted = {
            process(
                GenericFileManager.Action.Restore(
                    list = listOf(
                        SelectionManager.SelectedItem(
                            id = securedMedia.item.id,
                            uri = securedMedia.item.uri,
                            immichUrl = securedMedia.item.immichUrl,
                            isImage = securedMedia.item.type == MediaType.Image,
                            parentPath = securedMedia.item.parentPath
                        )
                    )
                )
            )
        },
        onRejected = {
            isGettingPermissions.value = false
        }
    )

    if (showRestoreDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.secure_move_out),
            confirmButtonLabel = stringResource(id = R.string.media_move),
            action = {
                isGettingPermissions.value = true

                permissionManager.start(
                    directories = setOf(
                        securedMedia.bytes?.getOriginalPath()?.parent() ?: context.appRestoredFilesDir
                    )
                )
            },
            onDismiss = {
                showRestoreDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialogWithBody(
            title = stringResource(id = R.string.media_delete_permanently_confirm),
            body = stringResource(id = R.string.action_cannot_be_undone),
            confirmButtonLabel = stringResource(id = R.string.media_delete),
            action = {
                process(
                    GenericFileManager.Action.Delete(
                        list = listOf(
                            SelectionManager.SelectedItem(
                                id = securedMedia.item.id,
                                uri = securedMedia.item.uri,
                                immichUrl = securedMedia.item.immichUrl,
                                isImage = securedMedia.item.type == MediaType.Image,
                                parentPath = securedMedia.item.parentPath,
                            )
                        )
                    )
                )
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

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
                            process(
                                GenericFileManager.Action.Share(
                                    list = listOf(
                                        SelectionManager.SelectedItem(
                                            id = securedMedia.item.id,
                                            uri = securedMedia.item.uri,
                                            immichUrl = securedMedia.item.immichUrl,
                                            isImage = securedMedia.item.type == MediaType.Image,
                                            parentPath = securedMedia.item.parentPath
                                        )
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
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable(enabled = !privacyMode) {
                            showRestoreDialog = true
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
                            showDeleteDialog = true
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