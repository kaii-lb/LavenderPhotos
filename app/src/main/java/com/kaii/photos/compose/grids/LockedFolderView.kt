package com.kaii.photos.compose.grids

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.app_bars.SecureFolderViewBottomAppBar
import com.kaii.photos.compose.app_bars.SecureFolderViewTopAppBar
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.appSecureVideoCacheDir
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.multi_album.groupPhotosBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedFolderView(
    window: Window,
    currentView: MutableState<BottomBarTab>
) {
    val context = LocalContext.current

    val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }
    val navController = LocalNavController.current

    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    BackHandler {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        navController.popBackStack()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var lastLifecycleState by rememberSaveable {
        mutableStateOf(Lifecycle.State.STARTED)
    }
    var hideSecureFolder by rememberSaveable {
        mutableStateOf(false)
    }
    val isGettingPermissions = rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(hideSecureFolder, lastLifecycleState) {
        if (hideSecureFolder
            && navController.currentBackStackEntry?.destination?.hasRoute(Screens.SingleHiddenPhotoView::class) == false
        ) {
            navController.navigate(MultiScreenViewType.MainScreen.name)
        }

        if (lastLifecycleState == Lifecycle.State.DESTROYED) {
            withContext(Dispatchers.IO) {
                File(context.appSecureVideoCacheDir).listFiles()?.forEach {
                    it.delete()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner.lifecycle.currentState, isGettingPermissions.value) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        if (navController.currentBackStackEntry?.destination?.hasRoute(Screens.SingleHiddenPhotoView::class) == false
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

    val secureFolder = File(context.appSecureFolderDir)
    val fileList = remember { secureFolder.listFiles() } ?: return
    val mediaStoreData = emptyList<MediaStoreData>().toMutableList()

    val groupedMedia = remember { mutableStateOf(mediaStoreData.toList()) }

    // TODO: USE APP CONTENT RESOLVER!!!!
    LaunchedEffect(fileList, groupedMedia.value) {
        val restoredFilesDir = context.appRestoredFilesDir
        val dao = applicationDatabase.securedItemEntityDao()

        withContext(Dispatchers.IO) {
            mediaStoreData.clear()

            fileList.forEach { file ->
                val mimeType = Files.probeContentType(Path(file.absolutePath))

                val type =
                    if (mimeType.lowercase().contains("image")) MediaType.Image
                    else if (mimeType.lowercase().contains("video")) MediaType.Video
                    else MediaType.Section

                val decryptedBytes =
                    run {
                        val iv = dao.getIvFromSecuredPath(file.absolutePath)
                        val thumbnailIv = dao.getIvFromSecuredPath(
                            getSecuredCacheImageForFile(file = file, context = context).absolutePath
                        )

                        if (iv != null && thumbnailIv != null) iv + thumbnailIv else ByteArray(32)
                    }

                val originalPath =
                    dao.getOriginalPathFromSecuredPath(file.absolutePath) ?: restoredFilesDir

                val item = MediaStoreData(
                    type = type,
                    id = file.hashCode() * file.length() * file.lastModified(),
                    uri = FileProvider.getUriForFile(
                        context,
                        LAVENDER_FILE_PROVIDER_AUTHORITY,
                        file
                    ),
                    mimeType = mimeType,
                    dateModified = file.lastModified() / 1000,
                    dateTaken = file.lastModified() / 1000,
                    displayName = file.name,
                    absolutePath = file.absolutePath,
                    bytes = decryptedBytes + originalPath.encodeToByteArray()
                )

                mediaStoreData.add(item)
            }

            groupedMedia.value = groupPhotosBy(mediaStoreData, MediaItemSortMode.LastModified)
        }
    }

    val showBottomSheet by remember {
        derivedStateOf {
            selectedItemsList.size > 0
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = SheetValue.Hidden,
    )

    LaunchedEffect(key1 = showBottomSheet) {
        if (showBottomSheet) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = {},
        sheetSwipeEnabled = false,
        topBar = {
            SecureFolderViewTopAppBar(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            ) {
                navController.popBackStack()
            }
        },
        sheetContent = {
            SecureFolderViewBottomAppBar(
                selectedItemsList = selectedItemsList,
                groupedMedia = groupedMedia,
                isGettingPermissions = isGettingPermissions
            )
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape,
        modifier = Modifier
            .fillMaxSize(1f),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhotoGrid(
                groupedMedia = groupedMedia,
                albums = emptyList(),
                selectedItemsList = selectedItemsList,
                viewProperties = ViewProperties.SecureFolder,
                shouldPadUp = true
            )
        }
    }
}

