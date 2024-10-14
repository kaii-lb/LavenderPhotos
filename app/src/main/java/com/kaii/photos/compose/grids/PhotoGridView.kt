package com.kaii.photos.compose.grids

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.FolderDoesntExist
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.main_activity.MainDataSharingModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.math.roundToInt

private const val TAG = "PHOTO_GRID_VIEW"

@Composable
fun PhotoGrid(
	groupedMedia: MutableState<List<MediaStoreData>>,
	navController: NavHostController,
	operation: ImageFunctions,
	path: String?,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	emptyText: String = "Empty Folder",
	emptyIconResId: Int = R.drawable.error,
	prefix: String = "",
	shouldPadUp: Boolean = false,
	modifier: Modifier = Modifier
) {
	val hasFiles = if (path == null) {
		groupedMedia.value.isNotEmpty()
	} else {
		Path("/storage/emulated/0/$path").checkHasFiles()
	}

	if (hasFiles == null) {
		FolderDoesntExist()
		return
	}
	 
	if (hasFiles) {
		Row (
			modifier = Modifier
				.fillMaxSize(1f)
				.then(modifier)	
		) {		
			DeviceMedia(
				groupedMedia,
				navController,
				operation,
				selectedItemsList,
				prefix,
				shouldPadUp
			)
		}
	} else {
		FolderIsEmpty(emptyText, emptyIconResId)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMedia(
	groupedMedia: MutableState<List<MediaStoreData>>,
	navController: NavHostController,
	operation: ImageFunctions,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	prefix: String,
	shouldPadUp: Boolean
) {
	val gridState = rememberLazyGridState()
	var showLoadingSpinner by remember { mutableStateOf(true) }

	val coroutineScope = rememberCoroutineScope()

	BackHandler (
		enabled = selectedItemsList.size > 0
	) {
		selectedItemsList.clear()
	}

	if (groupedMedia.value.isNotEmpty()) {
		val handler = Handler(Looper.getMainLooper())
		val runnable = Runnable {
			showLoadingSpinner = false
		}
		handler.removeCallbacks(runnable)
		handler.postDelayed(runnable, 500)
	}
	
	Box (
		modifier = Modifier
			.fillMaxSize(1f)
			.background(CustomMaterialTheme.colorScheme.background)
			.padding(
				0.dp,
				0.dp,
				0.dp,
				if (selectedItemsList.size > 0 && shouldPadUp) 80.dp else 0.dp
			)
	) {
		LazyVerticalGrid(
	        columns = GridCells.Fixed(
				if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
					3
				} else {
					6
				}
			),
	        modifier = Modifier
				.fillMaxSize(1f)
				.align(Alignment.TopCenter),
	        state = gridState
	    ) {
	        items(
	            count = groupedMedia.value.size,
	            key = {
					groupedMedia.value[it].uri.toString()
	            },
	            span = { index ->
	                val item = groupedMedia.value[index]
	                if (item.type == MediaType.Section) {
	                    GridItemSpan(maxLineSpan)
	                } else {
	                    GridItemSpan(1)
	                }
	            }
	        ) { i ->
	        	if (groupedMedia.value.isEmpty()) return@items
				val mediaStoreItem = groupedMedia.value[i]

				Row (
					modifier = Modifier
						.wrapContentSize()
						.animateItem(
							fadeInSpec = null
						)
				) {
					MediaStoreItem(
						navController,
						mediaStoreItem,
						operation,
						MainActivity.mainViewModel,
						groupedMedia.value,
						prefix,
						selectedItemsList
					)
				}
	        }
	    }
	    
		if (showLoadingSpinner) {
			Row (
				modifier = Modifier
					.fillMaxWidth(1f)
					.height(48.dp)
					.align(Alignment.TopCenter),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center
			) {
				Row (
					modifier = Modifier
						.size(40.dp)
						.clip(RoundedCornerShape(1000.dp))
						.background(CustomMaterialTheme.colorScheme.surfaceContainer),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.Center
				) {
					CircularProgressIndicator(
						modifier = Modifier
							.size(22.dp),
						color = CustomMaterialTheme.colorScheme.primary,
						strokeWidth = 4.dp,
						strokeCap = StrokeCap.Round
					)
				}
			}
		}

		Box (
			modifier = Modifier
				.align(Alignment.TopEnd)
				.fillMaxHeight(1f)
				.width(48.dp)
		) {
			var showHandle by remember { mutableStateOf(false) }
			var isScrollingByHandle by remember { mutableStateOf(false) }
			val interactionSource = remember { MutableInteractionSource() }

			LaunchedEffect(interactionSource) {
				interactionSource.interactions.collect { interaction -> 
					when(interaction) {
						is DragInteraction.Start -> { isScrollingByHandle = true }
						is DragInteraction.Cancel -> { isScrollingByHandle = false }
						is DragInteraction.Stop -> { isScrollingByHandle = false }
						else -> {}
					}
				}
			}
						
			LaunchedEffect(key1 = gridState.isScrollInProgress, key2 = isScrollingByHandle) {
				if (gridState.isScrollInProgress || isScrollingByHandle) {
					showHandle = true
				} else {
					kotlinx.coroutines.delay(3000)
					showHandle = false
				}
			}


			val listSize by remember { derivedStateOf {
				groupedMedia.value.size - 1
			}}
			val totalLeftOverItems by remember { derivedStateOf{
				(listSize - gridState.layoutInfo.visibleItemsInfo.size).toFloat()
			}}
			AnimatedVisibility (
				visible = showHandle && !showLoadingSpinner && totalLeftOverItems > 50f,
				modifier = Modifier.fillMaxHeight(1f),
				enter =
					slideInHorizontally { width -> width },
				exit =
					slideOutHorizontally { width -> width }
			) {
				val visibleItemIndex = remember { derivedStateOf { gridState.firstVisibleItemIndex } }
				val percentScrolled = (visibleItemIndex.value / totalLeftOverItems)

				Slider (
					value = percentScrolled,
					interactionSource = interactionSource,
					onValueChange = {
						coroutineScope.launch {
							if (!gridState.isScrollInProgress) {
								gridState.scrollToItem(
									(it * groupedMedia.value.size).roundToInt()
								)
							}
						}
					},
					valueRange = 0f..1f,
					thumb = { state ->
						Box (
							modifier = Modifier
								.height(48.dp)
								.width(96.dp)
						) {
							
							Box (
								modifier = Modifier
									.size(48.dp)
									.clip(RoundedCornerShape(0.dp, 0.dp, 1000.dp, 1000.dp))
									.background(CustomMaterialTheme.colorScheme.secondaryContainer)
									.align(Alignment.Center)
							) {
								Icon (
									painter = painterResource(id = R.drawable.code),
									contentDescription = "scrollbar handle",
									tint = CustomMaterialTheme.colorScheme.onSecondaryContainer,
									modifier = Modifier
										.size(24.dp)
										.align(Alignment.Center)
								)

							}

							Box (
								modifier = Modifier
									.align(Alignment.Center)
									.rotate(-90f)
									.graphicsLayer {
										translationX = -220f
									}
							) {
								AnimatedVisibility(
									visible = isScrollingByHandle,
									enter =
										slideInHorizontally { width -> width / 4 } + fadeIn(),
									exit =
										slideOutHorizontally { width -> width / 4 } + fadeOut(),
									modifier = Modifier
										.align(Alignment.CenterStart)
										.height(32.dp)
										.wrapContentWidth()
								) {
									Box (
										modifier = Modifier
											.height(32.dp)
											.wrapContentWidth()
											.clip(RoundedCornerShape(1000.dp))
											.background(CustomMaterialTheme.colorScheme.secondaryContainer)
											.padding(8.dp, 4.dp)
									) {
										val item = groupedMedia.value[(state.value * listSize).roundToInt()]
										val format = DateTimeFormatter.ofPattern("MMM yyyy")
										val formatted = Instant.ofEpochSecond(item.dateTaken).atZone(ZoneId.systemDefault()).toLocalDateTime().format(format)

										Text(
											text = formatted,
											fontSize = TextUnit(14f, TextUnitType.Sp),
											textAlign = TextAlign.Center,
											color = CustomMaterialTheme.colorScheme.onSecondaryContainer,
											modifier = Modifier
												.align(Alignment.CenterStart)
										)
									}
								}		
							}
						}
					},
					track = {
						val colors = SliderDefaults.colors()
		                SliderDefaults.Track(
		                    sliderState = it,
		                    trackInsideCornerSize = 8.dp,
		                    colors = colors.copy(
		                        activeTickColor = Color.Transparent,
		                        inactiveTickColor = Color.Transparent,
		                        disabledActiveTickColor = Color.Transparent,
		                        disabledInactiveTickColor = Color.Transparent,

		                        activeTrackColor = Color.Transparent,
		                        inactiveTrackColor = Color.Transparent
		                    ),
		                    thumbTrackGapSize = 4.dp,
		                    drawTick = { _, _ -> },
		                    modifier = Modifier
		                        .height(16.dp)
		                )					
					},
					modifier = Modifier
						.width(40.dp)
						.fillMaxHeight(1f)
						.graphicsLayer {
							rotationZ = 90f
							translationX = 30f
							transformOrigin = TransformOrigin(0f, 0f)
						}
						.layout { measurable, constraints ->
							val placeable = measurable.measure(
								Constraints(
									minWidth = constraints.minHeight,
									minHeight = constraints.minWidth,
									maxWidth = constraints.maxHeight,
									maxHeight = constraints.maxWidth
								)
							)

							layout(placeable.height, placeable.width) {
								placeable.place(0, -placeable.height)
							}
						}
				)
			}
		}
	}
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaStoreItem(
	navController: NavHostController,
	item: MediaStoreData,
	operation: ImageFunctions,
	mainViewModel: MainDataSharingModel,
	groupedMedia: List<MediaStoreData>,
	prefix: String,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
	val vibratorManager = rememberVibratorManager()
	val coroutineScope = rememberCoroutineScope()

	if (item.type == MediaType.Section) {
		val isSectionSelected by remember { derivedStateOf {
			selectedItemsList.contains(item)	
		}}

        Box (
            modifier = Modifier
				.fillMaxWidth(1f)
				.height(56.dp)
				.background(Color.Transparent)
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
				) {
					coroutineScope.launch {
						val datedMedia = groupedMedia.filter {
							if (prefix == "Deleted On ") { // find a better way to identify when in trash
								it.getLastModifiedDay() == item.getLastModifiedDay() && it.type != MediaType.Section	
							} else {
								it.getDateTakenDay() == item.getDateTakenDay() && it.type != MediaType.Section	
							}
						}
								
						if (selectedItemsList.containsAll(datedMedia)) {
							selectedItemsList.removeAll(datedMedia)
							selectedItemsList.remove(item)
						} else {
							if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()
							selectedItemsList.addAll(datedMedia)
							selectedItemsList.add(item)
						}
						vibratorManager.vibrateLong()
					}
				}
				.padding(16.dp, 8.dp),
        ) {
            Text (
                text = prefix + item.displayName,
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = CustomMaterialTheme.colorScheme.onBackground,
				modifier = Modifier
					.align(Alignment.CenterStart)
            )

			ShowSelectedState(
				isSelected = isSectionSelected,
				selectedItemsList = selectedItemsList,
				modifier = Modifier
					.align(Alignment.CenterEnd)
			)
        }
    } else {
		val isSelected by remember(selectedItemsList.size) { derivedStateOf { selectedItemsList.contains(item) }}

		val animatedItemCornerRadius by animateDpAsState(
			targetValue = if (isSelected) 16.dp else 0.dp,
	        animationSpec = tween(
	        	durationMillis = 150,
	        ),
			label = "animate corner radius of selected item"
		)
		val animatedItemScale by animateFloatAsState(
			targetValue = if (isSelected) 0.8f else 1f,
	        animationSpec = tween(
	        	durationMillis = 150
	        ),
			label = "animate scale of selected item"
		)    
        Box (
            modifier = Modifier
				.aspectRatio(1f)
				.padding(2.dp)
				.clip(RoundedCornerShape(0.dp))
				.background(CustomMaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
				.combinedClickable(
					onClick = {
						vibratorManager.vibrateShort()
						coroutineScope.launch {
							if (selectedItemsList.size > 0) {
								val sectionItems = groupedMedia.filter {
									if (prefix == "Deleted On ") {
										it.getLastModifiedDay() == item.getLastModifiedDay()
									} else {
										it.getDateTakenDay() == item.getDateTakenDay()
									}								
								}

								val section = sectionItems.first { it.type == MediaType.Section }

								if (isSelected) {
									if (selectedItemsList.contains(section)) selectedItemsList.remove(section)
									selectedItemsList.remove(item)
								} else {
									if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()

									selectedItemsList.add(item)
									
									val allItems = sectionItems.filter { it.type != MediaType.Section }
									if (selectedItemsList.containsAll(allItems)) {
										selectedItemsList.add(section)
									} else {
										selectedItemsList.remove(section)	
									}
								}

								return@launch
							}

							when (operation) {
								ImageFunctions.LoadNormalImage -> {
									mainViewModel.setSelectedMediaData(item)
									mainViewModel.setGroupedMedia(groupedMedia)
									navController.navigate(MultiScreenViewType.SinglePhotoView.name)
								}

								ImageFunctions.LoadTrashedImage -> {
									mainViewModel.setSelectedMediaData(item)
									mainViewModel.setGroupedMedia(groupedMedia)
									navController.navigate(MultiScreenViewType.SingleTrashedPhotoView.name)
								}

								else -> {
									Log.e(
										TAG,
										"No acceptable ImageFunction provided, this should not happen."
									)
								}
							}
						}
					},

					onDoubleClick = { /*ignore double clicks*/ },

					onLongClick = {
						if (selectedItemsList.size > 0) return@combinedClickable

						val sectionItems = groupedMedia.filter {
							if (prefix == "Deleted On ") {
								it.getLastModifiedDay() == item.getLastModifiedDay()
							} else {
								it.getDateTakenDay() == item.getDateTakenDay()
							}
						}
						println("SECTION ITEMS $sectionItems")
						val section = sectionItems.first { it.type == MediaType.Section }

						vibratorManager.vibrateLong()
						if (isSelected) {
							if (selectedItemsList.contains(section)) selectedItemsList.remove(section)
							selectedItemsList.remove(item)
						} else {
							if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()
							selectedItemsList.add(item)

							val allItems = sectionItems.filter { it.type != MediaType.Section }
							if (selectedItemsList.containsAll(allItems)) {
								selectedItemsList.add(section)
							} else {
								selectedItemsList.remove(section)	
							}							
						}
					}
				)
        ) {
            GlideImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
				failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
					.fillMaxSize(1f)
					.align(Alignment.Center)
					.scale(animatedItemScale)
					.clip(RoundedCornerShape(animatedItemCornerRadius))
            ) {
                it.signature(item.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
            }
			

			if (item.type == MediaType.Video) {
				Box (
					modifier = Modifier
						.align(Alignment.BottomStart)
						.padding(2.dp)
				) {			
	        		Icon (
	        			painter = painterResource(id = R.drawable.movie_filled),
						contentDescription = "file is video indicator",
	                    tint = Color.White,
	                    modifier = Modifier
							.size(20.dp)
							.align(Alignment.Center)
	        		)
				}
        	}

			ShowSelectedState(
				isSelected = isSelected,
				selectedItemsList = selectedItemsList,
				modifier = Modifier
					.align(Alignment.TopEnd)
			)
        }
    }
}

@Composable
fun ShowSelectedState(
	isSelected: Boolean,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	modifier: Modifier
) {
	AnimatedVisibility(
		visible = selectedItemsList.size > 0,
		enter =
		scaleIn (
			animationSpec = tween(
				durationMillis = 150
			)
		) + fadeIn(
			animationSpec = tween(
				durationMillis = 150
			)
		),
		exit =
		scaleOut(
			animationSpec = tween(
				durationMillis = 150
			)
		) + fadeOut(
			animationSpec = tween(
				durationMillis = 150
			)
		),
		modifier = Modifier
			.then(modifier)
	) {
		Box (
			modifier = Modifier
				.then(modifier)
				.padding(2.dp)
			// TODO: show "faded out" background to contrast these icons from photo
		) {
			Icon(
				painter = painterResource(id = if (isSelected) R.drawable.file_is_selected_background else R.drawable.file_not_selected_background),
				contentDescription = "file is selected indicator",
				tint =
					if (isSelected)
						CustomMaterialTheme.colorScheme.primary
					else {
						if (isSystemInDarkTheme()) CustomMaterialTheme.colorScheme.onBackground else CustomMaterialTheme.colorScheme.background
					},
				modifier = Modifier
					.size(24.dp)
					.clip(CircleShape)
					.align(Alignment.Center)
			)

			if (isSelected) {
				Icon (
					painter = painterResource(id = R.drawable.file_is_selected_foreground),
					contentDescription = "file is selected indicator",
					tint = CustomMaterialTheme.colorScheme.onPrimary,
					modifier = Modifier
						.size(16.dp)
						.align(Alignment.Center)
				)
			}
		}
	}
}
