package com.kaii.photos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.view.WindowInsetsCompat
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.compose.AboutPage
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.DialogClickableItem
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.grids.LockedFolderView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.addToAlbumsList
import com.kaii.photos.datastore.getUsername
import com.kaii.photos.datastore.setUsername
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import com.kaii.photos.models.main_activity.MainDataSharingModel
import com.kaii.photos.models.main_activity.MainDataSharingModelFactory
import com.kaii.photos.models.search_page.SearchViewModel
import com.kaii.photos.models.search_page.SearchViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : ComponentActivity() {
    companion object {
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

		val failedList = emptyList<String>().toMutableList()

		for (perm in PERMISSIONS_REQUEST) {
			val hasBeenGranted = 
				ContextCompat.checkSelfPermission(
		            this,
		            perm
		        ) == PackageManager.PERMISSION_GRANTED

		    if (!hasBeenGranted) {
		    	failedList.add(perm)
		    }
		}

		if (!Environment.isExternalStorageManager()) {
			val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
			startActivity(intent)

			val request = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                if (failedList.isNotEmpty()) {
                    failedList.forEach {
                        Log.e(TAG, "PERM FAILED $it")
                    }
                    val permRequest = registerForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { result ->
                        result.entries.forEach { perm ->
                            val name = perm.key
                            val granted = perm.value

                            if (granted) failedList.remove(name)
                            else {
                                val shortName = name.split(".").last()
                                Toast.makeText(this, "$shortName permission is required", Toast.LENGTH_LONG).show()
                            }

                            if (failedList.isEmpty() && Environment.isExternalStorageManager()) {
                                setContentForActivity()
                            }

		                    if (name == failedList.lastOrNull()) {
	               	           	Toast.makeText(this, "Failed getting necessary permissions", Toast.LENGTH_LONG).show()
	               	           	finishAffinity()
		                    }                            
                        }
                    }

                    requestStoragePermission(permRequest, failedList)
                } else {
                    setContentForActivity()
                }
			}

			request.launch(intent)
		} else {
			if (failedList.isNotEmpty()) {
	        	failedList.forEach {
	        		println("PERM FAILED $it")
	        	}
		        val permRequest = registerForActivityResult(
		            ActivityResultContracts.RequestMultiplePermissions()
		        ) { result ->
                    result.entries.forEach { perm ->
                        val name = perm.key
                        val granted = perm.value

	                    if (granted) failedList.remove(name)
	                    else {
	                        val shortName = name.split(".").last()
	                        Toast.makeText(this, "$shortName permission is required", Toast.LENGTH_LONG).show()
	                    }

	                    if (failedList.isEmpty() && Environment.isExternalStorageManager()) {
	                        setContentForActivity()
	                    }

	                    if (name == failedList.lastOrNull()) {
               	           	Toast.makeText(this, "Failed getting necessary permissions", Toast.LENGTH_LONG).show()
               	           	finishAffinity()
	                    }
                    }
		        }
	        	
	            requestStoragePermission(permRequest, failedList)
	        } else {
	            setContentForActivity()
	        }
		}
    }

    private fun requestStoragePermission(permRequest: ActivityResultLauncher<Array<String>>, failedList: List<String>) {
    	permRequest.launch(failedList.toTypedArray())
    }

    private fun setContentForActivity() {
        setContent {
            PhotosTheme {
				window.decorView.setBackgroundColor(CustomMaterialTheme.colorScheme.background.toArgb())

                mainViewModel = viewModel(
                    factory = MainDataSharingModelFactory()
                )

                val navControllerLocal = rememberNavController()
                val currentView = remember { mutableStateOf(MainScreenViewType.PhotosGridView) }
                val showDialog = remember { mutableStateOf(false) }
                val windowInsetsController = window.insetsController
                val scale = remember { mutableFloatStateOf(1f) }
                val rotation = remember { mutableFloatStateOf(0f) }
                val offset = remember { mutableStateOf(Offset.Zero) }

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
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}

                        Content(currentView, navControllerLocal, showDialog)
                    }

                    composable(MultiScreenViewType.SinglePhotoView.name) {
						enableEdgeToEdge(
							navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f).toArgb()),
							statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
								CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()) 
						)
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
						
                    	SinglePhotoView(navControllerLocal, window, scale, rotation, offset)
                    }

                    composable(MultiScreenViewType.SingleAlbumView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
						                        
                        SingleAlbumView(navControllerLocal)
                    }

                    composable(MultiScreenViewType.SingleTrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
						
                        SingleTrashedPhotoView(navControllerLocal, window, scale, rotation, offset)
                    }

                    composable(MultiScreenViewType.TrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
						
                        TrashedPhotoGridView(navControllerLocal)
                    }

                    composable(MultiScreenViewType.LockedFolderView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb())
                        )
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
						
                        LockedFolderView(navControllerLocal)
                    }

                    composable(MultiScreenViewType.SingleHiddenPhotoVew.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb())
                        )
						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
						
                        // TODO: should merge with SingleTrashedPhotoView???? idfk wait for future
                        SingleHiddenPhotoView(navControllerLocal, window, scale, rotation, offset)
                    }

                    composable(MultiScreenViewType.AboutAndUpdateView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.background.toArgb(), CustomMaterialTheme.colorScheme.background.toArgb())
                        )

						windowInsetsController?.apply {
							show(WindowInsetsCompat.Type.systemBars())
							systemBarsBehavior =
								WindowInsetsController.BEHAVIOR_DEFAULT
						}
                        AboutPage(navControllerLocal)
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
    	val searchViewModel: SearchViewModel = viewModel(
   	        factory = SearchViewModelFactory(LocalContext.current.applicationContext, "")
   	    )
        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar(showDialog)
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
                	MainDialog(showDialog, currentView, navController)
                	
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
	                    when (stateValue) {
	                        MainScreenViewType.PhotosGridView -> PhotoGrid(navController, ImageFunctions.LoadNormalImage, stringResource(id = R.string.default_homepage_photogrid_dir), MediaItemSortMode.DateTaken)
	                        MainScreenViewType.SecureFolder -> LockedFolderEntryView(navController)
	                        MainScreenViewType.AlbumsGridView -> AlbumsGridView(navController)
   	                        MainScreenViewType.SearchPage -> SearchPage(navController, searchViewModel)
	                    }
                	}
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar(showDialog: MutableState<Boolean>) {
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
					MainScreenViewType.PhotosGridView -> {
						photoGridColor = selectedColor
                        lockedFolderColor = unselectedColor
                        albumGridColor = unselectedColor
                        searchPageColor = unselectedColor

                        photoGridIcon = R.drawable.photogrid_filled
                        lockedFolderIcon = R.drawable.locked_folder
                        albumGridIcon = R.drawable.albums
					}	
					MainScreenViewType.SecureFolder -> {
						photoGridColor = unselectedColor
	                    lockedFolderColor = selectedColor
	                    albumGridColor = unselectedColor
	                    searchPageColor = unselectedColor

	                    photoGridIcon = R.drawable.photogrid
	                    lockedFolderIcon = R.drawable.locked_folder_filled
	                    albumGridIcon = R.drawable.albums						
					}	
					MainScreenViewType.AlbumsGridView -> {
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
                            if (currentView.value != MainScreenViewType.PhotosGridView) {
                                currentView.value = MainScreenViewType.PhotosGridView
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
                            if (currentView.value != MainScreenViewType.SecureFolder) {
                                currentView.value = MainScreenViewType.SecureFolder
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
                            if (currentView.value != MainScreenViewType.AlbumsGridView) {
                                currentView.value = MainScreenViewType.AlbumsGridView
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

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun MainDialog(showDialog: MutableState<Boolean>, currentView: MutableState<MainScreenViewType>, navController: NavHostController) {
        if (showDialog.value) {
            val context = LocalContext.current

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

                        val splitBy = Regex("(?=[A-Z])")
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
                        // TODO: update by TextField
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

//                        Text (
//                            text = username,
//                            fontSize = TextUnit(16f, TextUnitType.Sp),
//                            textAlign = TextAlign.Start,
//                        )

                        TextField (
                            value = username,
                            onValueChange = { newVal ->
                                username = newVal
                                lifecycleScope.launch {
                                    context.datastore.setUsername(newVal)
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = TextUnit(16f, TextUnitType.Sp),
                                textAlign = TextAlign.Start,
                                color = CustomMaterialTheme.colorScheme.onSurface,
                            ),
                            maxLines = 1,
                            colors = TextFieldDefaults.colors().copy(
                                unfocusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = Color.Transparent,
                                focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done,
                                showKeyboardOnFocus = true
                            )
                        )
                    }

                    Column (
                        modifier = Modifier
                            .padding(12.dp)
                            .wrapContentHeight()
                    ) {
                        DialogClickableItem(
                            text = "Select",
                            iconResId = R.drawable.check_item,
                            position = RowPosition.Top,
                        ) {
                        	//showDialog.value = false
                        }
                        
                        if (currentView.value == MainScreenViewType.AlbumsGridView) {
                            DialogClickableItem(
                                text = "Add an album",
                                iconResId = R.drawable.add,
                                position = RowPosition.Middle,
                            ) {
                               	showDialog.value = false
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "".toUri())
                                }
                                val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        result.data?.data?.also { uri ->
                                            val path = uri.path ?: ""

                                            Log.d(TAG, "the path is $path")
                                            val runnable = Runnable {
                                                runBlocking {
                                                    applicationContext.datastore.addToAlbumsList(path.replace("/tree/primary:", ""))
                                                }
                                            }
                                            Thread(runnable).start()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to add album", Toast.LENGTH_LONG).show()
                                    }
                                }
                                startForResult.launch(intent)

                                val runnable = Runnable {
									Thread.sleep(250)
									currentView.value = MainScreenViewType.PhotosGridView
									Thread.sleep(1000)
									currentView.value = MainScreenViewType.AlbumsGridView
								}
                               	Thread(runnable).start()
                            }
                        }

                        DialogClickableItem(
                            text = "Data & Backup",
                            iconResId = R.drawable.data,
                            position = RowPosition.Middle,
                        ) {
                        	//showDialog.value = false
                        }

                        DialogClickableItem(
                            text = "Settings",
                            iconResId = R.drawable.settings,
                            position = RowPosition.Middle,
                        ) {
                        	//showDialog.value = false
                        }

                        DialogClickableItem (
                            text = "About & Updates",
                            iconResId = R.drawable.info,
                            position = RowPosition.Bottom,
                        ) {
                        	showDialog.value = false
                        	navController.navigate(MultiScreenViewType.AboutAndUpdateView.name)
                        }
                    }
                }
            }
        }
    }
}
