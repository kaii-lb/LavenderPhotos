package com.kaii.photos.compose.grids

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.AnimatableText
import com.kaii.photos.compose.AnimatableTextField
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.DialogClickableItem
import com.kaii.photos.compose.SingleAlbumViewBottomBar
import com.kaii.photos.compose.SingleAlbumViewTopBar
import com.kaii.photos.datastore
import com.kaii.photos.datastore.editInAlbumsList
import com.kaii.photos.datastore.removeFromAlbumsList
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.compose.getAppBarContentTransitionBottomToTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumView(
	navController: NavHostController,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
    val mainViewModel = MainActivity.mainViewModel

    val albumDir = mainViewModel.selectedAlbumDir.collectAsState(initial = null).value ?: return

	val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(LocalContext.current, albumDir, MediaItemSortMode.DateTaken)
	)
//	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

	val mediaStoreData = galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

	var groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
	mainViewModel.setGroupedMedia(groupedMedia.value)

	LaunchedEffect(mediaStoreData.value) {
		groupedMedia.value = mediaStoreData.value
		mainViewModel.setGroupedMedia(mediaStoreData.value)
	}

	val showDialog = remember { mutableStateOf(false) }
    val showBottomSheet by remember { derivedStateOf {
        selectedItemsList.size > 0
    }}
    
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
        println("SHEET STATE $showBottomSheet")
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
	
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
   		sheetDragHandle = {},
        sheetSwipeEnabled = false,
        modifier = Modifier
            .fillMaxSize(1f),
        topBar = {
            SingleAlbumViewTopBar(
                navController = navController,
                dir = albumDir,
                selectedItemsList = selectedItemsList,
                showDialog = showDialog
            )
        },
        sheetContent = {
            SingleAlbumViewBottomBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        },
        sheetPeekHeight = 0.dp,
        sheetShape = RectangleShape
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f)
                .windowInsetsPadding(
                	WindowInsets.navigationBars
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
       		PhotoGrid(
       			groupedMedia = groupedMedia,
       			navController = navController,
       			operation = ImageFunctions.LoadNormalImage,
       			path = albumDir,
       			selectedItemsList = selectedItemsList,
       			shouldPadUp = true
       		)

       		SingleAlbumDialog(showDialog, albumDir, navController, selectedItemsList)
        }
    }
}

@Composable
fun SingleAlbumDialog(
	showDialog: MutableState<Boolean>,
	dir: String,
	navController: NavHostController,
	selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    if (showDialog.value) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val title = dir.split("/").last()

        Dialog(
            onDismissRequest = {
                showDialog.value = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                    .padding(8.dp)
            ) {
                val isEditingFileName = remember { mutableStateOf(false) }

                Box (
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                            	.size(24.dp)
                        )
                    }

                    AnimatableText(
                        first = "Rename",
                        second = title,
                        state = isEditingFileName.value,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                val reverseHeight by animateDpAsState(
					targetValue = if (isEditingFileName.value) 0.dp else 42.dp,
					label = "height of other options",
					animationSpec = tween(
						durationMillis = 500
					)
				)				

				Column (
					modifier = Modifier
                        .height(reverseHeight)
                        .padding(8.dp, 0.dp)
				) {
					DialogClickableItem(
	                    text = "Select",
	                    iconResId = R.drawable.check_item,
	                    position = RowPosition.Top,
	                ) {
	                	showDialog.value = false
	                	selectedItemsList.clear()
	                	selectedItemsList.add(MediaStoreData())
	                }
				}
                val fileName = remember { mutableStateOf(title) }
                val saveFileName = remember { mutableStateOf(false) }

                LaunchedEffect(key1 = saveFileName.value) {
                    if (!saveFileName.value) {
                        return@LaunchedEffect
                    }

                    operateOnImage(
                        "/storage/emulated/0/$dir",
                        0L,
                        ImageFunctions.RenameImage,
                        context,
                        mapOf(
                            Pair("old_name", title),
                            Pair("new_name", fileName.value)
                        )
                    )

					val mainViewModel = MainActivity.mainViewModel
					val newDir = dir.replace(title, fileName.value)
                    mainViewModel.setSelectedAlbumDir(newDir)

	                coroutineScope.launch {
	                    context.datastore.editInAlbumsList(dir, fileName.value)
	                    showDialog.value = false
	                }

					navController.popBackStack()
	                navController.navigate(MultiScreenViewType.SingleAlbumView.name)

                    saveFileName.value = false
                }

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    doAction = saveFileName,
                    rowPosition = RowPosition.Middle,
                    modifier = Modifier
                    	.padding(8.dp, 0.dp)
                ) {
                    fileName.value = title
                }

				Column (
					modifier = Modifier
                        .height(reverseHeight + 6.dp)
                        .padding(8.dp, 0.dp, 8.dp, 6.dp)
				) {
	                DialogClickableItem (
	                    text = "Remove album from list",
	                    iconResId = R.drawable.delete,
	                    position = RowPosition.Bottom,
	                ) {
		                coroutineScope.launch {
		                    context.datastore.removeFromAlbumsList(dir)
		                    showDialog.value = false
		                    navController.popBackStack()
		                }
		            }                
				}
            }
        }
    }
}
