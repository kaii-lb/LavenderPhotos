package com.kaii.photos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.AlbumGridView
import com.kaii.photos.compose.LockedFolderView
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.PhotoGrid
import com.kaii.photos.compose.SearchPage
import com.kaii.photos.compose.SingleAlbumView
import com.kaii.photos.compose.SingleHiddenPhotoView
import com.kaii.photos.compose.SinglePhotoView
import com.kaii.photos.compose.SingleTrashedPhotoView
import com.kaii.photos.compose.TrashedPhotoGridView
import com.kaii.photos.compose.DialogClickableItem
import com.kaii.photos.compose.DialogItemPosition
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.addToAlbumsList
import com.kaii.photos.datastore.albumsListKey
import com.kaii.photos.datastore.getUsername
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.models.main_activity.MainDataSharingModel
import com.kaii.photos.models.main_activity.MainDataSharingModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private const val DIR_REQUEST_CODE = 1
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
				val showDialog = remember { mutableStateOf(false) }
                
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

                        Content(currentView, navControllerLocal, showDialog)
                    }

                    composable(MultiScreenViewType.SinglePhotoView.name) {
						enableEdgeToEdge(
							navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f).toArgb()),
							statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
								CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()) 
						)
						val scale = remember { mutableFloatStateOf(1f) }
						val rotation = remember { mutableFloatStateOf(0f) }
						val offset = remember { mutableStateOf(Offset.Zero) }
                    	SinglePhotoView(navControllerLocal, window, scale, rotation, offset)
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

                    composable(MultiScreenViewType.SingleHiddenPhotoVew.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb())
                        )

                        // TODO: should merge with SingleTrashedPhotoView???? idfk wait for future
                        SingleHiddenPhotoView(navControllerLocal, window)
                    }
                }
            }
        }
    }

    @Composable
    private fun Content(
        currentView: MutableState<MainScreenViewType>,
        navController: NavHostController,
        showDialog: MutableState<Boolean>
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar(showDialog, currentView)
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
    private fun TopBar(showDialog: MutableState<Boolean>, currentView: MutableState<MainScreenViewType>) {
        val context = LocalContext.current

		MainDialog(showDialog, currentView)
        
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
                    onClick = {
                        showDialog.value = true
                    },
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

    override fun onActivityResult(requetCode: Int, resultCode: Int, resultData: Intent?) {
    	if (requetCode == DIR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
    		resultData?.data?.also { uri ->
    			val path = uri.path ?: ""
                lifecycleScope.launch {
 					applicationContext.datastore.addToAlbumsList(path.replace("/storage/emulated/0/", ""))
                }
    		}
    	}
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainDialog(showDialog: MutableState<Boolean>, currentView: MutableState<MainScreenViewType>) {
    if (showDialog.value) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        Dialog(
            onDismissRequest = {
                showDialog.value = false
            }
        ) {
            Column (
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                    .padding(8.dp)
            ) {
                Box (
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon (
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                            	.size(24.dp)
                        )
                    }

					val splitBy = kotlin.text.Regex("(?=[A-Z])")
                    Text(
                        text = currentView.value.name.split(splitBy)[1],
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                Row (
                	modifier = Modifier
                		.fillMaxWidth(1f)
                		.padding(8.dp),
                	verticalAlignment = Alignment.CenterVertically,
                	horizontalArrangement = Arrangement.Start
                ) {
                	var username by remember { 
                		mutableStateOf(
                			runBlocking {
                				context.datastore.getUsername()	
                			}
                			
                		) 
               		}
		
               		GlideImage (
               			model = R.drawable.cat_picture,
               			contentDescription = "User profile picture",
               			contentScale = ContentScale.Crop,
               			modifier = Modifier
               				.size(56.dp)
               				.clip(RoundedCornerShape(1000.dp))
               		) {
               			it
               				.override(256)
               				.diskCacheStrategy(DiskCacheStrategy.ALL)
               		}

               		Spacer(modifier = Modifier.width(8.dp))

               		Text (
               			text = username,
               			fontSize = TextUnit(16f, TextUnitType.Sp),
               			textAlign = TextAlign.Start,
               		)
                }

				Column (
					modifier = Modifier
						.padding(12.dp)
						.wrapContentHeight()
				) {
					DialogClickableItem(
						text = "Data & Backup",
						iconResId = R.drawable.data,
						position = DialogItemPosition.Top,
						action = {}
					)

					if (currentView.value == MainScreenViewType.AlbumGridView) {
						DialogClickableItem(
							text = "Add an album",
							iconResId = R.drawable.add,
							position = DialogItemPosition.Middle,
							action = {
								val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
									putExtra(DocumentsContract.EXTRA_INITIAL_URI, "".toUri())
								}
								context.startActivity(intent)
							}
						)
					}
									
					DialogClickableItem(
						text = "Settings",
						iconResId = R.drawable.settings,
						position = DialogItemPosition.Middle,
						action = {}
					)

					DialogClickableItem(
						text = "Support & Donations",
						iconResId = R.drawable.donation,
						position = DialogItemPosition.Middle,
						action = {}
					)					

					DialogClickableItem (
						text = "About",
						iconResId = R.drawable.info,
						position = DialogItemPosition.Bottom,
						action = {}
					)
				}
            }
        }
    }
}
