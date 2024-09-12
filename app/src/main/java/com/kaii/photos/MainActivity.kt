package com.kaii.photos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.kaii.photos.compose.CustomMaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.AlbumGridView
import com.kaii.photos.compose.LockedFolderView
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.PhotoGrid
import com.kaii.photos.compose.SearchPage
import com.kaii.photos.compose.SingleAlbumView
import com.kaii.photos.compose.SinglePhotoView
import com.kaii.photos.compose.SingleTrashedPhotoView
import com.kaii.photos.compose.TrashedPhotoGridView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.models.main_activity.MainDataSharingModel
import com.kaii.photos.models.main_activity.MainDataSharingModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_READ_STORAGE = 0
        private val PERMISSIONS_REQUEST =
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )

        lateinit var applicationDatabase: MediaDatabase
        lateinit var mainViewModel: MainDataSharingModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mediaDatabase = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media-database"
        ).build()
        applicationDatabase = mediaDatabase

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
				window.decorView.setBackgroundColor(CustomMaterialTheme.colorScheme.background.toArgb())

                mainViewModel = viewModel(
                    factory = MainDataSharingModelFactory()
                )

                val navControllerLocal = rememberNavController()
                val currentView = remember { mutableStateOf(MainScreenViewType.PhotoGridView) }
                NavHost (
                    navController = navControllerLocal,
                    startDestination = MultiScreenViewType.MainScreen.name,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .background(CustomMaterialTheme.colorScheme.background),
                    enterTransition = {
                        slideInHorizontally (
                            animationSpec = tween(
                                durationMillis = 250
                            )
                        ) { width -> width }
                    },
                    exitTransition = {
						slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 250
                            )
                        ) { width -> -width }
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 250
                            )
                        ) { width -> width }
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 250
                            )
                        ) { width -> -width }
                    }
                ) {
                    composable(MultiScreenViewType.MainScreen.name) {
       					enableEdgeToEdge(
							navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
							statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
						)

                        Content(currentView, navControllerLocal)
                    }

                    composable(MultiScreenViewType.SinglePhotoView.name) {
						enableEdgeToEdge(
							navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f).toArgb()),
							statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
								CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()) 
						)
                    	SinglePhotoView(navControllerLocal, window)
                    }

                    composable(MultiScreenViewType.SingleAlbumView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )
                        SingleAlbumView(navControllerLocal)
                    }

                    composable(MultiScreenViewType.SingleTrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )

                        SingleTrashedPhotoView(navControllerLocal, window)
                    }

                    composable(MultiScreenViewType.TrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )

                        TrashedPhotoGridView(navControllerLocal)
                    }

                    composable(MultiScreenViewType.LockedFolderView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb())
                        )

                        LockedFolderView(navControllerLocal)
                    }
                }
            }
        }
    }

    @Composable
    private fun Content(
        currentView: MutableState<MainScreenViewType>,
        navController: NavHostController
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar()
            },
            bottomBar = { 
            	BottomBar(currentView) 
           	}
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
                                (slideInHorizontally { height -> height } + fadeIn()).togetherWith(
                                    slideOutHorizontally { height -> -height } + fadeOut())
                			} else {
                                (slideInHorizontally { height -> -height } + fadeIn()).togetherWith(
                                    slideOutHorizontally { height -> height } + fadeOut())
                			}.using(
                				SizeTransform(clip = false)
                			)
                		},
                        label = "MainAnimatedContentView"
                    ) { stateValue ->
                    	val searchViewModel: SearchViewModel = viewModel(
                   	        factory = SearchViewModelFactory(LocalContext.current.applicationContext, "")
                   	    )
	                    when (stateValue) {
	                        MainScreenViewType.PhotoGridView -> PhotoGrid(navController, ImageFunctions.LoadNormalImage, stringResource(id = R.string.default_homepage_photogrid_dir), MediaItemSortMode.DateTaken)
	                        MainScreenViewType.LockedFolder -> LockedFolderEntryView(navController)
	                        MainScreenViewType.AlbumGridView -> AlbumGridView(navController)
   	                        MainScreenViewType.SearchPage -> SearchPage(navController, searchViewModel)
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
                        tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
	    	colors = TopAppBarDefaults.topAppBarColors(
	    		containerColor = CustomMaterialTheme.colorScheme.background
	    	),
        )
    }

    @Composable
    private fun BottomBar(currentView: MutableState<MainScreenViewType>) {
        BottomAppBar(
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
            contentColor = CustomMaterialTheme.colorScheme.onPrimaryContainer,
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
            	val unselectedColor = CustomMaterialTheme.colorScheme.surfaceContainer
            	val selectedColor = CustomMaterialTheme.colorScheme.secondaryContainer
            	var photoGridColor by remember { mutableStateOf(unselectedColor) }
            	var lockedFolderColor by remember { mutableStateOf(unselectedColor) }
            	var albumGridColor by remember { mutableStateOf(unselectedColor) }
            	var searchPageColor by remember { mutableStateOf(unselectedColor) }
				// for the love of god find a better way
            	var photoGridIcon by remember { mutableIntStateOf(R.drawable.photogrid_filled) }
            	var lockedFolderIcon by remember { mutableIntStateOf(R.drawable.locked_folder) }
            	var albumGridIcon by remember { mutableIntStateOf(R.drawable.albums) }

				when (currentView.value) {
					MainScreenViewType.PhotoGridView -> {
						photoGridColor = selectedColor
                        lockedFolderColor = unselectedColor
                        albumGridColor = unselectedColor
                        searchPageColor = unselectedColor

                        photoGridIcon = R.drawable.photogrid_filled
                        lockedFolderIcon = R.drawable.locked_folder
                        albumGridIcon = R.drawable.albums
					}	
					MainScreenViewType.LockedFolder -> {
						photoGridColor = unselectedColor
	                    lockedFolderColor = selectedColor
	                    albumGridColor = unselectedColor
	                    searchPageColor = unselectedColor

	                    photoGridIcon = R.drawable.photogrid
	                    lockedFolderIcon = R.drawable.locked_folder_filled
	                    albumGridIcon = R.drawable.albums						
					}	
					MainScreenViewType.AlbumGridView -> {
	                    photoGridColor = unselectedColor
	                    lockedFolderColor = unselectedColor
	                    albumGridColor = selectedColor
	                    searchPageColor = unselectedColor

	                    photoGridIcon = R.drawable.photogrid
	                    lockedFolderIcon = R.drawable.locked_folder
	                    albumGridIcon = R.drawable.albums_filled						
					}
					MainScreenViewType.SearchPage -> {
						photoGridColor = unselectedColor
	                    lockedFolderColor = unselectedColor
	                    albumGridColor = unselectedColor
	                    searchPageColor = selectedColor

	                    photoGridIcon = R.drawable.photogrid
	                    lockedFolderIcon = R.drawable.locked_folder
	                    albumGridIcon = R.drawable.albums
					}
				}

                // photo grid button
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentView.value != MainScreenViewType.PhotoGridView) {
                                currentView.value = MainScreenViewType.PhotoGridView
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
                            if (currentView.value != MainScreenViewType.LockedFolder) {
                                currentView.value = MainScreenViewType.LockedFolder
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
                            if (currentView.value != MainScreenViewType.AlbumGridView) {
                                currentView.value = MainScreenViewType.AlbumGridView
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
                            if (currentView.value != MainScreenViewType.SearchPage) {
                                currentView.value = MainScreenViewType.SearchPage
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



    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
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
