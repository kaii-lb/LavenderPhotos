package com.kaii.photos.compose.single_photo

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EditingView(uri: Uri, showEditingView: MutableState<Boolean> = remember { mutableStateOf(false) }) {
    val pagerState = rememberPagerState {
        4
    }

    BackHandler (
        enabled = showEditingView.value
    ) {
        showEditingView.value = false
    }

    Scaffold (
        topBar = {
            EditingViewTopBar(showEditingView)
        },
        bottomBar = {
            EditingViewBottomBar(pagerState)
        },
        modifier = Modifier
            .fillMaxSize(1f)
    ) { innerPadding ->
        BoxWithConstraints (
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(1f)
        ) {
        	var initialLoad by remember { mutableStateOf(false) }

			LaunchedEffect(Unit) {
				delay(500)
				initialLoad = true
			}

            val animatedSize by animateFloatAsState(
            	targetValue = if (pagerState.currentPage == 0 && initialLoad) 0.8f else 1f,
                label = "Animate size of preview image in crop mode"
            )

            GlideImage(
                model = uri,
                contentDescription = "Image editing view",
                modifier = Modifier
                    .height(maxHeight * animatedSize)
                    .width(maxWidth * animatedSize)
                    .align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewTopBar(showEditingView: MutableState<Boolean>) {
    TopAppBar(
        title = {},
        navigationIcon = {
        	Box (
        		modifier = Modifier
                    .height(40.dp)
                    .width(72.dp)
                    .padding(8.dp, 0.dp, 0.dp, 0.dp)
        	) {
	            Column (
	                modifier = Modifier
                        .height(40.dp)
                        .width(72.dp)
                        .clip(RoundedCornerShape(1000.dp))
                        .background(CustomMaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            showEditingView.value = false
                        },
	                verticalArrangement = Arrangement.Center,
	                horizontalAlignment = Alignment.CenterHorizontally
	            ) {
	                Icon(
	                    painter = painterResource(id = R.drawable.close),
	                    contentDescription = "Close currently visible editing view",
	                    tint = CustomMaterialTheme.colorScheme.onSecondaryContainer
	                )
	            }
        	}
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingViewBottomBar(pagerState: PagerState) {
    Box (
        modifier = Modifier
            .background(CustomMaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth(1f)
            .height(180.dp)
            .padding(0.dp, 0.dp, 0.dp, 40.dp)
    ) {
        val coroutineScope = rememberCoroutineScope()

        Column (
            modifier = Modifier
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                divider = {},
                indicator = { tabPosition ->
                    Box (
                        modifier = Modifier
                            .tabIndicatorOffset(tabPosition[pagerState.currentPage])
                            .padding(4.dp)
                            .fillMaxHeight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(CustomMaterialTheme.colorScheme.primary)
                            .zIndex(1f)
                    )
                },
		        containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
		        contentColor = CustomMaterialTheme.colorScheme.onSurface,
		        modifier = Modifier
                    .clip(RoundedCornerShape(1000.dp))
                    .draggable(
                        state = rememberDraggableState { delta ->
                            if (delta > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1).coerceAtMost(
                                            3
                                        )
                                    )
                                }
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage - 1).coerceAtLeast(
                                            0
                                        )
                                    )
                                }
                            }
                        },
                        orientation = Orientation.Horizontal
                    )
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(100.dp))
                ) {
                    Text(
                    	text = "Crop",
                    	color = if (pagerState.currentPage == 0) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface
                   	)
                }

                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(100.dp))
                ) {
                    Text(
                    	text = "Adjust",
                    	color = if (pagerState.currentPage == 1) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface
                   	)
                }

                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(100.dp))
                ) {
                    Text(
                    	text = "Filters",
                    	color = if (pagerState.currentPage == 2) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface
                   	)
                }

                Tab(
                    selected = pagerState.currentPage == 3,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(3)
                        }
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(100.dp))
                ) {
                    Text(
                    	text = "Draw",
                    	color = if (pagerState.currentPage == 3) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface
                   	)
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                snapPosition = SnapPosition.Center,
                pageSize = PageSize.Fill
            ) { index ->
                when (index) {
                    0 -> {
                        CropTools()
                    }

                    1 -> {
                        AdjustTools()
                    }

                    2 -> {
                        FiltersTools()
                    }

                    3 -> {
                        DrawTools()
                    }
                }
            }
        }
    }
}

@Composable
fun CropTools() {
    Row (
       modifier = Modifier
           .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Rotate", iconResId = R.drawable.rotate_ccw)
        EditingViewBottomAppBarItem(text = "Ratio", iconResId = R.drawable.resolution)
        EditingViewBottomAppBarItem(text = "Reset", iconResId = R.drawable.reset)
    }
}

@Composable
fun AdjustTools() {
    LazyRow (
    	verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        item {
            EditingViewBottomAppBarItem(text = "Contrast", iconResId = R.drawable.contrast)
        }

        item {
            EditingViewBottomAppBarItem(text = "Brightness", iconResId = R.drawable.palette)
        }

        item {
            EditingViewBottomAppBarItem(text = "Saturation", iconResId = R.drawable.resolution)
        }

        item {
            EditingViewBottomAppBarItem(text = "Black Point", iconResId = R.drawable.file_is_selected_background)
        }

        item {
            EditingViewBottomAppBarItem(text = "White Point", iconResId = R.drawable.file_not_selected_background)
        }

        item {
            EditingViewBottomAppBarItem(text = "Shadows", iconResId = R.drawable.shadow)
        }

        item {
            EditingViewBottomAppBarItem(text = "Warmth", iconResId = R.drawable.skillet)
        }

        item {
            EditingViewBottomAppBarItem(text = "Tint", iconResId = R.drawable.colors)
        }
    }
}

@Composable
fun FiltersTools() {
    LazyRow (
    	verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize(1f)
    ) {
        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }

        item {
            EditingViewBottomAppBarItem(text = "Filter", iconResId = R.drawable.style)
        }
    }
}

@Composable
fun DrawTools() {
    Row (
        modifier = Modifier
            .fillMaxSize(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditingViewBottomAppBarItem(text = "Pencil", iconResId = R.drawable.pencil)
        EditingViewBottomAppBarItem(text = "Highlighter", iconResId = R.drawable.highlighter)
        EditingViewBottomAppBarItem(text = "Text", iconResId = R.drawable.text)
    }
}

@Composable
fun EditingViewBottomAppBarItem(
	text: String,
	iconResId: Int
) {
	BottomAppBarItem(
       	text = text,
       	iconResId = iconResId,
       	buttonWidth = 84.dp,
       	buttonHeight = 56.dp,
       	cornerRadius = 8.dp
	)
}
