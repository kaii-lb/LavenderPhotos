package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.SinglePhotoInfoDialog
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.operateOnImage
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
	val state = rememberPagerState {
		groupedMedia.value.size
	}
	val currentMediaItem = remember { derivedStateOf {
		val index = state.layoutInfo.visiblePagesInfo.firstOrNull()?.index ?: 0
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
		val context = LocalContext.current
		val coroutineScope = rememberCoroutineScope()
		ConfirmationDialog(
			showDialog = showActionDialog,
			dialogTitle = neededDialogTitle.value,
			confirmButtonLabel = neededDialogButtonLabel.value,
		) {
			operateOnImage(currentMediaItem.value.absolutePath, currentMediaItem.value.id, neededDialogFunction.value, context)
			sortOutMediaMods(
				currentMediaItem.value,
				groupedMedia,
				coroutineScope,
				navController,
				state
			)
		}
	
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
			state.animateScrollToPage(
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
	val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
	val color = if (isLandscape)
		CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
	else
		CustomMaterialTheme.colorScheme.surfaceContainer

	AnimatedVisibility(
		visible = visible,
		enter =
		slideInVertically (
			animationSpec = tween(
				durationMillis = 350
			)
		) { width -> -width } + fadeIn(),
		exit =
		slideOutVertically(
			animationSpec = tween(
				durationMillis = 400
			)
		) { width -> -width } + fadeOut(),
	) {
		TopAppBar(
			colors = TopAppBarDefaults.topAppBarColors(
				containerColor = color
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
	val color = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
		CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
	else
		CustomMaterialTheme.colorScheme.surfaceContainer

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
			containerColor = color,
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


