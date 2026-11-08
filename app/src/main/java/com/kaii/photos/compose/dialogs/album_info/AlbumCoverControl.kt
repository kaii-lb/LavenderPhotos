package com.kaii.photos.compose.dialogs.album_info

import android.graphics.Bitmap
import android.graphics.Canvas as BitmapCanvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumCoverStore
import com.kaii.photos.helpers.AlbumCoverCrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AlbumCoverControl(albumId: String, modifier: Modifier = Modifier) {
    val store = PhotosApplication.appModule.settings.albums.covers
    val covers by remember(store) { store.get() }.collectAsStateWithLifecycle(initialValue = emptyMap())
    val scope = rememberCoroutineScope()
    var pendingUri by rememberSaveable(albumId) { mutableStateOf<String?>(null) }
    var failed by remember(albumId) { mutableStateOf(false) }
    var resetting by remember(albumId) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            failed = false
            pendingUri = uri.toString()
        }
    }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = !resetting,
                onClick = { picker.launch(arrayOf("image/*")) }
            ) {
                Text(stringResource(R.string.albums_cover_choose))
            }
            if (albumId in covers) {
                TextButton(
                    enabled = !resetting,
                    onClick = {
                        scope.launch {
                            resetting = true
                            try {
                                store.remove(setOf(albumId))
                                failed = false
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                failed = true
                            } finally {
                                resetting = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.albums_cover_reset))
                }
            }
        }
        if (failed) {
            Text(stringResource(R.string.albums_cover_error), color = MaterialTheme.colorScheme.error)
        }
    }

    pendingUri?.let { uri ->
        AlbumCoverCropDialog(
            uri = uri,
            albumId = albumId,
            store = store,
            onDismiss = { pendingUri = null }
        )
    }
}

@Composable
private fun AlbumCoverCropDialog(
    uri: String,
    albumId: String,
    store: AlbumCoverStore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(uri) { mutableStateOf(false) }
    var saveFailed by remember(uri) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var zoom by rememberSaveable(uri) { mutableFloatStateOf(1f) }
    var horizontal by rememberSaveable(uri) { mutableFloatStateOf(0.5f) }
    var vertical by rememberSaveable(uri) { mutableFloatStateOf(0.5f) }

    LaunchedEffect(uri) {
        try {
            bitmap = withContext(Dispatchers.IO) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri.toUri())) { decoder, info, _ ->
                    // ImageDecoder applies EXIF orientation, including HEIF images.
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val largest = max(info.size.width, info.size.height)
                    if (largest > 2048) {
                        val ratio = 2048f / largest
                        decoder.setTargetSize(
                            (info.size.width * ratio).roundToInt().coerceAtLeast(1),
                            (info.size.height * ratio).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.albums_cover_crop)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val source = bitmap
                if (source != null) {
                    val image = remember(source) { source.asImageBitmap() }
                    val crop = AlbumCoverCrop.calculate(source.width, source.height, zoom, horizontal, vertical)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clipToBounds()
                            .pointerInput(source, saving) {
                                if (!saving) detectTransformGestures { _, pan, gestureZoom, _ ->
                                    val old = AlbumCoverCrop.calculate(source.width, source.height, zoom, horizontal, vertical)
                                    zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                    val next = AlbumCoverCrop.calculate(source.width, source.height, zoom, horizontal, vertical)
                                    val dx = source.width - next.size
                                    val dy = source.height - next.size
                                    horizontal = if (dx > 0) {
                                        ((old.left + (old.size - next.size) / 2f - pan.x * next.size / size.width) / dx).coerceIn(0f, 1f)
                                    } else 0.5f
                                    vertical = if (dy > 0) {
                                        ((old.top + (old.size - next.size) / 2f - pan.y * next.size / size.height) / dy).coerceIn(0f, 1f)
                                    } else 0.5f
                                }
                            }
                    ) {
                        drawImage(
                            image = image,
                            srcOffset = IntOffset(crop.left, crop.top),
                            srcSize = IntSize(crop.size, crop.size),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        )
                    }
                    Text(stringResource(R.string.albums_cover_crop_hint))
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it },
                        valueRange = 1f..5f,
                        enabled = !saving
                    )
                } else if (!loadFailed) {
                    CircularProgressIndicator()
                }

                if (loadFailed || saveFailed) {
                    Text(stringResource(R.string.albums_cover_error), color = MaterialTheme.colorScheme.error)
                }
                if (saving) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(
                enabled = bitmap != null && !saving,
                onClick = {
                    val source = bitmap ?: return@TextButton
                    val crop = AlbumCoverCrop.calculate(source.width, source.height, zoom, horizontal, vertical)
                    saving = true
                    saveFailed = false
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val dimension = crop.size.coerceAtMost(768)
                                val output = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
                                try {
                                    val canvas = BitmapCanvas(output)
                                    canvas.drawColor(android.graphics.Color.BLACK)
                                    canvas.drawBitmap(
                                        source,
                                        Rect(crop.left, crop.top, crop.left + crop.size, crop.top + crop.size),
                                        Rect(0, 0, dimension, dimension),
                                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                                    )
                                    store.save(albumId, output)
                                } finally {
                                    output.recycle()
                                }
                            }
                            onDismiss()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            saveFailed = true
                        } finally {
                            saving = false
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.albums_cover_save))
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text(stringResource(R.string.media_cancel))
            }
        }
    )
}
