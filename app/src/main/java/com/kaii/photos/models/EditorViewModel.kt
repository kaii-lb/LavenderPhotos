package com.kaii.photos.models

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.Settings
import com.kaii.photos.di.ApplicationScope
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.editing.GenericFileEditor
import com.kaii.photos.file_management.editing.HybridFileEditor
import com.kaii.photos.models.traits.PrepareFileForWriteImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditorViewModel.Factory::class)
class EditorViewModel @AssistedInject constructor(
    @Assisted album: AlbumType,
    editorFactory: HybridFileEditor.Factory,
    settings: Settings,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ViewModel(), PrepareFileForWriteImpl {
    @AssistedFactory
    interface Factory {
        fun create(album: AlbumType): EditorViewModel
    }

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

    val overwriteByDefault = settings.behaviour.getEditingOverwriteByDefault().stateIn(
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

    val exitOnSave = settings.behaviour.getEditingExitOnSave().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    var newId: Long? = null

    private val editor = editorFactory.create(album)

    private val progressChannel = Channel<FileOperationProgress<Unit>>(Channel.BUFFERED)
    val fileOperationProgress = progressChannel.receiveAsFlow()

    private val _navChannel = Channel<Long?>(Channel.BUFFERED)
    val navIdFlow = _navChannel.receiveAsFlow()

    private fun editImage(
        params: GenericFileEditor.EditParameters.Image
    ) {
        PhotosApplication.appModule.scope.launch {
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

            _navChannel.send(newId)
        }
    }

    private fun editVideo(
        params: GenericFileEditor.EditParameters.Video
    ) {
        PhotosApplication.appModule.scope.launch {
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
            _navChannel.send(newId)
        }
    }

    fun runAction(action: FileOperationAction) {
        when (action) {
            is FileOperationAction.PrepareFilesForWrite -> editor.prepareFileForWrite(action.files, action.followUpAction, progressChannel, appScope)

            is FileOperationAction.SaveEditedMedia -> {
                appScope.launch {
                    if (action.params is GenericFileEditor.EditParameters.Image) {
                        editImage(action.params)
                    } else {
                        editVideo(action.params as GenericFileEditor.EditParameters.Video)
                    }
                }
            }

            else -> Unit
        }
    }
}