package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.AnimatableText
import com.kaii.photos.compose.AnimatableTextField
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.DialogClickableItem
import com.kaii.photos.compose.DialogExpandableItem
import com.kaii.photos.compose.DialogInfoText
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.getExifDataForMedia
import com.kaii.photos.helpers.MediaData
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.helpers.single_image_functions.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.launch

//private const val TAG = "SINGLE_PHOTO_VIEW"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoView(
	navController: NavHostController,
	window: Window,
	scale: MutableState<Float>,
	rotation: MutableState<Float>,
	offset: MutableState<Offset>,
) {
	val mainViewModel = MainActivity.mainViewModel

	val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

	val holderGroupedMedia = mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

	val groupedMedia = remember { mutableStateOf(
		holderGroupedMedia.filter { item ->
			(item.type == MediaType.Image || item.type == MediaType.Video)  && item.mimeType != null && item.id != 0L
		}
	)}

	val systemBarsShown = remember { mutableStateOf(true) }
	val appBarsVisible = remember { mutableStateOf(true) }
	val state = rememberLazyListState()
	val currentMediaItem = remember { derivedStateOf {
		val index = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
		if (index != groupedMedia.value.size) {
			groupedMedia.value[index]
		} else {
			MediaStoreData(
				displayName = "Broken Media"
			)
		}
	} }
	val showActionDialog = remember { mutableStateOf(false) }
	val showInfoDialog = remember { mutableStateOf(false) }
	val neededDialogFunction = remember { mutableStateOf(ImageFunctions.MoveToLockedFolder) }
	val neededDialogTitle = remember { mutableStateOf("Move this ${currentMediaItem.value.type.name} to Locked Folder?") }
	val neededDialogButtonLabel = remember { mutableStateOf("Move") }

	Scaffold (
		topBar =  { TopBar(
			navController,
			currentMediaItem.value,
			appBarsVisible.value,
			showInfoDialog
		) },
		bottomBar = { BottomBar(
			appBarsVisible.value,
			currentMediaItem.value,
			showActionDialog,
			neededDialogTitle,
			neededDialogButtonLabel,
			neededDialogFunction
		)},
		containerColor = CustomMaterialTheme.colorScheme.background,
		contentColor = CustomMaterialTheme.colorScheme.onBackground
	) {  _ ->
		// material theme doesn't seem to apply just above????
		SinglePhotoConfirmationDialog(
			showActionDialog,
			currentMediaItem.value,
			groupedMedia,
			neededDialogTitle,
			neededDialogButtonLabel,
			neededDialogFunction,
			navController,
			state
		)
	
		SinglePhotoInfoDialog(
			showInfoDialog,
			currentMediaItem,
			groupedMedia
		)

		
		Column (
			modifier = Modifier
				.padding(0.dp)
				.background(CustomMaterialTheme.colorScheme.background)
				.fillMaxSize(1f),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			HorizontalImageList(
				currentMediaItem.value,
				groupedMedia,
				state,
				scale,
				rotation,
				offset,
				systemBarsShown,
				window,
				appBarsVisible
			)
		}
	}

	val coroutineScope = rememberCoroutineScope()
	LaunchedEffect(key1 = mediaItem) {
		coroutineScope.launch {
			state.scrollToItem(
				if (groupedMedia.value.indexOf(mediaItem) >= 0) groupedMedia.value.indexOf(mediaItem) else 0
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
	navController: NavHostController,
	mediaItem: MediaStoreData?,
	visible: Boolean,
	showInfoDialog: MutableState<Boolean>
) {
	AnimatedVisibility(
		visible = visible,
		enter =
		slideInVertically (
			animationSpec = tween(
				durationMillis = 250
			)
		) { width -> -width } + fadeIn(),
		exit =
		slideOutVertically(
			animationSpec = tween(
				durationMillis = 300
			)
		) { width -> -width } + fadeOut(),
	) {
		TopAppBar(
//			modifier = Modifier.alpha(alpha),
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
				val mediaTitle = if (mediaItem != null) {
					mediaItem.displayName ?: mediaItem.type.name
				} else {
					"Media"
				}

				Spacer (modifier = Modifier.width(8.dp))

				Text(
					text = mediaTitle,
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
						painter = painterResource(id = R.drawable.favorite),
						contentDescription = "favorite this media item",
						tint = CustomMaterialTheme.colorScheme.onBackground,
						modifier = Modifier
							.size(24.dp)
							.padding(0.dp, 1.dp, 0.dp, 0.dp)
					)
				}

				IconButton(
					onClick = {
						showInfoDialog.value = true
					},
				) {
					Icon(
						painter = painterResource(id = R.drawable.more_options),
						contentDescription = "show more options",
						tint = CustomMaterialTheme.colorScheme.onBackground,
						modifier = Modifier
							.size(24.dp)
					)
				}
			}
		)
	}
}

@Composable
private fun BottomBar(
	visible: Boolean,
	item: MediaStoreData,
	showDialog: MutableState<Boolean>,
	neededDialogTitle: MutableState<String>,
	neededDialogButtonLabel: MutableState<String>,
	neededDialogFunction: MutableState<ImageFunctions>
) {
	AnimatedVisibility(
		visible = visible,
		enter =
		slideInVertically (
			animationSpec = tween(
				durationMillis = 250
			)
		) { width -> width } + fadeIn(),
		exit =
		slideOutVertically(
			animationSpec = tween(
				durationMillis = 300
			)
		) { width -> width } + fadeOut(),
	) {
		val context = LocalContext.current
		BottomAppBar(
			containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
			contentColor = CustomMaterialTheme.colorScheme.onBackground,
			contentPadding = PaddingValues(0.dp),
			actions = {
				Row (
					modifier = Modifier
						.fillMaxWidth(1f)
						.padding(12.dp, 0.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceEvenly
				) {
					val listOfResources = listOf(
						R.drawable.share,
						R.drawable.paintbrush,
						R.drawable.trash,
						R.drawable.locked_folder
					)

					val listOfStrings = listOf(
						"Share",
						"Edit",
						"Delete",
						"Hide"
					)

					repeat(4) { index ->
						val operation = ImageFunctions.entries[index] // WARNING: ORDER IS VERY IMPORTANT!!!
						Button(
							onClick = {
								when (operation) {
									ImageFunctions.MoveToLockedFolder -> {
										neededDialogTitle.value = "Move this image to Locked Folder?"
										neededDialogFunction.value = ImageFunctions.MoveToLockedFolder
										neededDialogButtonLabel.value = "Move"
										showDialog.value = true
									}

									ImageFunctions.TrashImage -> {
										neededDialogTitle.value = "Delete this ${item.type}?"
										neededDialogFunction.value = ImageFunctions.TrashImage
										neededDialogButtonLabel.value = "Delete"
										showDialog.value = true
									}

									else -> {
										operateOnImage(item.absolutePath, item.id, operation, context)
									}
								}
							},
							colors = ButtonDefaults.buttonColors(
								containerColor = Color.Transparent,
								contentColor = CustomMaterialTheme.colorScheme.onBackground
							),
							contentPadding = PaddingValues(0.dp, 4.dp),
							modifier = Modifier
								.wrapContentHeight()
								.weight(1f)
						) {
							Column (
								verticalArrangement = Arrangement.Center,
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								Icon(
									painter = painterResource(id = listOfResources[index]),
									contentDescription = listOfStrings[index],
									tint = CustomMaterialTheme.colorScheme.onBackground,
									modifier = Modifier
										.size(26.dp)
								)
								Text(
									text = listOfStrings[index],
									fontSize = TextUnit(15f, TextUnitType.Sp),
									maxLines = 1,
									modifier = Modifier
										.padding(0.dp, 2.dp, 0.dp, 0.dp)
								)
							}
						}
					}
				}
			},
//			modifier = Modifier.alpha(alpha)
		)
	}
}

@Composable
private fun SinglePhotoConfirmationDialog(
	showDialog: MutableState<Boolean>,
	currentMediaItem: MediaStoreData,
	groupedMedia: MutableState<List<MediaStoreData>>,
	neededDialogTitle: MutableState<String>,
	neededDialogButtonLabel: MutableState<String>,
	neededDialogFunction: MutableState<ImageFunctions>,
	navController: NavHostController,
	state: LazyListState
) {
	if (showDialog.value) {
		val context = LocalContext.current
		val coroutineScope = rememberCoroutineScope()

		AlertDialog(
			onDismissRequest = {
				showDialog.value = false
			},
			confirmButton = {
				Button(
					onClick = {
						showDialog.value = false
						operateOnImage(currentMediaItem.absolutePath, currentMediaItem.id, neededDialogFunction.value, context)
						sortOutMediaMods(
							currentMediaItem,
							groupedMedia,
							coroutineScope,
							navController,
							state
						)
					}
				) {
					Text(
						text = neededDialogButtonLabel.value,
						fontSize = TextUnit(14f, TextUnitType.Sp)
					)
				}
			},
			title = {
				Text(
					text = neededDialogTitle.value,
					fontSize = TextUnit(16f, TextUnitType.Sp)
				)
			},
			dismissButton = {
				Button(
					onClick = {
						showDialog.value = false
					},
					colors = ButtonDefaults.buttonColors(
						containerColor = CustomMaterialTheme.colorScheme.tertiaryContainer,
						contentColor = CustomMaterialTheme.colorScheme.onTertiaryContainer
					)
				) {
					Text(
						text = "Cancel",
						fontSize = TextUnit(14f, TextUnitType.Sp)
					)
				}
			},
			shape = RoundedCornerShape(32.dp)
		)
	}
}

@Composable
private fun SinglePhotoInfoDialog(
	showDialog: MutableState<Boolean>,
	currentMediaItem: State<MediaStoreData>,
	groupedMedia: MutableState<List<MediaStoreData>>
) {
	val context = LocalContext.current
	val isEditingFileName = remember { mutableStateOf(false) }
	
	if (showDialog.value) {
		Dialog(
			onDismissRequest = {
				showDialog.value = false
				isEditingFileName.value = false
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
					.padding(4.dp),
			) {
				Box (
					modifier = Modifier
						.fillMaxWidth(1f),
				) {
					IconButton(
						onClick = {
							showDialog.value = false
							isEditingFileName.value = false
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
						second = "More Options",
						state = isEditingFileName.value,
						modifier = Modifier
							.align(Alignment.Center)
					)
				}

				Column (
					modifier = Modifier
						.padding(12.dp)
						.wrapContentHeight()
				) {
					var originalFileName = currentMediaItem.value.displayName ?: "Broken File"
					val fileName = remember { mutableStateOf(originalFileName) }
					val saveFileName = remember { mutableStateOf(false) }

					val expanded = remember { mutableStateOf(false) }

					LaunchedEffect(key1 = saveFileName.value) {
						if (!saveFileName.value) {
							return@LaunchedEffect
						}

						val oldName = currentMediaItem.value.displayName ?: "Broken File"
						val path = currentMediaItem.value.absolutePath
						operateOnImage(
							path,
							currentMediaItem.value.id,
							ImageFunctions.RenameImage,
							context,
							mapOf(
								Pair("old_name", oldName),
								Pair("new_name", fileName.value)
							)
						)

						originalFileName = fileName.value
						val newGroupedMedia = groupedMedia.value.toMutableList()
						// set currentMediaItem to new one with new name
						val newMedia = currentMediaItem.value.copy(
							displayName = fileName.value,
							absolutePath = path.replace(oldName, fileName.value)
						)

						val index = groupedMedia.value.indexOf(currentMediaItem.value)
						newGroupedMedia[index] = newMedia
						groupedMedia.value = newGroupedMedia
		
						saveFileName.value = false
					}

					AnimatableTextField(
						state = isEditingFileName,
						string = fileName,
						doAction = saveFileName,
						extraAction = expanded,
						rowPosition = RowPosition.Top
					) {
						fileName.value = originalFileName
					}

					val mediaData = getExifDataForMedia(currentMediaItem.value.absolutePath)
					// should add a way to automatically calculate height needed for this
					val addedHeight by remember { mutableStateOf(36.dp * mediaData.keys.size) }
                    val height by androidx.compose.animation.core.animateDpAsState(
	                    targetValue = if (!isEditingFileName.value && expanded.value) {
	                    	124.dp + addedHeight
	                    } else if (!isEditingFileName.value && !expanded.value) {
	                    	124.dp
	                    } else {
	                    	0.dp
	                    },
	                    label = "height of other options",
	                    animationSpec = tween(
	                    	durationMillis = 500
	                    )					
					)

					Column (
						modifier = Modifier
							.height(height)
							.fillMaxWidth(1f)
					) {
						DialogClickableItem(
							text = "Copy to Album",
							iconResId = R.drawable.copy,
							position = RowPosition.Middle,
						)

						DialogClickableItem (
							text = "Move to Album",
							iconResId = R.drawable.cut,
							position = RowPosition.Middle,
						)						

						val infoComposable = @Composable {
							Column (
								modifier = Modifier
									.wrapContentHeight()
							) {
								for (key in mediaData.keys) {
									val value = mediaData[key]

									val splitBy = Regex("(?=[A-Z])")
									val split = key.toString().split(splitBy)
									// println("SPLIT IS $split")
									val name = if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()

									DialogInfoText(
										firstText = name,
										secondText = value.toString(),
										iconResId = key.iconResInt,
									)
								}

								Spacer (modifier = Modifier.height(8.dp))
							}
						}

						DialogExpandableItem (
							text = "More Info",
							iconResId = R.drawable.info,
							position = RowPosition.Bottom,
							expanded = expanded
						) {
							infoComposable()
						}
					}
				}
			}
		}
	}
}