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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.ConfirmationDialogWithBody
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.DialogInfoText
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.getExifDataForMedia
import com.kaii.photos.helpers.moveImageOutOfLockedFolder
import com.kaii.photos.helpers.permanentlyDeleteSecureFolderImageList
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SingleHiddenPhotoView(
    navController: NavHostController,
    window: Window,
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
) {
    val mainViewModel = MainActivity.mainViewModel

    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

    val holderGroupedMedia =
        mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    val systemBarsShown = remember { mutableStateOf(true) }
    val appBarsVisible = remember { mutableStateOf(true) }
    val state = rememberPagerState {
        groupedMedia.value.size
    }
    val currentMediaItem by remember {
        derivedStateOf {
            val index = state.layoutInfo.visiblePagesInfo.firstOrNull()?.index ?: 0
            if (index != groupedMedia.value.size) {
                groupedMedia.value[index]
            } else {
                MediaStoreData(
                    displayName = "Broken Media"
                )
            }
        }
    }

    val showInfoDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(navController, currentMediaItem, appBarsVisible.value, showInfoDialog) },
        bottomBar = {
            BottomBar(
                navController,
                appBarsVisible.value,
                currentMediaItem,
                groupedMedia,
                state
            )
        },
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(CustomMaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalImageList(
                navController,
                currentMediaItem,
                groupedMedia.value,
                state,
                scale,
                rotation,
                offset,
                systemBarsShown,
                window,
                appBarsVisible,
                true
            )
        }
        SingleSecuredPhotoInfoDialog(showDialog = showInfoDialog, currentMediaItem = currentMediaItem)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, mediaItem: MediaStoreData, visible: Boolean, showInfoDialog: MutableState<Boolean>) {
    AnimatedVisibility(
        visible = visible,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> -width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> -width } + fadeOut(),
    ) {
        TopAppBar(
//            modifier = Modifier.alpha(alpha),
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
                val mediaTitle = mediaItem.displayName ?: mediaItem.type.name

                Spacer(modifier = Modifier.width(8.dp))

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
    navController: NavHostController,
    visible: Boolean,
    item: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    state: PagerState
) {
    val coroutineScope = rememberCoroutineScope()

    val showRestoreDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    ConfirmationDialog(
        showDialog = showRestoreDialog,
        dialogTitle = "Move item out of Secure Folder?",
        confirmButtonLabel = "Move"
    ) {
        moveImageOutOfLockedFolder(item.absolutePath)

        sortOutMediaMods(
            item,
            groupedMedia,
            coroutineScope,
            state
        ) {
            navController.popBackStack()
        }
    }

    ConfirmationDialogWithBody(
        showDialog = showDeleteDialog,
        dialogTitle = "Permanently delete this item?",
        dialogBody = "This action cannot be undone!",
        confirmButtonLabel = "Delete"
    ) {
        permanentlyDeleteSecureFolderImageList(listOf(item.absolutePath))

        sortOutMediaMods(
            item,
            groupedMedia,
            coroutineScope,
            state
        ) {
            navController.popBackStack()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> width } + fadeOut(),
    ) {

        BottomAppBar(
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
            contentColor = CustomMaterialTheme.colorScheme.onBackground,
            contentPadding = PaddingValues(0.dp),
            actions = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(12.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            showRestoreDialog.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.unlock),
                                contentDescription = "Restore Image Button",
                                tint = CustomMaterialTheme.colorScheme.primary,
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
                                color = CustomMaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            showDeleteDialog.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.trash),
                                contentDescription = "Permanently Delete Image Button",
                                tint = CustomMaterialTheme.colorScheme.primary,
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
                                color = CustomMaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SingleSecuredPhotoInfoDialog(
	showDialog: MutableState<Boolean>,
	currentMediaItem: MediaStoreData	
) {
	val modifier = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
		Modifier.width(256.dp)
	else
		Modifier.fillMaxWidth(0.85f)

	if (showDialog.value) {
		Dialog (
			onDismissRequest = {
				showDialog.value = false
			},
			properties = DialogProperties(
				usePlatformDefaultWidth = false
			),
		) {
			Column (
				modifier = Modifier
					.then(modifier)
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

					Text (
						text = "Info",
						fontWeight = FontWeight.Bold,
						fontSize = TextUnit(18f, TextUnitType.Sp),
						modifier = Modifier
							.align(Alignment.Center)
					)
				}

				Column (
					modifier = Modifier
						.padding(12.dp)
						.wrapContentHeight()
				) {
					val mediaData = getExifDataForMedia(currentMediaItem.absolutePath)

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
			}			
		}
	}
}
