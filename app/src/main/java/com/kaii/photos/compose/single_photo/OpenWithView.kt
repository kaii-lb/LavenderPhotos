package com.kaii.photos.compose.single_photo

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.photos.BuildConfig
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.BottomAppBarItem
import com.kaii.photos.compose.ConfirmationDialog
import com.kaii.photos.compose.ExplanationDialog
import com.kaii.photos.compose.setBarVisibility
import com.kaii.photos.compose.rememberDeviceOrientation
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.helpers.EditingScreen
import com.kaii.photos.helpers.GetPermissionAndRun
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.setTrashedOnPhotoList
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import com.kaii.photos.ui.theme.PhotosTheme
import java.io.File

class OpenWithView : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val absolutePath =
            intent.data?.path?.removePrefix("/")?.replace("%20", " ")?.let {
                File(Uri.decode(it).toUri().path!!).absolutePath
            }

        val mediaItem = absolutePath?.let { path ->
            val uri = contentResolver.getUriFromAbsolutePath(path, MediaType.Image)

            uri?.let { contentUri ->
                val mediaStoreData = contentResolver.getMediaStoreDataFromUri(contentUri)

                mediaStoreData
            }
        }

        if (mediaItem == null) {
            Toast.makeText(applicationContext, "This media doesn't exist", Toast.LENGTH_LONG).show()
            return
        }

        setContent {
            enableEdgeToEdge(
                navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
                statusBarStyle =
                if (!isSystemInDarkTheme()) {
                    SystemBarStyle.light(
                        MaterialTheme.colorScheme.background.toArgb(),
                        MaterialTheme.colorScheme.background.toArgb()
                    )
                } else {
                    SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb())
                }
            )

            val followDarkTheme =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }

            PhotosTheme(
                darkTheme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val navController = rememberNavController()

                CompositionLocalProvider(LocalNavController provides navController) {
	                NavHost(
	                    navController = navController,
	                    startDestination = MultiScreenViewType.OpenWithView.name,
	                    modifier = Modifier
	                        .fillMaxSize(1f)
	                        .background(MaterialTheme.colorScheme.background),
	                    enterTransition = {
	                        slideInHorizontally(
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
	                    composable(MultiScreenViewType.OpenWithView.name) {
		                    enableEdgeToEdge(
		                        navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
		                        statusBarStyle =
		                        if (!isSystemInDarkTheme()) {
		                            SystemBarStyle.light(
		                                MaterialTheme.colorScheme.background.toArgb(),
		                                MaterialTheme.colorScheme.background.toArgb()
		                            )
		                        } else {
		                            SystemBarStyle.dark(MaterialTheme.colorScheme.background.toArgb())
		                        }
		                    )

	                        Content(
	                            mediaItem = mediaItem,
	                            window = window
	                        )
	                    }

	                    composable<EditingScreen> {
	                        enableEdgeToEdge(
	                            navigationBarStyle = SystemBarStyle.dark(MaterialTheme.colorScheme.surfaceContainer.toArgb()),
	                            statusBarStyle = SystemBarStyle.auto(
	                                MaterialTheme.colorScheme.surfaceContainer.toArgb(),
	                                MaterialTheme.colorScheme.surfaceContainer.toArgb()
	                            )
	                        )

	                        val screen: EditingScreen = it.toRoute()

	                        EditingView(
	                            absolutePath = screen.absolutePath,
	                            dateTaken = screen.dateTaken,
	                            uri = screen.uri.toUri(),
	                            window = window,
	                            overwriteByDefault = false
	                        )
	                    }
	                }
                }
            }
        }
    }
}

@Composable
private fun Content(
    mediaItem: MediaStoreData,
    window: Window,
) {
    val navController = LocalNavController.current
    val showInfoDialog = remember { mutableStateOf(false) }
    val appBarsVisible = remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopBar(
                showInfoDialog = showInfoDialog,
                appBarsVisible = appBarsVisible,
                mediaItem = mediaItem
            )
        },
        bottomBar = {
            BottomBar(
                mediaItem = mediaItem,
                appBarsVisible = appBarsVisible,
                window = window
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { _ ->
        SingleSecuredPhotoInfoDialog(showDialog = showInfoDialog, currentMediaItem = mediaItem)

        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val scale = remember { mutableStateOf(0f) }
            val rotation = remember { mutableStateOf(0f) }
            val offset = remember { mutableStateOf(Offset.Zero) }

            HorizontalImageList(
                navController = navController,
                currentMediaItem = mediaItem,
                groupedMedia = listOf(mediaItem),
                state = rememberPagerState {
                    1
                },
                scale = scale,
                rotation = rotation,
                offset = offset,
                window = window,
                appBarsVisible = appBarsVisible,
                isOpenWithView = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    showInfoDialog: MutableState<Boolean>,
    appBarsVisible: MutableState<Boolean>,
    mediaItem: MediaStoreData
) {
    AnimatedVisibility(
        visible = appBarsVisible.value,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> -width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> -width } + fadeOut(),
    ) {
        TopAppBar(
            title = {
                val mediaTitle = mediaItem.displayName ?: mediaItem.type.name

                Text(
                    text = mediaTitle,
                    fontSize = TextUnit(18f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(160.dp)
                )
            },
            navigationIcon = {
            	val context = LocalContext.current

                IconButton(
                    onClick = {
						(context as Activity).finish()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "Go back to previous page",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        showInfoDialog.value = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_options),
                        contentDescription = "show more options",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun BottomBar(
    mediaItem: MediaStoreData,
    appBarsVisible: MutableState<Boolean>,
    window: Window
) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    AnimatedVisibility(
        visible = appBarsVisible.value,
        enter =
        slideInVertically(
            animationSpec = tween(
                durationMillis = 250
            )
        ) { width -> width } + fadeIn(),
        exit =
        slideOutVertically(
            animationSpec = tween(
                durationMillis = 300
            )
        ) { width -> width } + fadeOut(),
    ) {
    	val isLandscape by rememberDeviceOrientation()

	    BottomAppBar(
	        actions = {
	        	Row(
	        	    modifier = Modifier
	        	        .fillMaxWidth(1f)
	        	        .padding(12.dp, 0.dp),
	        	    verticalAlignment = Alignment.CenterVertically,
	        	    horizontalArrangement =
	        	    if (isLandscape)
	        	        Arrangement.spacedBy(
	        	            space = 48.dp,
	        	            alignment = Alignment.CenterHorizontally
	        	        )
	        	    else Arrangement.SpaceEvenly
	        	) {
		            BottomAppBarItem(
		                text = "Share",
		                iconResId = R.drawable.share,
		                cornerRadius = 32.dp,
		                action = {
		                    shareImage(mediaItem.uri, context)
		                }
		            )

		            val showNotImplementedDialog = remember { mutableStateOf(false) }

		            if (showNotImplementedDialog.value) {
		                ExplanationDialog(
		                    title = "Unimplemented",
		                    explanation = "Editing videos has not been implemented yet as of version ${BuildConfig.VERSION_NAME} of Lavender Photos. This feature will be added as soon as possible, thank you for your patience.",
		                    showDialog = showNotImplementedDialog
		                )
		            }

		            BottomAppBarItem(
		                text = "Edit",
		                iconResId = R.drawable.paintbrush,
		                cornerRadius = 32.dp,
		                action = if (mediaItem.type == MediaType.Image) {
		                    {
		                        setBarVisibility(
		                            visible = true,
		                            window = window
		                        ) {
		                            appBarsVisible.value = it
		                        }

		                        navController.navigate(
		                            EditingScreen(
		                                absolutePath = mediaItem.absolutePath,
		                                uri = mediaItem.uri.toString(),
		                                dateTaken = mediaItem.dateTaken
		                            )
		                        )
		                    }
		                } else {
		                    { showNotImplementedDialog.value = true }
		                }
		            )

		            val showDeleteDialog = remember { mutableStateOf(false) }
		            val runTrashAction = remember { mutableStateOf(false) }

		            GetPermissionAndRun(
		                uris = listOf(mediaItem.uri),
		                shouldRun = runTrashAction,
		                onGranted = {
						    val contentResolver = context.contentResolver

						    val trashedValues = ContentValues().apply {
						        put(MediaColumns.IS_TRASHED, true)
						        put(MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
						    }

						    contentResolver.update(mediaItem.uri, trashedValues, null)

		                    (context as Activity).finish()
		                }
		            )

		            BottomAppBarItem(
		                text = "Delete",
		                iconResId = R.drawable.trash,
		                cornerRadius = 32.dp,
		                dialogComposable = {
		                    ConfirmationDialog(
		                        showDialog = showDeleteDialog,
		                        dialogTitle = "Delete this ${mediaItem.type}?",
		                        confirmButtonLabel = "Delete"
		                    ) {
		                        runTrashAction.value = true
		                    }
		                },
		                action = {
		                    showDeleteDialog.value = true
		                }
		            )
	        	}
	        }
	    )
    }
}
