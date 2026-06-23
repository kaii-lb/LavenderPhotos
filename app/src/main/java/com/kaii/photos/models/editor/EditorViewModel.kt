package com.kaii.photos.models.editor

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.file_management.editing.CustomFileEditor
import com.kaii.photos.file_management.editing.GenericFileEditor
import com.kaii.photos.file_management.editing.HybridFileEditor
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(
    context: Context,
    album: AlbumType
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings
    private var exitOnSave = false

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val overwriteByDefault = settings.editing.getOverwriteByDefault().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    /** 2 to 8 (20% to 80%) */
    val exportQuality = settings.storage.getExportQuality().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = 8
    )

    val immichInfo = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
    )

    var newId: Long? = null

    private val db = MediaDatabase.getInstance(context.applicationContext)
    private val assetsClient = AssetsClient(
        client = context.appModule.apiClient,
        endpoint = "",
        auth = Auth.None
    )
    private val albumsClient = AlbumsClient(
        client = context.appModule.apiClient,
        endpoint = "",
        auth = Auth.None
    )

    private val editor =
        when (album) {
            is AlbumType.Custom -> {
                CustomFileEditor(
                    customDao = db.customDao(),
                    mediaDao = db.mediaDao(),
                    albumId = album.id
                )
            }

            else -> {
                HybridFileEditor(
                    mediaDao = db.mediaDao(),
                    assetsClient = assetsClient,
                    albumsClient = albumsClient,
                    albumImmichId = album.immichId.takeIf { album !is AlbumType.PlaceHolder }
                )
            }
        }

    init {
        viewModelScope.launch {
            launch {
                settings.immich.getImmichBasicInfo().collect { info ->
                    assetsClient.setEndpoint(info.endpoint)
                    assetsClient.setAuth(info.auth)

                    albumsClient.setEndpoint(info.endpoint)
                    albumsClient.setAuth(info.auth)
                }
            }

            launch {
                settings.editing.getExitOnSave().collect {
                    exitOnSave = it
                }
            }
        }
    }

    fun editImage(
        navController: NavController,
        params: GenericFileEditor.EditParameters.Image
    ) {
        params.context.appModule.scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = params.context.resources.getString(R.string.editing_saving),
                    icon = R.drawable.image_arrow_up,
                    isLoading = isLoading
                )
            )

            val result = editor.editImage(
                context = params.context,
                image = params.image,
                uri = params.uri,
                containerDimens = params.containerDimens,
                exportQuality = params.exportQuality,
                drawingPaintState = params.drawingPaintState,
                imageEditingState = params.imageEditingState,
                modifications = params.modifications,
                textMeasurer = params.textMeasurer,
                actualLeft = params.actualLeft,
                actualTop = params.actualTop,
                overwrite = params.overwrite,
                isFromOpenWithView = params.isFromOpenWithView
            )

            isLoading.value = false

            if (result == null) {
                LavenderSnackbarController.pushEvent(
                    event = LavenderSnackbarEvent.MessageEvent(
                        message = params.context.resources.getString(R.string.editing_export_image_failed),
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            newId = result
            setNavProps(navController)

            if (exitOnSave && result != null && !params.isFromOpenWithView) launch(Dispatchers.Main) { // need to be on main thread
                navController.popBackStack()
            }
        }
    }

    fun editVideo(
        navController: NavController,
        params: GenericFileEditor.EditParameters.Video
    ) {
        params.context.appModule.scope.launch {
            val result = editor.editVideo(
                context = params.context,
                modifications = params.modifications,
                videoEditingState = params.videoEditingState,
                basicVideoData = params.basicVideoData,
                uri = params.uri,
                info = params.info,
                overwrite = params.overwrite,
                containerDimens = params.containerDimens,
                canvasSize = params.canvasSize,
                textMeasurer = params.textMeasurer,
                isFromOpenWithView = params.isFromOpenWithView
            )

            if (result == null) {
                LavenderSnackbarController.pushEvent(
                    event = LavenderSnackbarEvent.MessageEvent(
                        message = params.context.resources.getString(R.string.editing_export_video_failed),
                        icon = R.drawable.error_2,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            newId = result
            setNavProps(navController)

            if (exitOnSave && result != null && !params.isFromOpenWithView) launch(Dispatchers.Main) { // need to be on main thread
                navController.popBackStack()
            }
        }
    }

    fun setNavProps(
        navController: NavController
    ) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("editId", newId)
    }
}