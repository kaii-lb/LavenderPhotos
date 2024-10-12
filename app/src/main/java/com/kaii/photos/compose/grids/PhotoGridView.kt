package com.kaii.photos.compose.grids

import android.content.res.Configuration
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.FolderDoesntExist
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.main_activity.MainDataSharingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.path.Path
import kotlin.math.roundToInt

private const val THUMBNAIL_DIMENSION = 50f
private const val TAG = "PHOTO_GRID_VIEW"

private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION, THUMBNAIL_DIMENSION)

@Composable
fun PhotoGrid(
	navController: NavHostController,
	operation: ImageFunctions,
	path: String,
	sortBy: MediaItemSortMode,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	emptyText: String = "Empty Folder",
	prefix: String = "",
	shouldPadUp: Boolean = false
) {
	val hasFiles = Path("/storage/emulated/0/$path").checkHasFiles()

	if (hasFiles == null) {
		FolderDoesntExist()
		return
	}
	 
	if (hasFiles) {
		DeviceMedia(
			navController,
			path,
			operation,
			selectedItemsList,
			sortBy,
			prefix,
			shouldPadUp
		)
	} else {
		FolderIsEmpty(emptyText)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMedia(
	navController: NavHostController,
	path: String,
	operation: ImageFunctions,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	sortBy: MediaItemSortMode,
	prefix: String,
	shouldPadUp: Boolean
) {
	val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(LocalContext.current, path, sortBy)
	)
//	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

	val mediaStoreData = galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

	val mainViewModel = MainActivity.mainViewModel

	val groupedMedia by remember(mediaStoreData.value) { derivedStateOf {
		mediaStoreData.value
	}}
	mainViewModel.setGroupedMedia(groupedMedia)

	val gridState = rememberLazyGridState()
	var showLoadingSpinner by remember { mutableStateOf(true) }

	val stops by remember { derivedStateOf {
		var lastMonth = 0L
		groupedMedia.filter {
			val itsMonth = it.getDateTakenMonth()
			val shouldAdd = it.type == MediaType.Section && itsMonth != lastMonth
			lastMonth = itsMonth

			shouldAdd
		}
	}}

	val numberOfStops by remember { derivedStateOf {
		(stops.size - 1).coerceAtLeast(1)
	}}
	val coroutineScope = rememberCoroutineScope()

    val requestBuilderTransform =
        { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
        }

    val preloadingData =
        rememberGlidePreloadingData(
            groupedMedia,
            THUMBNAIL_SIZE,
            requestBuilderTransform = requestBuilderTransform,
        )
        	
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
	            count = preloadingData.size,
	            key = {
	                groupedMedia[it].uri.toString()
	            },
	            span = { index ->
	                val item = groupedMedia[index]
	                if (item.type == MediaType.Section) {
	                    GridItemSpan(maxLineSpan)
	                } else {
	                    GridItemSpan(1)
	                }
	            }
	        ) { i ->
	            val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]

				Row (
					modifier = Modifier
						.wrapContentSize()
						.animateItem()
				) {
					MediaStoreItem(
						navController,
						mediaStoreItem,
						preloadRequestBuilder,
						operation,
						mainViewModel,
						groupedMedia,
						prefix,
						selectedItemsList
					)
				}

	            if (i >= 0) {
					val handler = Handler(Looper.getMainLooper())
					val runnable = Runnable {
		                showLoadingSpinner = false	
		            }
		            handler.removeCallbacks(runnable)
		            handler.postDelayed(runnable, 500)
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
				.padding(0.dp, 16.dp)
		) {
			var currentStop by remember { mutableIntStateOf(0) }
			var isScrollingByHandle by remember { mutableStateOf(false) }

			val currentIndex by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
			val interactionSource = remember { MutableInteractionSource() }
			LaunchedEffect(currentIndex) {
				if (groupedMedia.isNotEmpty()) {
					val possibleSection = groupedMedia[currentIndex]
					if (possibleSection.type == MediaType.Section && !isScrollingByHandle) {
						val first = stops.filter { it.getDateTakenMonth() == possibleSection.getDateTakenMonth() }.firstOrNull()
						currentStop = stops.indexOf(first)
					}
				}
			}

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

			var showHandle by remember { mutableStateOf(false) }
			LaunchedEffect(key1 = gridState.isScrollInProgress, key2 = isScrollingByHandle) {
				if (gridState.isScrollInProgress || isScrollingByHandle) {
					showHandle = true
				} else {
					kotlinx.coroutines.delay(3000)
					showHandle = false
				}
			}
			
			AnimatedVisibility (
				visible = showHandle,
				modifier = Modifier.fillMaxHeight(1f),
				enter =
					slideInHorizontally(
						
					) { width -> width },
				exit =
					slideOutHorizontally(
						
					) { width -> width }
			) {
				Slider (
					value = currentStop.toFloat(),
					interactionSource = interactionSource,
					onValueChange = {
						currentStop = it.roundToInt()
						coroutineScope.launch {
							if (stops.isNotEmpty()) {
								val index = (currentStop - 1).coerceIn(0, numberOfStops)
								gridState.scrollToItem(
									groupedMedia.indexOf(stops[index])
								)
							}
						}
					},
					steps = numberOfStops,
					valueRange = 1f..numberOfStops.toFloat(),
					thumb = {
						Box (
							modifier = Modifier
								.size(48.dp)
								.clip(RoundedCornerShape(0.dp, 0.dp, 1000.dp, 1000.dp))
								.background(CustomMaterialTheme.colorScheme.secondaryContainer)
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
	preloadRequestBuilder: RequestBuilder<Drawable>,
	operation: ImageFunctions,
	mainViewModel: MainDataSharingModel,
	groupedMedia: List<MediaStoreData>,
	prefix: String,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
	val isSelected by remember(selectedItemsList.size) { derivedStateOf { selectedItemsList.contains(item) }}
	val vibratorManager = rememberVibratorManager()

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

	BackHandler (
		enabled = selectedItemsList.size > 0
	) {
		selectedItemsList.clear()
	}

	if (item.mimeType == null && item.type == MediaType.Section) {
		val datedMedia = groupedMedia.filter {
			if (prefix == "Deleted On ") { // find a better way to identify when in trash
				it.getLastModifiedDay() == item.getLastModifiedDay() && it.type != MediaType.Section	
			} else {
				it.getDateTakenDay() == item.getDateTakenDay() && it.type != MediaType.Section	
			}
		}
		val sectionSelected by remember { derivedStateOf {
			selectedItemsList.containsAll(datedMedia)
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
					if (selectedItemsList.containsAll(datedMedia)) {
						selectedItemsList.removeAll(datedMedia)
					} else {
						if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()
						selectedItemsList.addAll(datedMedia)
					}
					vibratorManager.vibrateLong()
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
				isSelected = sectionSelected,
				selectedItemsList = selectedItemsList,
				modifier = Modifier
					.align(Alignment.CenterEnd)
			)
        }
    } else {
        Box (
            modifier = Modifier
				.aspectRatio(1f)
				.padding(2.dp)
				.clip(RoundedCornerShape(0.dp))
				.background(CustomMaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
				.combinedClickable(
					onClick = {
						if (selectedItemsList.size > 0) {
							vibratorManager.vibrateShort()
							if (isSelected) {
								selectedItemsList.remove(item)
							} else {
								if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()
								selectedItemsList.add(item)
							}
							return@combinedClickable
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
					},

					onDoubleClick = { /*ignore double clicks*/ },

					onLongClick = {
						if (selectedItemsList.size > 0) return@combinedClickable

						vibratorManager.vibrateLong()
						if (isSelected) {
							selectedItemsList.remove(item)
						} else {
							if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()
							selectedItemsList.add(item)
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
                it.thumbnail(preloadRequestBuilder).signature(item.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
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
