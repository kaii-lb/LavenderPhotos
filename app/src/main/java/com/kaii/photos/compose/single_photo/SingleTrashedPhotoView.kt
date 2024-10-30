package com.kaii.photos.compose.single_photo

import android.annotation.SuppressLint
import android.content.ContentValues
import android.provider.MediaStore.MediaColumns
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.compose.SinglePhotoInfoDialog
import com.kaii.photos.helpers.permanentlyDeletePhotoList
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType

//private const val TAG = "SINGLE_TRASHED_PHOTO_VIEW"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SingleTrashedPhotoView(
    navController: NavHostController,
    window: Window,
    scale: MutableState<Float>,
    rotation: MutableState<Float>,
    offset: MutableState<Offset>,
) {  
    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

    val holderGroupedMedia = mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

    val groupedMedia = remember {
        mutableStateOf(
            holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
        )
    }

    LaunchedEffect(holderGroupedMedia) {
    	groupedMedia.value = 
    		holderGroupedMedia.filter { item ->
                item.type != MediaType.Section
            }
    }

    val systemBarsShown = remember { mutableStateOf(true) }
    val appBarsVisible = remember { mutableStateOf(true) }

    var currentMediaItemIndex by rememberSaveable {
        mutableIntStateOf(
            groupedMedia.value.indexOf(
                mediaItem
            )
        )
    }
    val state = rememberPagerState(
        initialPage = currentMediaItemIndex.coerceAtLeast(0)
    ) {
        groupedMedia.value.size
    }

    LaunchedEffect(key1 = state.currentPage) {
        currentMediaItemIndex = state.currentPage
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

    val showDialog = remember { mutableStateOf(false) }
	val showInfoDialog = remember { mutableStateOf(false) }
	
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

                        permanentlyDeletePhotoList(context, listOf(currentMediaItem.uri))

                        sortOutMediaMods(
                            currentMediaItem,
                            groupedMedia,
                            coroutineScope,
                            state
                        ) {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = "Permanently delete this ${currentMediaItem.type.name}?",
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

    Scaffold(
        topBar = { TopBar(navController, currentMediaItem, appBarsVisible.value, showInfoDialog) },
        bottomBar = {
            BottomBar(
                navController,
                appBarsVisible.value,
                currentMediaItem,
                showDialog,
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
                appBarsVisible
            )
        }
        
        SinglePhotoInfoDialog(showDialog = showInfoDialog, currentMediaItem = currentMediaItem, groupedMedia = groupedMedia, showMoveCopy = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, mediaItem: MediaStoreData?, visible: Boolean, showInfoDialog: MutableState<Boolean>) {
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
                val mediaTitle = if (mediaItem != null) {
                    mediaItem.displayName ?: mediaItem.type.name
                } else {
                    "Media"
                }

                Spacer(modifier = Modifier.width(8.dp))

                val splitBy = Regex("trashed-[0-9]+-")
                Text(
                    text = mediaTitle.split(splitBy).lastOrNull() ?: "Media",
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
    showDialog: MutableState<Boolean>,
    groupedMedia: MutableState<List<MediaStoreData>>,
    state: PagerState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                            val untrashValues = ContentValues().apply {
                                put(MediaColumns.IS_TRASHED, false)
                            }
                            context.contentResolver.update(item.uri, untrashValues, null)

                            sortOutMediaMods(
                                item,
                                groupedMedia,
                                coroutineScope,
                                state
                            ) {
                                navController.popBackStack()
                            }
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
                                painter = painterResource(id = R.drawable.favourite),
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
                            showDialog.value = true
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
