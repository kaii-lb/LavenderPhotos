package com.kaii.photos.compose

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Window
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.movableContentOf
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.MainActivity
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.helpers.single_image_functions.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "SINGLE_PHOTO_VIEW"

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SinglePhotoView(
	navController: NavHostController,
	window: Window,
	scale: MutableState<Float>,
	rotation: MutableState<Float>,
	offset: MutableState<Offset>
) {
    val mainViewModel = MainActivity.mainViewModel
    
    val mediaItem = mainViewModel.selectedMediaData.collectAsState(initial = null).value ?: return

	val holderGroupedMedia = mainViewModel.groupedMedia.collectAsState(initial = null).value ?: return

	val groupedMedia = remember { mutableStateOf(holderGroupedMedia.filter { item ->
        !(item.mimeType == null && item.type == MediaType.Section)
   	})}
	
    var systemBarsShown by remember { mutableStateOf(true) }
    var appBarAlpha by remember { mutableFloatStateOf(1f) }
    var currentMediaItem by remember { mutableStateOf(mediaItem) }
	val state = rememberLazyListState()
	val showDialog = remember { mutableStateOf(false) }
	val neededDialogFunction = remember { mutableStateOf(ImageFunctions.MoveToLockedFolder) }
	val neededDialogTitle = remember { mutableStateOf("Move this ${currentMediaItem.type.name} to Locked Folder?") }
	val neededDialogButtonLabel = remember { mutableStateOf("Move") }
	
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

                       	coroutineScope.launch {
                        	val scrollIndex = groupedMedia.value.indexOf(currentMediaItem)
                        	// val added = if (scrollIndex == groupedMedia.value.size) -1 else 1

                            if (groupedMedia.value.size == 1) {
                            	navController.popBackStack()	
                            }

							val newMedia = groupedMedia.value.toMutableList()
							newMedia.removeAt(scrollIndex)
							groupedMedia.value = newMedia

                            state.animateScrollToItem(scrollIndex)
                       	}
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

    Scaffold (
        topBar =  { TopBar(navController, currentMediaItem, appBarAlpha) },
        bottomBar = { BottomBar(appBarAlpha, currentMediaItem, showDialog, neededDialogTitle, neededDialogButtonLabel, neededDialogFunction) },
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) {  _ ->
        Column (
            modifier = Modifier
				.padding(0.dp)
				.background(CustomMaterialTheme.colorScheme.background)
				.fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val requestBuilderTransform =
                { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
                    requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
                }

            val preloadingData =
                rememberGlidePreloadingData(
                    groupedMedia.value,
                    Size(75f, 75f),
                    requestBuilderTransform = requestBuilderTransform,
                )

            LazyRow (
                modifier = Modifier
                    .fillMaxHeight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                state = state,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
            ) {
                items(
                    count = preloadingData.size,
                    key = {
       	                groupedMedia.value[it].uri.toString()
       	            },
                ) { i ->
                	val movableContent = movableContentOf {
                		val index = if (i+1 == preloadingData.size) 0 else i
						Log.d(TAG, "INDEX IS $index $i ${preloadingData.size}")
						val (mediaStoreItem, preloadRequestBuilder) = preloadingData[index]
						
	                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
	                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

						if (mediaStoreItem.type != MediaType.Section && mediaStoreItem.mimeType != null && mediaStoreItem.id != 0L) {
						    currentMediaItem = mediaStoreItem

	                        GlideImage(
		                        model = mediaStoreItem.uri,
		                        contentDescription = "selected image",
		                        contentScale = ContentScale.Fit,
		                        modifier = Modifier
									.fillParentMaxSize(1f)
									.combinedClickable (
										onClick = {
											if (systemBarsShown) {
												windowInsetsController.apply {
													hide(WindowInsetsCompat.Type.systemBars())
													systemBarsBehavior =
														WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
												}
												window.setDecorFitsSystemWindows(false)
												systemBarsShown = false
												appBarAlpha = 0f
											} else {
												windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
												window.setDecorFitsSystemWindows(false)
												systemBarsShown = true
												appBarAlpha = 1f
											}
										},

										onDoubleClick = {
											if (scale.value == 1f) {
												scale.value = 2f
												rotation.value = 0f
												offset.value = Offset.Zero
											} else {
												scale.value = 1f
												rotation.value = 0f
												offset.value = Offset.Zero
											}
										}
									)
									.graphicsLayer(
										scaleX = scale.value,
										scaleY = scale.value,
										rotationZ = rotation.value,
										translationX = -offset.value.x * scale.value,
										translationY = -offset.value.y * scale.value,
										transformOrigin = TransformOrigin(0.5f, 0.5f)
									)
									.pointerInput(Unit) {
// 										detectTransformGestures (
// 											panZoomLock = true,
// 											onGesture = { centerOfTransformation, movedBy, zoom, rotateBy ->
// 												val oldScale = scale
// 												val newScale = scale * zoom
// 
// 												if (newScale != 1f) {
// 													offset = (offset + Offset(0.5f, 0.5f) / oldScale).rotateBy(rotateBy) -
// 															(Offset(0.5f, 0.5f) / newScale + movedBy * 3f / oldScale)
// 												}
// 
// 												scale = newScale
// 												rotation += rotateBy
// 											}
// 										)

										forEachGesture {
											awaitPointerEventScope {
												awaitFirstDown()

												do {
													val event = awaitPointerEvent()
													
													if (event.changes.size == 2) {
														scale.value *= event.calculateZoom()
														scale.value.coerceIn(0.75f, 5f)
														rotation.value += event.calculateRotation()

														event.changes.forEach {
															it.consume()
														}
													} else if (event.changes.size == 1 && event.calculatePan() != Offset.Zero) {
														if (scale.value != 1f) {
															// this is from android docs, i have no clue what the math here is xD
															offset.value = (offset.value + Offset(0.5f, 0.5f) / scale.value) -
																	(Offset(0.5f, 0.5f) / scale.value + event.calculatePan())

															event.changes.forEach {
																it.consume()
															}
														}
													}
												} while (event.changes.any { it.pressed })
											}
										}
									},
		                    ) {
		                        it.thumbnail(preloadRequestBuilder).signature(mediaStoreItem.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
		                    }
						} else {
							Column (
								modifier = Modifier.fillParentMaxSize(1f).background(CustomMaterialTheme.colorScheme.primary)
							) {
								Text(
									text = "this media is broken or is not an image/video"
								)
							}
						}
                	}
                	movableContent()
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, mediaItem: MediaStoreData?, alpha: Float) {
    TopAppBar(
    	modifier = Modifier.alpha(alpha),
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
                    painter = painterResource(id = com.kaii.photos.R.drawable.favorite),
                    contentDescription = "favorite this media item",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(0.dp, 1.dp, 0.dp, 0.dp)
                )
            }

       		IconButton(
                onClick = { /* TODO */ },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.more_options),
                    contentDescription = "show more options",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Composable
private fun BottomBar(
	alpha: Float, item: MediaStoreData,
	showDialog: MutableState<Boolean>,
	neededDialogTitle: MutableState<String>,
	neededDialogButtonLabel: MutableState<String>,
	neededDialogFunction: MutableState<ImageFunctions>
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
            		com.kaii.photos.R.drawable.share,
           			com.kaii.photos.R.drawable.paintbrush,
            		com.kaii.photos.R.drawable.trash,	
            		com.kaii.photos.R.drawable.locked_folder		
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
									neededDialogTitle.value = "Delete this image?"
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
        modifier = Modifier.alpha(alpha)
    )
}

private fun Offset.rotateBy(angle: Float): Offset {
	val rads = angle * (PI / 180)
	val cos = cos(rads)
	val sin = sin(rads)
	return Offset((x * cos - y * sin).toFloat(), (x * sin + y * cos).toFloat())
}
