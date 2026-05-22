package com.kaii.photos.compose.open_with_view

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.editor.EditorViewModel
import com.kaii.photos.models.editor.EditorViewModelFactory
import com.kaii.photos.ui.theme.PhotosTheme
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarBox
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarHostState

class EditingView : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type =
            intent.data?.let {
                val mimeType = applicationContext.contentResolver.getType(it) ?: "image/*"

                if (mimeType.contains("image")) MediaType.Image
                else MediaType.Video
            }

        if (type == null) {
            Toast.makeText(applicationContext, applicationContext.resources.getString(R.string.media_non_existent), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            val initialDarkMode =
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> 1
                    AppCompatDelegate.MODE_NIGHT_NO -> 2

                    else -> 0
                }

            val followDarkTheme by applicationContext.appModule.settings.lookAndFeel.getFollowDarkMode()
                .collectAsStateWithLifecycle(
                    initialValue = initialDarkMode
                )

            PhotosTheme(
                theme = followDarkTheme,
                dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val navController = rememberNavController()

                lavenderEdgeToEdge(
                    isDarkMode = isSystemInDarkTheme(),
                    navBarColor = MaterialTheme.colorScheme.surfaceContainer,
                    statusBarColor = MaterialTheme.colorScheme.background
                )

                CompositionLocalProvider(
                    LocalNavController provides navController
                ) {
                    val snackbarHostState = remember {
                        LavenderSnackbarHostState()
                    }

                    LavenderSnackbarBox(snackbarHostState = snackbarHostState) {
                        if (type == MediaType.Video) {
                            VideoEditor(
                                uri = intent.data!!.toString(),
                                window = window,
                                isFromOpenWithView = true,
                                album = null
                            )
                        } else {
                            val viewModel = viewModel<EditorViewModel>(
                                factory = EditorViewModelFactory(
                                    context = applicationContext,
                                    album = AlbumType.PlaceHolder
                                )
                            )

                            ImageEditor(
                                uri = intent.data!!.toString(),
                                info = { ImmichBasicInfo.Empty },
                                isFromOpenWithView = true,
                                exportQuality = { 8 },
                                overwriteByDefault = { false },
                                editImage = viewModel::editImage,
                                setNavProps = viewModel::setNavProps
                            )
                        }
                    }
                }
            }
        }
    }
}