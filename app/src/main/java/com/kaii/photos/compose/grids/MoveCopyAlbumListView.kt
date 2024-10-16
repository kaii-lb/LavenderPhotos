package com.kaii.photos.compose.grids

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.R
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.getDefaultShapeSpacerForPosition
import com.kaii.photos.datastore
import com.kaii.photos.datastore.getAlbumsList
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.operateOnImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveCopyAlbumListView(
    show: MutableState<Boolean>,
    selectedItemsWithoutSection: List<MediaStoreData>,
    isMoving: Boolean
) {
    val context = LocalContext.current
    val originalAlbumsList = runBlocking {
        context.datastore.getAlbumsList()
    }

    val albumsViewModel: AlbumsViewModel = viewModel(
        factory = AlbumsViewModelFactory(context, originalAlbumsList)
    )
    val dataList = albumsViewModel.mediaStoreData.collectAsState()

    var albumsList by remember { mutableStateOf(originalAlbumsList) }

    val searchedForText = remember { mutableStateOf("") }

	val state = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = searchedForText.value) {
        albumsList = originalAlbumsList.filter {
            it.contains(searchedForText.value, true)
        }
    	if (albumsList.size != 0) state.scrollToItem(0)	    
    }

	LaunchedEffect(show.value) {
		searchedForText.value = ""
	}

    if (show.value) {
        ModalBottomSheet(
            sheetState = sheetState,
            tonalElevation = 16.dp,
            shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false
            ),
            containerColor = CustomMaterialTheme.colorScheme.background,
            onDismissRequest = { show.value = false },
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.statusBars
                ),
        ) {
			BackHandler (
				enabled = show.value == true && !WindowInsets.isImeVisible
			) {
				coroutineScope.launch {
					sheetState.hide()
					show.value = false
				}
			}
        
            AnimatedVisibility (
                visible = sheetState.currentValue == SheetValue.Expanded,
                enter = expandVertically (
                    expandFrom = Alignment.Top
                ) + fadeIn(),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top
                ) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth(1f)
            ) {
                SearchTextField(
                	searchedForText = searchedForText, 
                	placeholder = "Search for an album's name",
                	modifier = Modifier
       		            .fillMaxWidth(1f)
            			.height(56.dp)
            			.padding(16.dp, 0.dp),
           			onClear = {
           				searchedForText.value = ""
           			},
           			onSearch = {}
               	)

               	Spacer (modifier = Modifier.height(16.dp))
            }

            if (albumsList.isEmpty()) {
                FolderIsEmpty(
                	emptyText = "No such albums exists", 
                	emptyIconResId = R.drawable.error,
                	backgroundColor = Color.Transparent
               	)
            } else {
                LazyColumn (
                    state = state,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .padding(8.dp, 8.dp, 8.dp, 0.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    items(
                        count = albumsList.size,
                        key = {
                            albumsList[it]
                        }
                    ) {
                        val album = albumsList[it]
                        AlbumsListItem(
                            album = album,
                            data = dataList.value[album] ?: MediaStoreData(),
                            position = if (it == albumsList.size - 1 && albumsList.size != 1) RowPosition.Bottom else if (albumsList.size == 1) RowPosition.Single else if (it == 0) RowPosition.Top else RowPosition.Middle,
                            selectedItemsList = selectedItemsWithoutSection,
                            isMoving = isMoving,
                            show = show,
                            modifier = Modifier
                                .fillParentMaxWidth(1f)
                                .padding(8.dp, 0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    searchedForText: MutableState<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
	Row (
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceEvenly
	) {
	    val keyboardController = LocalSoftwareKeyboardController.current
	    
	    TextField(
	        value = searchedForText.value,
	        onValueChange = {
	            searchedForText.value = it
	        },
	        maxLines = 1,
	        singleLine = true,
	        placeholder = {
	            Text(
	                text = placeholder,
	                fontSize = TextUnit(16f, TextUnitType.Sp)
	            )
	        },
	        prefix = {
	        	Row {
		            Icon(
		                painter = painterResource(id = R.drawable.search),
		                contentDescription = "Search Icon",
		                   modifier = Modifier
		                       .size(24.dp)
		            )
	        		
	        		Spacer (modifier = Modifier.width(8.dp))
	        	}
	        },
	        colors = TextFieldDefaults.colors(
	            focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
	            unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
	            cursorColor = CustomMaterialTheme.colorScheme.primary,
	            focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
	            unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
	            focusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
	            unfocusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
	            unfocusedIndicatorColor = Color.Transparent,
	            focusedIndicatorColor = Color.Transparent
	        ),
	        keyboardOptions = KeyboardOptions(
	            autoCorrectEnabled = false,
	            keyboardType = KeyboardType.Text,
	            imeAction = ImeAction.Search
	        ),
	        keyboardActions = KeyboardActions(
	            onSearch = {
	                onSearch()
	                keyboardController?.hide()
	            }
	        ),
	        shape = RoundedCornerShape(1000.dp, 0.dp, 0.dp, 1000.dp),
	        modifier = Modifier
	        	.weight(1f)
	    )

	    Row(
	    	modifier = Modifier
	    		.height(56.dp)
	    		.width(32.dp)
	    		.clip(RoundedCornerShape(0.dp, 1000.dp, 1000.dp, 0.dp))
	    		.background(CustomMaterialTheme.colorScheme.surfaceContainer)
	    		.weight(0.2f),
    		verticalAlignment = Alignment.CenterVertically,
    		horizontalArrangement = Arrangement.Center
	    ) {
	    	Row (
	    		modifier = Modifier
	    			.size(36.dp)
	    			.clip(RoundedCornerShape(1000.dp))
		    		.clickable {
		    			onClear()
		    		},
	    		verticalAlignment = Alignment.CenterVertically,
	    		horizontalArrangement = Arrangement.Center
	    	) {
		    	Icon(
		    		painter = painterResource(id = R.drawable.close),
		    		contentDescription = "Clear search query",
		    		tint = CustomMaterialTheme.colorScheme.onSurface,
		    		modifier = Modifier
		    			.size(24.dp)
		    	)
	    	}
	    }
	}
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlbumsListItem(
    album: String,
    data: MediaStoreData,
    position: RowPosition,
    modifier: Modifier,
    selectedItemsList: List<MediaStoreData>,
    isMoving: Boolean,
    show: MutableState<Boolean>
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, 24.dp)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Row (
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(CustomMaterialTheme.colorScheme.surfaceContainer)
            .clickable {
            	show.value = false
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        if (isMoving) {
                            selectedItemsList.forEach {
                                operateOnImage(
                                    it.absolutePath,
                                    it.id,
                                    ImageFunctions.MoveImage,
                                    context,
                                    mapOf(
                                        Pair("albumPath", album)
                                    )
                                )
                            }
                        } else {
                            selectedItemsList.forEach {
                                operateOnImage(
                                    it.absolutePath,
                                    it.id,
                                    ImageFunctions.CopyImage,
                                    context,
                                    mapOf(
                                        Pair("albumPath", album)
                                    )
                                )
                            }
                        }
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
		Spacer (modifier = Modifier.width(12.dp))
    
        GlideImage(
            model = data.uri,
            contentDescription = album,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.broken_image),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer (modifier = Modifier.width(16.dp))

        Text(
            text = album.split("/").last(),
            fontSize = TextUnit(16f, TextUnitType.Sp),
            textAlign = TextAlign.Start,
            color = CustomMaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
        )
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(CustomMaterialTheme.colorScheme.surface)
    )
}
