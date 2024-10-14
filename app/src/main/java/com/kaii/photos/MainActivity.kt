package com.kaii.photos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.kaii.photos.compose.AboutPage
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.compose.IsSelectingBottomAppBar
import com.kaii.photos.compose.IsSelectingTopBar
import com.kaii.photos.compose.LockedFolderEntryView
import com.kaii.photos.compose.MainAppBottomBar
import com.kaii.photos.compose.MainAppDialog
import com.kaii.photos.compose.MainAppTopBar
import com.kaii.photos.compose.getAppBarContentTransition
import com.kaii.photos.compose.grids.AlbumsGridView
import com.kaii.photos.compose.grids.LockedFolderView
import com.kaii.photos.compose.grids.PhotoGrid
import com.kaii.photos.compose.grids.SearchPage
import com.kaii.photos.compose.grids.SingleAlbumView
import com.kaii.photos.compose.grids.TrashedPhotoGridView
import com.kaii.photos.compose.single_photo.SingleHiddenPhotoView
import com.kaii.photos.compose.single_photo.SinglePhotoView
import com.kaii.photos.compose.single_photo.SingleTrashedPhotoView
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.addToAlbumsList
import com.kaii.photos.datastore.albumsListKey
import com.kaii.photos.datastore.getAlbumsList
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import com.kaii.photos.models.main_activity.MainDataSharingModel
import com.kaii.photos.models.main_activity.MainDataSharingModelFactory
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

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

        lateinit var startForResult: ActivityResultLauncher<Intent>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

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

		startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                Toast.makeText(applicationContext, "Failed to add album", Toast.LENGTH_LONG).show()
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
	        		Log.e(TAG, "PERMISSION FAILED $it")
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
				val selectedItemsList = remember { SnapshotStateList<MediaStoreData>() }
				
                val context = LocalContext.current

                // TODO: please make it not hang lol
				runBlocking {
					context.datastore.addToAlbumsList("DCIM/Camera")
					context.datastore.addToAlbumsList("Pictures/Screenshot")
					context.datastore.addToAlbumsList("Pictures/Whatsapp")
					context.datastore.addToAlbumsList("Pictures/100PINT/Pins")
					context.datastore.addToAlbumsList("Movies")
					context.datastore.addToAlbumsList("LavenderPhotos/Restored Files")
					context.datastore.addToAlbumsList("Download")
					context.datastore.addToAlbumsList("Pictures/Instagram")
				}
                
                NavHost (
                    navController = navControllerLocal,
                    startDestination = MultiScreenViewType.MainScreen.name,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .background(CustomMaterialTheme.colorScheme.background),
                    enterTransition = {
                        slideInHorizontally (
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> width } + fadeIn()
                    },
                    exitTransition = {
						slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> -width } + fadeOut()
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> width } + fadeOut()
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 350
                            )
                        ) { width -> -width } + fadeIn()
                    }
                ) {
                    composable(MultiScreenViewType.MainScreen.name) {
       					enableEdgeToEdge(
							navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
							statusBarStyle = 
								if (!isSystemInDarkTheme()) {
									SystemBarStyle.light(CustomMaterialTheme.colorScheme.background.toArgb(), CustomMaterialTheme.colorScheme.background.toArgb()) 	
								} else {
									SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb())
								}
						)
                        setupNextScreen(context, windowInsetsController, selectedItemsList)

                        Content(currentView, navControllerLocal, showDialog, selectedItemsList)
                    }

                    composable(MultiScreenViewType.SinglePhotoView.name) {
						enableEdgeToEdge(
							navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f).toArgb()),
							statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb(),
								CustomMaterialTheme.colorScheme.surface.copy(alpha = 0.2f).toArgb()) 
						)
                        setupNextScreen(context, windowInsetsController, selectedItemsList)
						
                    	SinglePhotoView(navControllerLocal, window, scale, rotation, offset)
                    }

                    composable(MultiScreenViewType.SingleAlbumView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )
                        setupNextScreen(context, windowInsetsController, selectedItemsList)
						                        
                        SingleAlbumView(navControllerLocal, selectedItemsList)
                    }

                    composable(MultiScreenViewType.SingleTrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )
                        setupNextScreen(context, windowInsetsController, selectedItemsList)
						
                        SingleTrashedPhotoView(navControllerLocal, window, scale, rotation, offset)
                    }

                    composable(MultiScreenViewType.TrashedPhotoView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb()) 
                        )

                        setupNextScreen(context, windowInsetsController, selectedItemsList)

                        TrashedPhotoGridView(navControllerLocal, selectedItemsList)
                    }

                    composable(MultiScreenViewType.LockedFolderView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb())
                        )
                        setupNextScreen(context, windowInsetsController, selectedItemsList)
						
                        LockedFolderView(navControllerLocal)
                    }

                    composable(MultiScreenViewType.SingleHiddenPhotoVew.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.surfaceContainer.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.surface.toArgb(), CustomMaterialTheme.colorScheme.surface.toArgb())
                        )
                        setupNextScreen(context, windowInsetsController, selectedItemsList)
						
                        // TODO: should merge with SingleTrashedPhotoView???? idfk wait for future
                        SingleHiddenPhotoView(navControllerLocal, window, scale, rotation, offset)
                    }

                    composable(MultiScreenViewType.AboutAndUpdateView.name) {
                        enableEdgeToEdge(
                            navigationBarStyle = SystemBarStyle.dark(CustomMaterialTheme.colorScheme.background.toArgb()),
                            statusBarStyle = SystemBarStyle.auto(CustomMaterialTheme.colorScheme.background.toArgb(), CustomMaterialTheme.colorScheme.background.toArgb())
                        )
                        setupNextScreen(context, windowInsetsController, selectedItemsList)

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
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
    ) {	
		val galleryViewModel: GalleryViewModel = viewModel(
			factory = GalleryViewModelFactory(LocalContext.current, stringResource(id = R.string.default_homepage_photogrid_dir), MediaItemSortMode.DateTaken)
		)
	//	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

		val mediaStoreData = galleryViewModel.mediaFlow.collectAsStateWithLifecycle(context = Dispatchers.IO)

		var groupedMedia = remember { mutableStateOf(mediaStoreData.value) }
		mainViewModel.setGroupedMedia(groupedMedia.value)

		LaunchedEffect(mediaStoreData.value) {
			groupedMedia.value = mediaStoreData.value
			mainViewModel.setGroupedMedia(mediaStoreData.value)
		}
    
        Scaffold(
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar(showDialog, selectedItemsList, navController)
            },
            bottomBar = { 
            	BottomBar(currentView, selectedItemsList, navController, groupedMedia)
           	}
        ) { padding ->
			val context = LocalContext.current

            BackHandler (
                enabled = currentView.value != MainScreenViewType.PhotosGridView && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name && selectedItemsList.size == 0
            ) {
                currentView.value = MainScreenViewType.PhotosGridView
            }

            Column(
                modifier = Modifier
                    .padding(0.dp, padding.calculateTopPadding(), 0.dp, padding.calculateBottomPadding())
            ) {
                Column(
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                	MainAppDialog(showDialog, currentView, navController, selectedItemsList)
                	
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
                    	selectedItemsList.clear()
                    	
	                    when (stateValue) {
	                        MainScreenViewType.PhotosGridView -> {
	                        	PhotoGrid(
	                        		groupedMedia = groupedMedia,
	                        		navController = navController,
	                        		operation = ImageFunctions.LoadNormalImage,
	                        		path = stringResource(id = R.string.default_homepage_photogrid_dir), 
	                        		selectedItemsList = selectedItemsList,
                        		)	
	                        }
	                        MainScreenViewType.SecureFolder -> LockedFolderEntryView(navController)
	                        MainScreenViewType.AlbumsGridView -> {
								val listOfDirs = runBlocking {
									val list = context.datastore.getAlbumsList()
									list
								}
								             
	                        	val albumsViewModel: AlbumsViewModel = viewModel(
                       				factory = AlbumsViewModelFactory(context, listOfDirs.toList())
                       			)
	                        	AlbumsGridView(albumsViewModel, navController, listOfDirs)	
	                        } 
   	                        MainScreenViewType.SearchPage -> SearchPage(navController, selectedItemsList)
	                    }
                	}
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        showDialog: MutableState<Boolean>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        navController: NavHostController
    ) {
		val show by remember { derivedStateOf {
			selectedItemsList.size > 0
		}}
        AnimatedContent(
           	targetState = show && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
            transitionSpec = {
                getAppBarContentTransition(show)
            },
            label = "MainTopBarAnimatedContentView"
        ) { target ->
            if (!target) {
                MainAppTopBar(showDialog = showDialog)
            } else {
                IsSelectingTopBar(selectedItemsList = selectedItemsList)
            }
        }
    }

    @Composable
    private fun BottomBar(
        currentView: MutableState<MainScreenViewType>,
        selectedItemsList: SnapshotStateList<MediaStoreData>,
        navController: NavHostController,
        groupedMedia: MutableState<List<MediaStoreData>>
    ) {
		val show by remember { derivedStateOf {
			selectedItemsList.size > 0
		}}    
        AnimatedContent(
            targetState = show && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name,
            transitionSpec = {
                getAppBarContentTransition(show)
            },
            label = "MainBottomBarAnimatedContentView"
        ) { state ->
            if (!state) {
                MainAppBottomBar(currentView)
            } else {
                IsSelectingBottomAppBar(selectedItemsList = selectedItemsList, groupedMedia = groupedMedia)
            }
        }

    }
}

private fun setupNextScreen(
	context: Context,
	windowInsetsController: WindowInsetsController?,
	selectedItemsList: SnapshotStateList<MediaStoreData>
) {
	selectedItemsList.clear()
	
    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        windowInsetsController?.apply {
            show(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_DEFAULT
        }
    }
}
