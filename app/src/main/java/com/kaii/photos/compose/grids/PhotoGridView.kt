package com.kaii.photos.compose.grids

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
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
import com.kaii.photos.compose.IsSelectingBottomAppBar
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.checkHasFiles
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.main_activity.MainDataSharingModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.io.path.Path
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val THUMBNAIL_DIMENSION = 50
private const val TAG = "PHOTO_GRID_VIEW"

private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION.toFloat(), THUMBNAIL_DIMENSION.toFloat())

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
	val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(LocalContext.current, path)
	)
		
	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

	val mainViewModel = MainActivity.mainViewModel

	val hasFiles = Path("/storage/emulated/0/$path").checkHasFiles()

	if (hasFiles == null) {
		FolderDoesntExist()
		return
	}
	 
	if (hasFiles) {
		DeviceMedia(
			navController,
			mediaStoreData,
			operation,
			mainViewModel,
			selectedItemsList,
			sortBy,
			prefix,
			shouldPadUp
		)
	} else {
		FolderIsEmpty(emptyText)
	}
}

@Composable
fun DeviceMedia(
	navController: NavHostController,
	mediaStoreData: State<List<MediaStoreData>>,
	operation: ImageFunctions,
	mainViewModel: MainDataSharingModel,
	selectedItemsList: SnapshotStateList<MediaStoreData>,
	sortBy: MediaItemSortMode,
	prefix: String,
	shouldPadUp: Boolean
) {
	val groupedMedia by remember { derivedStateOf {
		groupPhotosBy(mediaStoreData.value, sortBy) 	
	}}
	mainViewModel.setGroupedMedia(groupedMedia)
	
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

	val gridState = rememberLazyGridState()
	var showLoadingSpinner by remember { mutableStateOf(true) }
	
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
	                // println("URI STRING ${mediaStoreData[it].uri}")
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
			// println("SPINNING STARTED AT ${LocalTime.now()}")
			
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
			val stops by remember { derivedStateOf {
				groupedMedia.filter {
					it.type == MediaType.Section
				}
			}}

			var currentStop by remember { mutableIntStateOf(0) }
		
			// var startFading by remember { mutableStateOf(true) }
			// val alpha by animateFloatAsState(
			// 	targetValue = if (!startFading) 1f else 0f,
			// 	label = "animate alpha of scrollbar"
			// )

			LaunchedEffect(key1 = currentStop) {
				if (stops.size > 0) {
					val index = (currentStop - 1).coerceIn(0, stops.size)
					gridState.scrollToItem(
						groupedMedia.indexOf(stops[index])
					)
				}

				// if (!gridState.isScrollInProgress) {
				// 	delay(3000)
				// 	startFading = true
				// } else {
				// 	startFading = false
				// }
			}

			Slider (
				value = currentStop.toFloat(),
				onValueChange = {
					currentStop = it.roundToInt()
				},
				steps = stops.size,
				valueRange = 0f..stops.size.toFloat(),
				modifier = Modifier
					.width(48.dp)
					.fillMaxHeight(1f)
					.graphicsLayer {
						rotationZ = 90f
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
					// .alpha(alpha)
			)
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
	val isSelected by remember { derivedStateOf { selectedItemsList.contains(item) } }
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
						selectedItemsList.addAll(datedMedia)
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
							if (isSelected) {
								selectedItemsList.remove(item)
							} else {
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
						if (isSelected) {
							selectedItemsList.remove(item)
						} else {
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
					.clip(RoundedCornerShape(animatedItemCornerRadius)),
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
