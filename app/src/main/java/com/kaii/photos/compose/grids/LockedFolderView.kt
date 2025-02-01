package com.kaii.photos.compose.grids

import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.SecureFolderViewBottomAppBar
import com.kaii.photos.compose.SecureFolderViewTopAppBar
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.SectionItem
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.appSecureVideoCacheDir
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.gallery_model.groupPhotosBy
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedFolderView(
    window: Window,
    currentView: MutableState<MainScreenViewType>
) {
	val context = LocalContext.current

	// TODO: move again to Android/data for space purposes
	val oldDir = context.getDir("locked_folder", Context.MODE_PRIVATE)
	oldDir?.let { oldFilesDir ->
		val subFiles = oldFilesDir.listFiles()

		if (subFiles?.isNotEmpty() == true) {
			val newDir = context.appSecureFolderDir
			subFiles.forEach { file ->
				val newPath = newDir + "/" + file.name
				file.copyTo(File(newPath))
				file.delete()
			}
		}
	}

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

    LaunchedEffect(hideSecureFolder, lastLifecycleState) {
        if (hideSecureFolder
            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.SingleHiddenPhotoVew.name
        ) {
            navController.navigate(MultiScreenViewType.MainScreen.name)
        }

        if (lastLifecycleState == Lifecycle.State.DESTROYED) {
        	withContext(Dispatchers.IO) {
        		File(context.appSecureVideoCacheDir)?.listFiles()?.forEach {
        			it?.delete()
        		}
        	}
        }
    }

    DisposableEffect(key1 = lifecycleOwner.lifecycle.currentState) {
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->

                when (event) {
                    Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                        if (navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.SingleHiddenPhotoVew.name
                            && navController.currentBackStackEntry?.destination?.route != MultiScreenViewType.MainScreen.name
                        ) {
                            lastLifecycleState = Lifecycle.State.DESTROYED
                        }
                    }

                    Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START, Lifecycle.Event.ON_CREATE -> {
                        if (lastLifecycleState == Lifecycle.State.DESTROYED && navController.currentBackStackEntry != null) {
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
    val fileList = secureFolder.listFiles() ?: return
    val mediaStoreData = emptyList<MediaStoreData>().toMutableList()

    val groupedMedia =
        remember { mutableStateOf(mediaStoreData.toList()) }

	val encryptionManager = remember {
		EncryptionManager()
	}

	// TODO: USE APP CONTENT RESOLVER!!!!
	var viewProperties by remember { mutableStateOf(ViewProperties.SecureFolder)}
	LaunchedEffect(fileList) {
		withContext(Dispatchers.IO) {
		    fileList.forEach { file ->
		        val mimeType = Files.probeContentType(Path(file.absolutePath))
		        val dateTaken = getDateTakenForMedia(file.absolutePath)

		        val type =
		            if (mimeType.lowercase().contains("image")) MediaType.Image
		            else if (mimeType.lowercase().contains("video")) MediaType.Video
		            else MediaType.Section

				// TODO: stop storing insane amounts of data in memory
				val decryptedBytes =
					if (type == MediaType.Image) {
						applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(file.absolutePath)
					} else {
                        val videoIv = applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(file.absolutePath)
                        val thumbnailIv = applicationDatabase.securedItemEntityDao().getIvFromSecuredPath(
                            context.appSecureVideoCacheDir + "/" + file.name + ".png"
                        )

                        videoIv + thumbnailIv
                    }

		        val item = MediaStoreData(
		            type = type,
		            id = file.hashCode() * file.length() * file.lastModified(),
		            uri = file.absolutePath.toUri(),
		            mimeType = mimeType,
		            dateModified = file.lastModified() / 1000,
		            dateTaken = dateTaken,
		            displayName = file.name,
		            absolutePath = file.absolutePath,
		            bytes = decryptedBytes
		        )

		        mediaStoreData.add(item)
		    }

		    groupedMedia.value = groupPhotosBy(mediaStoreData, MediaItemSortMode.LastModified)

		    if (groupedMedia.value.isEmpty()) viewProperties = ViewProperties.SecureFolder.apply {
		    	isListEmpty = true
		    }
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
            SecureFolderViewTopAppBar(selectedItemsList = selectedItemsList, currentView = currentView) {
                navController.popBackStack()
            }
        },
        sheetContent = {
            SecureFolderViewBottomAppBar(
                selectedItemsList = selectedItemsList,
                groupedMedia = groupedMedia
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
                path = null,
                selectedItemsList = selectedItemsList,
                viewProperties = viewProperties,
                shouldPadUp = true
            )
        }
    }
}

