package com.kaii.photos.compose.open_with_view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.BottomAppBarItem
import com.kaii.photos.compose.app_bars.setBarVisibility
import com.kaii.photos.compose.single_photo.GlideView
import com.kaii.photos.compose.single_photo.MotionPhotoView
import com.kaii.photos.compose.single_photo.VideoPlayer
import com.kaii.photos.compose.single_photo.rememberGlideZoomableState
import com.kaii.photos.compose.transformable
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.ScreenType
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.formatDate
import com.kaii.photos.helpers.motion_photo.rememberMotionPhoto
import com.kaii.photos.helpers.motion_photo.rememberMotionPhotoState
import com.kaii.photos.helpers.scrolling.rememberSinglePhotoScrollState
import com.kaii.photos.helpers.shareImage
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import kotlinx.coroutines.Dispatchers

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun OpenWithContent(
    uri: Uri,
    window: Window
) {
    val appBarsVisible = remember { mutableStateOf(true) }
    val context = LocalContext.current

    val releaseExoPlayer: MutableState<() -> Unit> = remember { mutableStateOf({}) }

    val mimeType = context.contentResolver.getType(uri) ?: "image/*"
    val type =
        if (mimeType.contains("image")) MediaType.Image
        else MediaType.Video

    val scrollState = rememberSinglePhotoScrollState(isOpenWithView = true)

    Scaffold(
        topBar = {
            TopBar(
                appBarsVisible = appBarsVisible
            ) {
                releaseExoPlayer.value()
            }
        },
        bottomBar = {
            BottomBar(
                uri = uri,
                appBarsVisible = appBarsVisible,
                window = window,
                mediaType = type,
                mimeType = mimeType
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets()
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(0.dp)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val zoomableState = rememberGlideZoomableState()
            val motionPhoto = rememberMotionPhoto(uri = uri)

            if (type == MediaType.Video) {
                val shouldPlay = rememberSaveable { mutableStateOf(true) }

                VideoPlayer(
                    item = MediaStoreData(
                        uri = uri
                    ),
                    appBarsVisible = appBarsVisible,
                    shouldAutoPlay = false,
                    scrollState = scrollState,
                    window = window,
                    shouldPlay = shouldPlay,
                    isOpenWithView = true,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .transformable()
                )
            } else {
                if (motionPhoto.isMotionPhoto.value) {
                    val state = rememberMotionPhotoState(uri = uri)
                    releaseExoPlayer.value = state::release

                    BackHandler {
                        state.release()
                        (context as Activity).finish()
                    }

                    MotionPhotoView(
                        state = state,
                        zoomableState = zoomableState,
                        appBarsVisible = appBarsVisible,
                        window = window,
                        glideImageView = @Composable { modifier ->
                            GlideView(
                                model = uri,
                                item = MediaStoreData.dummyItem,
                                zoomableState = zoomableState,
                                window = window,
                                appBarsVisible = appBarsVisible,
                                modifier = modifier,
                                useCache = false,
                                disableSetBarVisibility = true
                            )
                        }
                    )
                } else {
                    BackHandler {
                        (context as Activity).finish()
                    }

                    GlideView(
                        model = uri,
                        item = MediaStoreData.dummyItem,
                        zoomableState = zoomableState,
                        window = window,
                        appBarsVisible = appBarsVisible,
                        useCache = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    appBarsVisible: MutableState<Boolean>,
    releaseExoPlayer: () -> Unit
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
                val isLandscape by rememberDeviceOrientation()
                Text(
                    text = stringResource(id = R.string.media),
                    fontSize = TextUnit(18f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = if (isLandscape) 300.dp else 180.dp)
                )
            },
            navigationIcon = {
                val context = LocalContext.current

                IconButton(
                    onClick = {
                        releaseExoPlayer()
                        (context as Activity).finish()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = stringResource(id = R.string.return_to_previous_page),
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
    uri: Uri,
    appBarsVisible: MutableState<Boolean>,
    window: Window,
    mediaType: MediaType,
    mimeType: String
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
                        text = stringResource(id = R.string.media_share),
                        iconResId = R.drawable.share,
                        cornerRadius = 32.dp,
                        action = {
                            shareImage(uri, context)
                        }
                    )

                    val resources = LocalResources.current
                    val mainViewModel = LocalMainViewModel.current
                    BottomAppBarItem(
                        text = stringResource(id = R.string.edit),
                        iconResId = R.drawable.paintbrush,
                        cornerRadius = 32.dp,
                        action = {
                            mainViewModel.launch(Dispatchers.IO) {
                                val isLoading = mutableStateOf(true)

                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.LoadingEvent(
                                        message = resources.getString(
                                            if (mediaType == MediaType.Image) R.string.editing_open_with_copying_image
                                            else R.string.editing_open_with_copying_video
                                        ),
                                        icon =
                                            if (mediaType == MediaType.Image) R.drawable.edit
                                            else R.drawable.movie_edit,
                                        isLoading = isLoading
                                    )
                                )

                                val extension = mimeType.split("/")[1]
                                val currentTime = System.currentTimeMillis()
                                val date = formatDate(
                                    timestamp = currentTime / 1000,
                                    sortBy = MediaItemSortMode.DateTaken,
                                    format = DisplayDateFormat.Default
                                )
                                val name = resources.getString(R.string.edit_desc, "$date.$extension")

                                val contentValues = ContentValues().apply {
                                    put(MediaColumns.DISPLAY_NAME, name)
                                    put(MediaColumns.DATE_MODIFIED, currentTime)
                                    put(MediaColumns.DATE_TAKEN, currentTime)
                                    put(MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                                    put(MediaColumns.MIME_TYPE, mimeType)
                                }

                                val contentUri = context.contentResolver.insert(
                                    if (mediaType == MediaType.Image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                    else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )

                                contentUri?.let {
                                    context.contentResolver.getMediaStoreDataFromUri(context = context, uri = contentUri)?.absolutePath?.let { absolutePath ->
                                        context.contentResolver.copyUriToUri(
                                            from = uri,
                                            to = contentUri
                                        )

                                        setBarVisibility(
                                            visible = true,
                                            window = window
                                        ) {
                                            appBarsVisible.value = it
                                        }

                                        isLoading.value = false

                                        mainViewModel.launch {
                                            navController.navigate(
                                                if (mediaType == MediaType.Image) {
                                                    Screens.ImageEditor(
                                                        absolutePath = absolutePath,
                                                        uri = contentUri.toString(),
                                                        dateTaken = currentTime / 1000,
                                                        albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                                                        type = ScreenType.Normal
                                                    )
                                                } else {
                                                    Screens.VideoEditor(
                                                        uri = contentUri.toString(),
                                                        absolutePath = absolutePath,
                                                        albumInfo = AlbumInfo.createPathOnlyAlbum(emptyList()),
                                                        type = ScreenType.Normal
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        )
    }
}
