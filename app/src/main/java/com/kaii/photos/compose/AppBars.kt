package com.kaii.photos.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kaii.photos.R
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType

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
    }  else {
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
fun MainAppBottomBar(currentView: MutableState<MainScreenViewType>) {
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
            // should find a better way
            val unselectedColor = CustomMaterialTheme.colorScheme.surfaceContainer
            val selectedColor = CustomMaterialTheme.colorScheme.secondaryContainer
            var photoGridColor by remember { mutableStateOf(unselectedColor) }
            var lockedFolderColor by remember { mutableStateOf(unselectedColor) }
            var albumGridColor by remember { mutableStateOf(unselectedColor) }
            var searchPageColor by remember { mutableStateOf(unselectedColor) }
            // for the love of god find a better way
            var photoGridIcon by remember { mutableIntStateOf(R.drawable.photogrid_filled) }
            var lockedFolderIcon by remember { mutableIntStateOf(R.drawable.locked_folder) }
            var albumGridIcon by remember { mutableIntStateOf(R.drawable.albums) }

            when (currentView.value) {
                MainScreenViewType.PhotosGridView -> {
                    photoGridColor = selectedColor
                    lockedFolderColor = unselectedColor
                    albumGridColor = unselectedColor
                    searchPageColor = unselectedColor

                    photoGridIcon = R.drawable.photogrid_filled
                    lockedFolderIcon = R.drawable.locked_folder
                    albumGridIcon = R.drawable.albums
                }
                MainScreenViewType.SecureFolder -> {
                    photoGridColor = unselectedColor
                    lockedFolderColor = selectedColor
                    albumGridColor = unselectedColor
                    searchPageColor = unselectedColor

                    photoGridIcon = R.drawable.photogrid
                    lockedFolderIcon = R.drawable.locked_folder_filled
                    albumGridIcon = R.drawable.albums
                }
                MainScreenViewType.AlbumsGridView -> {
                    photoGridColor = unselectedColor
                    lockedFolderColor = unselectedColor
                    albumGridColor = selectedColor
                    searchPageColor = unselectedColor

                    photoGridIcon = R.drawable.photogrid
                    lockedFolderIcon = R.drawable.locked_folder
                    albumGridIcon = R.drawable.albums_filled
                }
                MainScreenViewType.SearchPage -> {
                    photoGridColor = unselectedColor
                    lockedFolderColor = unselectedColor
                    albumGridColor = unselectedColor
                    searchPageColor = selectedColor

                    photoGridIcon = R.drawable.photogrid
                    lockedFolderIcon = R.drawable.locked_folder
                    albumGridIcon = R.drawable.albums
                }
            }

            // photo grid button
            BottomAppBarItem(
                text = "Photos",
                iconResId = photoGridIcon,
                color = photoGridColor,
                cornerRadius = 16.dp,
                action = {
	                if (currentView.value != MainScreenViewType.PhotosGridView) {
	                    currentView.value = MainScreenViewType.PhotosGridView
	                }
                }
            )

            // locked folder button
            BottomAppBarItem(
                text = "Secure",
                iconResId = lockedFolderIcon,
                color = lockedFolderColor,
                cornerRadius = 16.dp,
                action = {
	                if (currentView.value != MainScreenViewType.SecureFolder) {
	                    currentView.value = MainScreenViewType.SecureFolder
	                }
                }
            )

            // album grid button
            BottomAppBarItem(
                text = "Albums",
                iconResId = albumGridIcon,
                color = albumGridColor,
                cornerRadius = 16.dp,
                action = {
	                if (currentView.value != MainScreenViewType.AlbumsGridView) {
	                    currentView.value = MainScreenViewType.AlbumsGridView
	                }
                }
            )

            // search page button
            BottomAppBarItem(
                text = "Search",
                iconResId = R.drawable.search,
                color = searchPageColor,
                cornerRadius = 16.dp,
                action = {
	                if (currentView.value != MainScreenViewType.SearchPage) {
	                    currentView.value = MainScreenViewType.SearchPage
	                }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(selectedItemsList: SnapshotStateList<MediaStoreData>, groupedMedia: List<MediaStoreData>) {
	var first by remember { mutableStateOf(selectedItemsList.first()) }
	if (selectedItemsList.size == 1) first = selectedItemsList.first()
	
	TopAppBar(
        title = {
        	Row (
           		verticalAlignment = Alignment.CenterVertically,
            	horizontalArrangement = Arrangement.SpaceEvenly
        	) {
	           	Row (
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

	           	Spacer (modifier = Modifier.width(4.dp))
	               
	           	Row (
					modifier = Modifier
                        .height(42.dp)
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(6.dp, 100.dp, 100.dp, 6.dp))
                        .background(CustomMaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp, 0.dp, 16.dp, 0.dp)
                        .clickable {
                            selectedItemsList.clear()
                        },
	            	verticalAlignment = Alignment.CenterVertically,
	            	horizontalArrangement = Arrangement.Center
	           	) {
					Text(
						text = selectedItemsList.size.toString(),
						color = CustomMaterialTheme.colorScheme.onSecondaryContainer,
						fontSize = TextUnit(18f, TextUnitType.Sp),
						modifier = Modifier
							.wrapContentSize()
					) 		
	           	}
        	}
        },
        actions = {
            IconButton(
                onClick = {
					val filteredMedia = groupedMedia.filter {
						it.type == MediaType.Image || it.type == MediaType.Video
					}
               		
                	if (selectedItemsList.size == filteredMedia.size) {
                    	selectedItemsList.clear()
                    	selectedItemsList.add(first)
                	} else {
                		selectedItemsList.clear()
                		
	                    for (item in filteredMedia) {
                   			selectedItemsList.add(item)
	                    }
                	}
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.check_item),
                    contentDescription = "select all items",
                    tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
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
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	groupedMedia: MutableState<List<MediaStoreData>>,
	isTrashBin: Boolean = false
) {
    val context = LocalContext.current
	
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
	        BottomAppBarItem(
	        	text = "Share", 
	        	iconResId = R.drawable.share,
	        	action = {
		            val hasVideos = selectedItemsList.any {
		                it.type == MediaType.Video
		            }

		            val intent = Intent().apply {
		                action = Intent.ACTION_SEND_MULTIPLE
		                type = if (hasVideos) "video/*" else "images/*"
		            }

		            val fileUris = ArrayList<Uri>()
		            selectedItemsList.forEach {
		                fileUris.add(it.uri)
		            }

		            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

		            context.startActivity(Intent.createChooser(intent, null))
	        	}
        	)

			if (isTrashBin) {
				val showRestoreDialog = remember { mutableStateOf(false) }
				BottomAppBarItem(
					text = "Restore",
					iconResId = R.drawable.favorite,
					cornerRadius = 16.dp,
					dialogComposable = {
					    ConfirmationDialog(showDialog = showRestoreDialog, dialogTitle = "Restore these items?", confirmButtonLabel = "Restore") {
					        val newList = groupedMedia.value.toMutableList()
					        selectedItemsList.forEach { item ->
					            operateOnImage(
					                item.absolutePath,
					                item.id,
					                ImageFunctions.UnTrashImage,
					                context
					            )
					            newList.remove(item)
					        }
					        selectedItemsList.clear()
					        groupedMedia.value = newList
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
					    ConfirmationDialog(showDialog = showPermaDeleteDialog, dialogTitle = "Permanently delete these items?", confirmButtonLabel = "Delete") {
					        val newList = groupedMedia.value.toMutableList()
					        selectedItemsList.forEach { item ->
					            operateOnImage(
					                item.absolutePath,
					                item.id,
					                ImageFunctions.PermaDeleteImage,
					                context
					            )
					            newList.remove(item)
					        }
					        selectedItemsList.clear()
					        groupedMedia.value = newList
					    }
		        	},
		        	action = {
		        		showPermaDeleteDialog.value = true
		        	}
	        	)
			} else {
		        BottomAppBarItem(text = "Move", iconResId = R.drawable.cut)

		        BottomAppBarItem(text = "Copy", iconResId = R.drawable.copy)

				val showDeleteDialog = remember { mutableStateOf(false) }
		        BottomAppBarItem(
		        	text = "Delete",
		        	iconResId = R.drawable.delete,
		        	cornerRadius = 16.dp,
		        	dialogComposable = {
					    ConfirmationDialog(showDialog = showDeleteDialog, dialogTitle = "Move selected items to Trash Bin?", confirmButtonLabel = "Delete") {
					        val newList = groupedMedia.value.toMutableList()
					        selectedItemsList.forEach { item ->
					            operateOnImage(
					                item.absolutePath,
					                item.id,
					                ImageFunctions.TrashImage,
					                context
					            )
					            newList.remove(item)
					        }
					        selectedItemsList.clear()
					        groupedMedia.value = newList
					    }
		        	},
		        	action = {
		        		showDeleteDialog.value = true
		        	}
	        	)
			}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAlbumViewTopBar(
    navController: NavHostController,
    showDialog: MutableState<Boolean>,
    title: String,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>
) {
    AnimatedContent(
        targetState = selectedItemsList.size > 0,
        transitionSpec = {
            getAppBarContentTransition(selectedItemsList.size > 0)
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
                        onClick = { navController.popBackStack() },
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
            IsSelectingTopBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        }
    }
}

@Composable
fun SingleAlbumViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>
) {
    AnimatedContent(
        targetState = selectedItemsList.size > 0,
        transitionSpec = {
            getAppBarContentTransition(selectedItemsList.size > 0)
        },
        label = "SingleAlbumViewTopBarAnimatedContent",
        modifier = Modifier
            .fillMaxWidth(1f)
    ) { target ->
        if (target) {
            IsSelectingBottomAppBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridViewTopBar(
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: List<MediaStoreData>
) {
    AnimatedContent(
        targetState = selectedItemsList.size > 0,
        transitionSpec = {
            getAppBarContentTransition(selectedItemsList.size > 0)
        },
        label = "TrashedPhotoGridViewBottomBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                    ) {
                        Icon(
                            painter = painterResource(id = com.kaii.photos.R.drawable.back_arrow),
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
                        onClick = { /* TODO */ },
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
            IsSelectingTopBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
        }
    }
}

@Composable
fun TrashedPhotoViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>
) {
    AnimatedContent(
        targetState = selectedItemsList.size > 0,
        transitionSpec = {
            getAppBarContentTransition(selectedItemsList.size > 0)
        },
        label = "TrashedPhotoGridViewTopBarAnimatedContent",
        modifier = Modifier
            .fillMaxWidth(1f)
    ) { target ->
        if (target) {
            IsSelectingBottomAppBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia, isTrashBin = true)
        }
    }
}
