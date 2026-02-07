package com.kaii.photos.compose.grids

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.ViewProperties
import com.kaii.photos.compose.widgets.FloatingScrollbar
import com.kaii.photos.compose.widgets.ShowSelectedState
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.compose.widgets.shimmerEffect
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.Behaviour
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.Storage
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.ScreenType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.ImmichInfo
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getThumbnailIv
import com.kaii.photos.mediastore.isRawImage
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val TAG = "com.kaii.photos.compose.grids.PhotoGridView"

@Composable
fun PhotoGrid(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    albumInfo: AlbumInfo,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    modifier: Modifier = Modifier,
    viewProperties: ViewProperties,
    isMediaPicker: Boolean = false,
    isMainPage: Boolean = false,
    state: LazyGridState = rememberLazyGridState(),
    hasFiles: Boolean
) {
    if (hasFiles) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f)
                .then(modifier)
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            DeviceMedia(
                pagingItems = pagingItems,
                selectedItemsList = selectedItemsList,
                viewProperties = viewProperties,
                gridState = state,
                albumInfo = albumInfo,
                isMediaPicker = isMediaPicker,
                isMainPage = isMainPage
            )
        }
    } else {
        val resources = LocalResources.current
        FolderIsEmpty(
            ViewProperties.getText(id = viewProperties.emptyText, resources = resources),
            viewProperties.emptyIconResId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMedia(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    viewProperties: ViewProperties,
    gridState: LazyGridState,
    albumInfo: AlbumInfo,
    isMediaPicker: Boolean,
    isMainPage: Boolean
) {
    val shouldPadUp by remember {
        derivedStateOf {
            selectedItemsList.isNotEmpty()
        }
    }

    BackHandler(
        enabled = selectedItemsList.isNotEmpty()
    ) {
        selectedItemsList.clear()
    }

    val mainViewModel = LocalMainViewModel.current
    val isLandscape by rememberDeviceOrientation()

    Box(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val cacheThumbnails by mainViewModel.settings.Storage.getCacheThumbnails()
            .collectAsStateWithLifecycle(initialValue = false)
        val thumbnailSize by mainViewModel.settings.Storage.getThumbnailSize()
            .collectAsStateWithLifecycle(initialValue = 0)

        val scrollSpeed = remember { mutableFloatStateOf(0f) }
        val isDragSelecting = remember { mutableStateOf(false) }
        val localDensity = LocalDensity.current

        LaunchedEffect(scrollSpeed.floatValue) {
            if (scrollSpeed.floatValue != 0f) {
                while (isActive) {
                    gridState.scrollBy(scrollSpeed.floatValue)
                    delay(10)
                }
            }
        }

        val context = LocalContext.current
        val columnSize by mainViewModel.columnSize.collectAsStateWithLifecycle()
        val useRoundedCorners by mainViewModel.settings.LookAndFeel.getUseRoundedCorners().collectAsStateWithLifecycle(initialValue = false)
        val openVideosExternally by mainViewModel.settings.Behaviour.getOpenVideosExternally().collectAsStateWithLifecycle(initialValue = false)

        // val resources = LocalResources.current
        // val view = LocalView.current
        // val coroutineScope = rememberCoroutineScope()

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(
                if (isLandscape) {
                    columnSize * 2
                } else {
                    columnSize
                }
            ),
            userScrollEnabled = !isDragSelecting.value || selectedItemsList.isEmpty(),
            modifier = Modifier
                .testTag("mainlazycolumn")
                .fillMaxSize(1f)
                .align(Alignment.TopCenter)

            // TODO
            // .dragSelectionHandler(
            //     state = gridState,
            //     selectedItemsList = selectedItemsList,
            //     groupedMedia = groupedMedia,
            //     scrollSpeed = scrollSpeed,
            //     scrollThreshold = with(localDensity) {
            //         40.dp.toPx()
            //     },
            //     isDragSelecting = isDragSelecting,
            //     resources = resources,
            //     view = view,
            //     coroutineScope = coroutineScope,
            //     context = context,
            //     thumbnailSettings = Pair(cacheThumbnails, thumbnailSize),
            // )
        ) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.itemKey() },
                contentType = pagingItems.itemContentType { it::class },
                span = { index ->
                    // TODO: check this
                    if (index < pagingItems.itemCount) {
                        val item = pagingItems[index]

                        if (item is PhotoLibraryUIModel.Section) {
                            GridItemSpan(maxLineSpan)
                        } else {
                            GridItemSpan(1)
                        }
                    } else {
                        GridItemSpan(1)
                    }
                }
            ) { i ->
                val mediaStoreItem = pagingItems[i]

                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateItem()
                ) {
                    val navController = LocalNavController.current

                    if (mediaStoreItem == null) {
                        LoadingMediaStoreItem(useRoundedCorners)
                    } else {
                        MediaStoreItem(
                            item = mediaStoreItem,
                            viewProperties = viewProperties,
                            selectedItemsList = selectedItemsList,
                            thumbnailSettings = Pair(cacheThumbnails, thumbnailSize),
                            isDragSelecting = isDragSelecting,
                            isMediaPicker = isMediaPicker,
                            useRoundedCorners = useRoundedCorners
                        ) {
                            if (!isMediaPicker && mediaStoreItem is PhotoLibraryUIModel.MediaImpl) {
                                val item = mediaStoreItem.item
                                val index =
                                    pagingItems.itemSnapshotList
                                        .filterIsInstance<PhotoLibraryUIModel.MediaImpl>()
                                        .indexOf(pagingItems[i])

                                when (viewProperties) {
                                    ViewProperties.Trash -> {
                                        navController.navigate(
                                            Screens.Trash.SinglePhoto(
                                                index = index
                                            )
                                        )
                                    }

                                    ViewProperties.SecureFolder -> {
                                        navController.navigate(
                                            Screens.SecureFolder.SinglePhoto(
                                                index = index
                                            )
                                        )
                                    }

                                    ViewProperties.Favourites -> {
                                        navController.navigate(
                                            Screens.Favourites.SinglePhoto(
                                                mediaItemId = item.id,
                                                nextMediaItemId = null
                                            )
                                        )
                                    }

                                    ViewProperties.Main -> {
                                        TODO("Implement main grid/single views after moving main pages to own nav views")
                                    }

                                    ViewProperties.Album -> {
                                        navController.navigate(
                                            Screens.Album.SinglePhoto(
                                                albumInfo = albumInfo,
                                                index = index,
                                                nextMediaItemId = null
                                            )
                                        )
                                    }

                                    ViewProperties.Immich -> {
                                        navController.navigate(
                                            Screens.Immich.SinglePhoto(
                                                albumInfo = albumInfo,
                                                index = index,
                                                nextMediaItemId = null
                                            )
                                        )
                                    }

                                    // TODO
                                    else -> {
                                        if (openVideosExternally && item.type == MediaType.Video) {
                                            val intent = Intent().apply {
                                                data = item.uri.toUri() // TODO
                                                action = Intent.ACTION_VIEW
                                            }

                                            context.startActivity(intent)
                                        } else {
                                            navController.navigate(
                                                Screens.SinglePhotoView(
                                                    albumInfo = albumInfo,
                                                    mediaItemId = item.id,
                                                    nextMediaItemId = null,
                                                    type = ScreenType.Search
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (shouldPadUp && !isMainPage) {
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(80.dp)
                    )
                }
            }

            if (isMainPage) {
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(120.dp)
                    )
                }
            }
        }

        val spacerHeight = animateDpAsState(
            targetValue =
                with(localDensity) {
                    WindowInsets.navigationBars.getBottom(localDensity).toDp() +
                            if (isMainPage) 128.dp
                            else if (shouldPadUp) 64.dp
                            else 8.dp
                },
            animationSpec = tween(
                durationMillis = 350,
                delayMillis = if (shouldPadUp) 350 else 0
            ),
            label = "animate spacer on bottom of photo grid"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight(1f)
                .wrapContentWidth()
        ) {
            FloatingScrollbar(
                gridState = gridState,
                spacerHeight = spacerHeight,
                pagingItems = pagingItems
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MediaStoreItem(
    item: PhotoLibraryUIModel,
    viewProperties: ViewProperties,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    thumbnailSettings: Pair<Boolean, Int>,
    isDragSelecting: MutableState<Boolean>,
    isMediaPicker: Boolean,
    useRoundedCorners: Boolean,
    onClick: () -> Unit
) {
    val vibratorManager = rememberVibratorManager()

    if (item is PhotoLibraryUIModel.Section) {
        val isSectionSelected by remember {
            derivedStateOf {
                selectedItemsList.contains(item)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(56.dp)
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    // TODO
                    // if (isSectionSelected) {
                    //     selectedItemsList.unselectSection(
                    //         section = item.section,
                    //         groupedMedia = groupedMedia.value
                    //     )
                    // } else {
                    //     if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData.dummyItem) selectedItemsList.clear()
                    //
                    //     selectedItemsList.selectSection(
                    //         section = item.section,
                    //         groupedMedia = groupedMedia.value
                    //     )
                    // }

                    vibratorManager.vibrateLong()
                }
                .padding(16.dp, 8.dp),
        ) {
            val resources = LocalResources.current
            Text(
                text = (viewProperties.prefix?.let {
                    ViewProperties.getText(
                        id = viewProperties.prefix,
                        resources = resources
                    ) + " "
                } ?: "") + item.title,
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )

            ShowSelectedState(
                isSelected = isSectionSelected,
                showIcon = selectedItemsList.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    } else {
        item as PhotoLibraryUIModel.MediaImpl

        val isSelected by remember {
            derivedStateOf {
                selectedItemsList.contains(item)
            }
        }

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

        val onSingleClick: () -> Unit = {
            vibratorManager.vibrateShort()
            if (selectedItemsList.isNotEmpty() || isMediaPicker) {
                // TODO
                // if (isSelected) {
                //     selectedItemsList.unselectItem(item, groupedMedia.value)
                // } else {
                //     if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData.dummyItem) selectedItemsList.clear()
                //
                //     selectedItemsList.selectItem(item, groupedMedia.value)
                // }
            } else {
                onClick()
            }
        }

        val onLongClick: () -> Unit = {
            isDragSelecting.value = true

            vibratorManager.vibrateLong()

            // TODO
            // if (isSelected) {
            //     selectedItemsList.unselectItem(item, groupedMedia.value)
            // } else {
            //     if (selectedItemsList.size == 1 && selectedItemsList[0] == MediaStoreData()) selectedItemsList.clear()
            //
            //     selectedItemsList.selectItem(item, groupedMedia.value)
            // }
        }

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(if (useRoundedCorners) 8.dp else 0.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                .then(
                    if (selectedItemsList.isNotEmpty()) {
                        Modifier.clickable(
                            enabled = !isDragSelecting.value
                        ) {
                            if (isMediaPicker) {
                                onLongClick()
                            } else {
                                onSingleClick()
                            }
                        }
                    } else {
                        Modifier.combinedClickable(
                            onClick = onSingleClick,

                            onLongClick = onLongClick
                        )
                    }
                )
        ) {
            // TODO: possibly move to a less messy and horrible decrypting implementation
            var model by remember { mutableStateOf<Any?>(null) }
            val context = LocalContext.current
            val isSecureMedia =
                remember(viewProperties) { viewProperties == ViewProperties.SecureFolder }

            LaunchedEffect(isSecureMedia) {
                if (!isSecureMedia || model != null) return@LaunchedEffect

                item as PhotoLibraryUIModel.SecuredMedia

                model =
                    withContext(Dispatchers.IO) {
                        try {
                            val thumbnailIv =
                                item.bytes!!.getThumbnailIv() // get thumbnail iv from video

                            EncryptionManager.decryptBytes(
                                bytes = getSecuredCacheImageForFile(
                                    fileName = item.item.displayName,
                                    context = context
                                ).readBytes(),
                                iv = thumbnailIv
                            )
                        } catch (e: Throwable) {
                            Log.d(TAG, e.toString())
                            e.printStackTrace()

                            item.item.uri.toUri().path
                        }
                    }
            }

            GlideImage(
                model = when {
                    isSecureMedia -> model

                    item.item.immichUrl != null -> ImmichInfo(
                        thumbnail = item.item.immichThumbnail!!,
                        original = item.item.immichUrl!!,
                        hash = item.item.hash!!,
                        accessToken = item.accessToken!!,
                        useThumbnail = true
                    )

                    else -> item.item.uri
                },
                contentDescription = item.item.displayName,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.broken_image),
                modifier = Modifier
                    .fillMaxSize(1f)
                    .align(Alignment.Center)
                    .scale(animatedItemScale)
                    .clip(RoundedCornerShape(animatedItemCornerRadius))
            ) {
                if (isSecureMedia) {
                    it.signature(item.signature())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                } else if (thumbnailSettings.second == 0) {
                    it.signature(item.signature())
                        .diskCacheStrategy(
                            if (thumbnailSettings.first) DiskCacheStrategy.ALL
                            else DiskCacheStrategy.NONE
                        )
                } else {
                    it.signature(item.signature())
                        .diskCacheStrategy(
                            if (thumbnailSettings.first) DiskCacheStrategy.ALL
                            else DiskCacheStrategy.NONE
                        )
                        .override(thumbnailSettings.second)
                }
            }

            if (item.item.type == MediaType.Video) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.movie_filled),
                        contentDescription = stringResource(id = R.string.file_is_a_video),
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            if (item.item.isRawImage()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.raw_on),
                        contentDescription = stringResource(id = R.string.media_is_raw),
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            ShowSelectedState(
                isSelected = isSelected,
                showIcon = selectedItemsList.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun LoadingMediaStoreItem(
    useRoundedCorners: Boolean
) {
    var showColors by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(10) // to avoid a pop-in effect
        showColors = true
    }

    val highlightColor by animateColorAsState(
        targetValue = if (showColors) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(
            durationMillis = 0
        )
    )

    // TODO
    // if (item is PhotoLibraryUIModel.Separator) {
    //     Box(
    //         modifier = Modifier
    //             .fillMaxWidth(1f)
    //             .height(56.dp)
    //             .background(Color.Transparent)
    //             .padding(16.dp, 8.dp),
    //     ) {
    //         Box(
    //             modifier = Modifier
    //                 .align(Alignment.CenterStart)
    //                 .fillMaxWidth(Random.nextFloat() * 0.5f + 0.5f)
    //                 .height(24.dp)
    //                 .clip(CircleShape)
    //                 .shimmerEffect(
    //                     containerColor = Color.Transparent,
    //                     highlightColor = highlightColor,
    //                     durationMillis = AnimationConstants.DURATION_EXTRA_LONG * 3,
    //                     delayMillis = -PhotoGridConstants.UPDATE_TIME.toInt() * 2
    //                 )
    //         )
    //     }
    // } else {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(if (useRoundedCorners) 8.dp else 0.dp))
            .shimmerEffect(
                containerColor = Color.Transparent,
                highlightColor = highlightColor,
                durationMillis = AnimationConstants.DURATION_EXTRA_LONG * 2
            )
    )
    // }
}

// private fun Modifier.dragSelectionHandler(
//     state: LazyGridState,
//     selectedItemsList: SnapshotStateList<MediaStoreData>,
//     groupedMedia: State<List<MediaStoreData>>,
//     scrollSpeed: MutableFloatState,
//     scrollThreshold: Float,
//     isDragSelecting: MutableState<Boolean>,
//     resources: Resources,
//     view: View,
//     coroutineScope: CoroutineScope,
//     context: Context,
//     thumbnailSettings: Pair<Boolean, Int>
// ) = pointerInput(groupedMedia.value) {
//     var initialKey: Int? = null
//     var currentKey: Int? = null
//     var isDragAndDropping = false
//
//     if (groupedMedia.value.isEmpty()) return@pointerInput
//
//     val itemWidth = state.layoutInfo.visibleItemsInfo.firstOrNull {
//         if (it.index in groupedMedia.value.indices) groupedMedia.value[it.index].type != MediaType.Section else false
//     }?.size?.width
//
//     val numberOfHorizontalItems = itemWidth?.let { state.layoutInfo.viewportSize.width / it } ?: 1
//
//     Log.d(TAG, "grid displays $numberOfHorizontalItems horizontal items")
//
//     detectDragGesturesAfterLongPress(
//         onDragStart = { offset ->
//             isDragSelecting.value = true
//
//             if (selectedItemsList.isNotEmpty()) {
//                 state.getGridItemAtOffset(
//                     offset,
//                     groupedMedia.value.fastMap { it.itemKey() },
//                     numberOfHorizontalItems
//                 )?.let { key ->
//                     val item = groupedMedia.value[key]
//
//                     if (item.type != MediaType.Section) {
//                         if (selectedItemsList.contains(item) && selectedItemsList.filter { it.type != MediaType.Section }.size != 1) {
//                             isDragAndDropping = true
//                             coroutineScope.launch(Dispatchers.IO) {
//                                 val items = selectedItemsList.filter { it.type != MediaType.Section }
//
//                                 val clipData = ClipData.newUri(
//                                     context.contentResolver,
//                                     resources.getString(R.string.drag_and_drop_data),
//                                     items.first().uri.toUri()
//                                 )
//
//                                 items.drop(1).forEach {
//                                     clipData.addItem(ClipData.Item(it.uri))
//                                 }
//
//                                 val bitmaps = items.take(3).map { // limit to 3 so we don't overstress the rendering/loading of bitmaps
//                                     Glide.with(context)
//                                         .asBitmap()
//                                         .override(thumbnailSettings.second)
//                                         .centerCrop()
//                                         .load(it.uri)
//                                         .submit()
//                                         .get()
//                                 }
//
//                                 val shadow = BitmapUriShadowBuilder(
//                                     view = view,
//                                     bitmaps = bitmaps,
//                                     count = items.size,
//                                     density = Density(density)
//                                 )
//
//                                 view.startDragAndDrop(
//                                     clipData,
//                                     shadow,
//                                     clipData,
//                                     View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE
//                                 )
//                             }
//                         } else {
//                             isDragAndDropping = false
//                             initialKey = key
//                             currentKey = key
//
//                             // TODO
//                             // if (!selectedItemsList.contains(item)) selectedItemsList.selectItem(
//                             //     item = item,
//                             //     groupedMedia = groupedMedia.value
//                             // )
//                         }
//                     }
//                 }
//             }
//         },
//
//         onDragCancel = {
//             initialKey = null
//             scrollSpeed.floatValue = 0f
//             isDragSelecting.value = false
//         },
//
//         onDragEnd = {
//             initialKey = null
//             scrollSpeed.floatValue = 0f
//             isDragSelecting.value = false
//         },
//
//         onDrag = { change, _ ->
//             if (initialKey != null && !isDragAndDropping) {
//                 val distanceFromBottom = state.layoutInfo.viewportSize.height - change.position.y
//                 val distanceFromTop = change.position.y // for clarity
//
//                 scrollSpeed.floatValue = when {
//                     distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
//                     distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
//                     else -> 0f
//                 }
//
//                 state.getGridItemAtOffset(
//                     change.position,
//                     groupedMedia.value.fastMap { it.itemKey() },
//                     numberOfHorizontalItems
//                 )?.let { key ->
//                     if (currentKey != key) {
//                         selectedItemsList.apply {
//                             // TODO
//                             // val toBeRemoved =
//                             //     if (initialKey!! <= currentKey!!) groupedMedia.value.subList(
//                             //         initialKey!!,
//                             //         currentKey!! + 1
//                             //     )
//                             //     else groupedMedia.value.subList(currentKey!!, initialKey!! + 1)
//                             //
//                             // unselectAll(
//                             //     items = toBeRemoved.filter {
//                             //         it.type != MediaType.Section
//                             //     },
//                             //     groupedMedia = groupedMedia.value
//                             // )
//                             //
//                             // val toBeAdded =
//                             //     if (initialKey!! <= key) groupedMedia.value.subList(initialKey!!, key + 1)
//                             //     else groupedMedia.value.subList(key, initialKey!! + 1)
//                             //
//                             // selectAll(
//                             //     items = toBeAdded.filter {
//                             //         it.type != MediaType.Section
//                             //     },
//                             //     groupedMedia = groupedMedia.value
//                             // )
//                         }
//
//                         currentKey = key
//                     }
//                 }
//             }
//         }
//     )
// }

// @Suppress("UNCHECKED_CAST")
//         /** make sure [T] is the same type as state keys */
// fun <T : Any> LazyGridState.getGridItemAtOffset(
//     offset: Offset,
//     keys: List<T>,
//     numberOfHorizontalItems: Int
// ): Int? {
//     var key: T? = null
//
//     // scan the entire row for this item
//     // if theres only one or two items on a row and user drag selects to the empty space they get selected
//     for (i in 1..numberOfHorizontalItems) {
//         val possibleItem = layoutInfo.visibleItemsInfo.find { item ->
//             val stretched = item.size.toIntRect().let {
//                 IntRect(
//                     top = it.top,
//                     bottom = it.bottom,
//                     left = it.left,
//                     right = it.right * i
//                 )
//             }
//
//             stretched.contains(offset.round() - item.offset)
//         }
//
//         if (possibleItem != null) {
//             key = possibleItem.key as T
//             break
//         }
//     }
//
//     val found = keys.find {
//         it == key
//     } ?: return null
//
//     return keys.indexOf(found)
// }

