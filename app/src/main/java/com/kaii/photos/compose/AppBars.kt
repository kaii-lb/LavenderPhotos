package com.kaii.photos.compose

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MainScreenViewType
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

fun getAppBarContentTransitionBottomToTop(slideUp: Boolean) = run {
	if (slideUp) {
        (slideInVertically(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            )
        )).togetherWith(
            slideOutVertically(
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
        (slideInVertically(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> -width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            )
        )).togetherWith(
            slideOutVertically(
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
        NavigationBar (
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 16.dp,
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(8.dp, 0.dp),
        ) {
            NavigationBarItem(
                selected = currentView.value == MainScreenViewType.PhotosGridView,
                onClick = { 
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

            NavigationBarItem(
                selected = currentView.value == MainScreenViewType.SecureFolder,
                onClick = { 
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

            NavigationBarItem(
                selected = currentView.value == MainScreenViewType.AlbumsGridView,
                onClick = { 
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

            NavigationBarItem(
                selected = currentView.value == MainScreenViewType.SearchPage,
                onClick = { 
	                if (currentView.value != MainScreenViewType.SearchPage) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(selectedItemsList: SnapshotStateList<MediaStoreData>) {
	val groupedMedia = MainActivity.mainViewModel.groupedMedia.collectAsState(initial = emptyList<MediaStoreData>())
	
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
	           		val selectionSize = if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) "0" else selectedItemsList.size.toString()
					Text(
						text = selectionSize,
						color = CustomMaterialTheme.colorScheme.onSurface,
						fontSize = TextUnit(18f, TextUnitType.Sp),
						modifier = Modifier
							.wrapContentSize()
					) 		
	           	}
        	}
        },
        actions = {
        	val filteredMedia by remember { derivedStateOf {
				groupedMedia.value?.filter {
					it.type != MediaType.Section
				} ?: emptyList()
        	}}
        	val isTicked by remember { derivedStateOf {
        		selectedItemsList.size == filteredMedia.size
        	}}
        	
            IconButton(
                onClick = {
                	if (groupedMedia.value != null) {
	                	if (selectedItemsList.size == filteredMedia.size) {
	                    	selectedItemsList.clear()
	                    	selectedItemsList.add(MediaStoreData())
	                	} else {
	                		selectedItemsList.clear()
	                		
		                    for (item in filteredMedia) {
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
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	isTrashBin: Boolean = false,
	groupedMedia: MutableState<List<MediaStoreData>>
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
    dir: String,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    showDialog: MutableState<Boolean>
) {
	val title = dir.split("/").last()
	// val showDialog = remember { mutableStateOf(false) }
	val show by remember { derivedStateOf {
		selectedItemsList.size > 0
	}}
		
	// SingleAlbumDialog(showDialog, dir, navController)
	
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
            IsSelectingTopBar(
            	selectedItemsList = selectedItemsList
           	)
        }
    }
}

@Composable
fun SingleAlbumViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>
) {
    IsSelectingBottomAppBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedPhotoGridViewTopBar(
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
	val show by remember { derivedStateOf {
		selectedItemsList.size > 0
	}}
    AnimatedContent(
        targetState = show,
        transitionSpec = {
            getAppBarContentTransition(show)
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
            IsSelectingTopBar(selectedItemsList = selectedItemsList)
        }
    }
}

@Composable
fun TrashedPhotoViewBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    groupedMedia: MutableState<List<MediaStoreData>>
) {
	val show by remember { derivedStateOf {
		selectedItemsList.size > 0
	}}
    AnimatedContent(
        targetState = show,
        transitionSpec = {
		    getAppBarContentTransitionBottomToTop(show)           
        },
        label = "TrashedPhotoGridViewBottomBarAnimatedContent",
        modifier = Modifier
            .fillMaxWidth(1f)
    ) { target ->
        if (target) {
            IsSelectingBottomAppBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia, isTrashBin = true)
        } else {
        	Row	(
        		modifier = Modifier
        			.fillMaxWidth(1f)
        	) {}
        }
    }
}
