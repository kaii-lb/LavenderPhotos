package com.kaii.photos.compose.open_with_view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.lavenderEdgeToEdge
import com.kaii.photos.compose.editing_view.image_editor.ImageEditor
import com.kaii.photos.compose.editing_view.video_editor.VideoEditor
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.EditorViewModel
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
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
            val themeSerial by PhotosApplication.appModule.settings.lookAndFeel
                .getThemeConfiguration()
                .collectAsStateWithLifecycle(initialValue = ThemeConfiguration.Default.serialize())

            PhotosTheme(
                theme = ThemeConfiguration(themeSerial)
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
                        val viewModel = hiltViewModel<EditorViewModel, EditorViewModel.Factory> { factory ->
                            factory.create(AlbumType.PlaceHolder)
                        }

                        if (type == MediaType.Video) {
                            VideoEditor(
                                file = FileOperationItemMetadata(
                                    id = 0L,
                                    uri = intent.data!!.toString(),
                                    absolutePath = "",
                                    isImage = false,
                                    immichUrl = null,
                                    parentPath = ""
                                ),
                                window = window,
                                isFromOpenWithView = true,
                                viewModel = viewModel
                            )
                        } else {
                            val exitOnSave by viewModel.exitOnSave.collectAsStateWithLifecycle()

                            ImageEditor(
                                file = FileOperationItemMetadata(
                                    id = 0L,
                                    uri = intent.data!!.toString(),
                                    absolutePath = "",
                                    isImage = true,
                                    immichUrl = null,
                                    parentPath = ""
                                ),
                                info = { ImmichBasicInfo.Empty },
                                isFromOpenWithView = true,
                                exportQuality = { 8 },
                                overwriteByDefault = { false },
                                exitOnSave = { exitOnSave },
                                navIdFlow = viewModel.navIdFlow,
                                fileOperationProgress = viewModel.fileOperationProgress,
                                runAction = viewModel::runAction
                            )
                        }
                    }
                }
            }
        }
    }
}