package com.kaii.photos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.with
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.AlbumGridItem
import com.kaii.photos.compose.AlbumGridView
import com.kaii.photos.compose.PhotoGrid
import com.kaii.photos.helpers.ComposeViewType
import com.kaii.photos.ui.theme.PhotosTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_READ_STORAGE = 0
        private val PERMISSIONS_REQUEST =
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)
        if (PERMISSIONS_REQUEST.any {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            requestStoragePermission()
        } else {
            setContentForActivity()
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this, PERMISSIONS_REQUEST, REQUEST_READ_STORAGE
        )
    }

    private fun setContentForActivity() {
        setContent {
            PhotosTheme {
				enableEdgeToEdge(
					navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb())
				)        
                Content()
            }
        }
    }

	@OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun Content() {
    	var currentView = remember { mutableStateOf(ComposeViewType.PhotoGridView) }

        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar()
            },
            bottomBar = { BottomBar(currentView) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(0.dp, padding.calculateTopPadding(), 0.dp, padding.calculateBottomPadding())
            ) {
                Column(
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                	AnimatedContent(
                		targetState = currentView.value,
                		transitionSpec = {
                			if (targetState.index > initialState.index ) {
	                			slideInVertically { height -> height } + fadeIn() with 
	                				slideOutVertically { height -> -height } + fadeOut()
                			} else {
	                			slideInVertically { height -> -height } + fadeIn() with 
	                				slideOutVertically { height -> height } + fadeOut()
                			}.using(
                				SizeTransform(clip = false)
                			)
                		}	
                	) {
	                    when (currentView.value) {
	                        ComposeViewType.PhotoGridView -> PhotoGrid()
	                        ComposeViewType.LockedFolder -> PhotoGrid()
	                        ComposeViewType.AlbumGridView -> AlbumGridView()
   	                        ComposeViewType.SearchPage -> AlbumGridView()
	                    }
                	}
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar() {
        TopAppBar(
            title = {
                Row {
                    Text(
                        text = "Lavender ",
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(22f, TextUnitType.Sp)
                    )
                    Text(
                        text = "Photos",
                        fontWeight = FontWeight.Normal,
                        fontSize = TextUnit(22f, TextUnitType.Sp)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { /*TODO*/ },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings Button",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        )
    }

    @Preview
    @Composable
    private fun BottomBar(currentView: MutableState<ComposeViewType>) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            contentPadding = PaddingValues(0.dp),
        ) {
            val buttonHeight = 56.dp
            val buttonWidth = 64.dp
            val iconSize = 24.dp
            val textSize = 14f

            Row(
                modifier = Modifier
                    .fillMaxSize(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
            	// should find a better way
            	val unselectedColor = MaterialTheme.colorScheme.surfaceContainer
            	val selectedColor = MaterialTheme.colorScheme.secondaryContainer
            	var photoGridColor by remember { mutableStateOf(selectedColor) }
            	var lockedFolderColor by remember { mutableStateOf(unselectedColor) }
            	var albumGridColor by remember { mutableStateOf(unselectedColor) }
            	var searchPageColor by remember { mutableStateOf(unselectedColor) }
				// for the love of god find a better way
            	var photoGridIcon by remember { mutableStateOf(R.drawable.photogrid_filled) }
            	var lockedFolderIcon by remember { mutableStateOf(R.drawable.locked_folder) }
            	var albumGridIcon by remember { mutableStateOf(R.drawable.albums) }

                // photo grid button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentView.value != ComposeViewType.PhotoGridView){
                            	currentView.value = ComposeViewType.PhotoGridView

                            	photoGridColor = selectedColor
                            	lockedFolderColor = unselectedColor
                            	albumGridColor = unselectedColor
                            	searchPageColor = unselectedColor	

                            	photoGridIcon = R.drawable.photogrid_filled
                            	lockedFolderIcon = R.drawable.locked_folder
                            	albumGridIcon = R.drawable.albums
                            } 
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(1000.dp))
							.background(photoGridColor),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = photoGridIcon),
                            contentDescription = "button",
                            modifier = Modifier
                                .size(iconSize)
                        )

                    }
                    Text(
                        text = "Photos",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }

                // locked folder button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentView.value != ComposeViewType.LockedFolder){
                            	currentView.value = ComposeViewType.LockedFolder

                            	photoGridColor = unselectedColor
                            	lockedFolderColor = selectedColor
                            	albumGridColor = unselectedColor
                            	searchPageColor = unselectedColor	

                            	photoGridIcon = R.drawable.photogrid
                            	lockedFolderIcon = R.drawable.locked_folder_filled
                            	albumGridIcon = R.drawable.albums
                            }                        	
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter)
                            .background(lockedFolderColor),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
	                    Icon(
	                        painter = painterResource(id = lockedFolderIcon),
	                        contentDescription = "button",
	                        modifier = Modifier
	                            .size(iconSize)
	                    )
                    }                

                    Text(
                        text = "Secure",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }

                // album grid button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentView.value != ComposeViewType.AlbumGridView){
                            	currentView.value = ComposeViewType.AlbumGridView

                            	photoGridColor = unselectedColor
                            	lockedFolderColor = unselectedColor
                            	albumGridColor = selectedColor
                            	searchPageColor = unselectedColor

                          		photoGridIcon = R.drawable.photogrid
                            	lockedFolderIcon = R.drawable.locked_folder
                            	albumGridIcon = R.drawable.albums_filled
                            }                        	
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter)
                            .background(albumGridColor),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
	                    Icon(
	                        painter = painterResource(id = albumGridIcon),
	                        contentDescription = "button",
	                        modifier = Modifier
	                            .size(iconSize)
	                    )
                    }                

                    Text(
                        text = "Albums",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }

                // search page button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentView.value != ComposeViewType.SearchPage){
                            	currentView.value = ComposeViewType.SearchPage

                            	photoGridColor = unselectedColor
                            	lockedFolderColor = unselectedColor
                            	albumGridColor = unselectedColor
                            	searchPageColor = selectedColor	

                            	photoGridIcon = R.drawable.photogrid
                            	lockedFolderIcon = R.drawable.locked_folder
                            	albumGridIcon = R.drawable.albums                          	
                            }                        	
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .height(iconSize + 8.dp)
                            .width(iconSize * 2.25f)
                            .clip(RoundedCornerShape(1000.dp))
                            .align(Alignment.TopCenter)
                            .background(searchPageColor),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
	                    Icon(
	                        painter = painterResource(id = R.drawable.search),
	                        contentDescription = "button",
	                        modifier = Modifier
	                            .size(iconSize)
	                    )
                    }                

                    Text(
                        text = "Search",
                        fontSize = TextUnit(textSize, TextUnitType.Sp),
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setContentForActivity()
                } else {
                    Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
                    requestStoragePermission()
                }
            }
        }
    }
}
