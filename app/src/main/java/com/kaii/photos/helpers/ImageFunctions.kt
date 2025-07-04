package com.kaii.photos.helpers

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.toSize
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.R
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyMedia
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getOriginalPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

private const val TAG = "IMAGE_FUNCTIONS"

enum class ImageFunctions {
    LoadNormalImage,
    LoadTrashedImage,
    LoadSecuredImage
}

fun permanentlyDeletePhotoList(context: Context, list: List<Uri>) {
    if (list.isNotEmpty()) {
        val deleteRequest = MediaStore.createDeleteRequest(
            context.contentResolver,
            list
        )

        (context as Activity).startIntentSenderForResult(
            deleteRequest.intentSender,
            9997,
            null,
            0,
            0,
            0
        )
    }
}

// TODO: remove from favourites
/** @param list is a list of pairs of item uri and its absolute path */
fun setTrashedOnPhotoList(context: Context, list: List<Pair<Uri, String>>, trashed: Boolean) {
    val contentResolver = context.contentResolver

    val currentTimeMillis = System.currentTimeMillis()
    val trashedValues = ContentValues().apply {
        put(MediaColumns.IS_TRASHED, trashed)
    }

    try {
        list.forEach { (uri, path) ->
            // order is very important!
            // this WILL crash if you try to set last modified on a file that got moved from ex image.png to .trashed-{timestamp}-image.png
            File(path).setLastModified(currentTimeMillis)
            contentResolver.update(uri, trashedValues, null)
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Setting trashed $trashed on photo list failed.")
        e.printStackTrace()
    }
}

fun shareImage(uri: Uri, context: Context, mimeType: String? = null) {
    val contentResolver = context.contentResolver

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = mimeType ?: contentResolver.getType(uri)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    val chooserIntent = Intent.createChooser(shareIntent, null)
    context.startActivity(chooserIntent)
}

fun shareSecuredImage(absolutePath: String, context: Context) {
    val uri =
        FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(absolutePath))

    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = context.contentResolver.getType(uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    context.startActivity(Intent.createChooser(intent, "Share secured image"))
}

/** @param paths is a list of absolute paths and [MediaType]s of items */
fun shareMultipleSecuredImages(
    paths: List<Pair<String, MediaType>>,
    context: Context
) {
    val hasVideos = paths.any {
        it.second == MediaType.Video
    }

    val intent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = if (hasVideos) "video/*" else "images/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val fileUris = ArrayList(
        paths.map {
            FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(it.first))
        }
    )

    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

    context.startActivity(Intent.createChooser(intent, null))
}

fun moveImageToLockedFolder(
    list: List<MediaStoreData>,
    context: Context,
    onDone: () -> Unit
) {
    val contentResolver = context.contentResolver
    val lastModified = System.currentTimeMillis()
    val metadataRetriever = MediaMetadataRetriever()

    list.forEach { mediaItem ->
        // set last modified so item shows up in correct place in locked folder
        File(mediaItem.absolutePath).setLastModified(lastModified)

        val fileToBeHidden = File(mediaItem.absolutePath)
        val copyToPath = context.appSecureFolderDir + "/" + fileToBeHidden.name
        val destinationFile = File(copyToPath)

        if (mediaItem.type == MediaType.Image) {
            setDateTakenForMedia(
                mediaItem.absolutePath,
                mediaItem.dateTaken
            )
        }

        addSecuredCachedMediaThumbnail(
            context = context,
            mediaItem = mediaItem,
            file = fileToBeHidden,
            metadataRetriever = metadataRetriever
        )

        // encrypt file data and write to secure folder path
        val iv =
            EncryptionManager.encryptInputStream(
                fileToBeHidden.inputStream(),
                destinationFile.outputStream()
            )

        applicationDatabase.securedItemEntityDao().insertEntity(
            SecuredItemEntity(
                originalPath = mediaItem.absolutePath,
                securedPath = copyToPath,
                iv = iv
            )
        )

        // cleanup
        contentResolver.delete(mediaItem.uri, null)
        applicationDatabase.mediaEntityDao().deleteEntityById(mediaItem.id)
    }

    onDone()
}

suspend fun moveImageOutOfLockedFolder(
    list: List<MediaStoreData>,
    context: Context,
    onDone: () -> Unit
) {
    val contentResolver = context.contentResolver
    val restoredFilesDir = context.appRestoredFilesDir

    list.forEach { media ->
        val fileToBeRestored = File(media.absolutePath)
        val originalPath = media.bytes?.getOriginalPath() ?: restoredFilesDir

        Log.d(TAG, "ORIGINAL PATH $originalPath")

        val tempFile = File(context.cacheDir, fileToBeRestored.name)

        val iv = media.bytes?.getIv()
        try {
            if (iv != null) {
                EncryptionManager.decryptInputStream(
                    fileToBeRestored.inputStream(),
                    tempFile.outputStream(),
                    iv
                )
            } else {
                fileToBeRestored.inputStream().copyTo(tempFile.outputStream())
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()
        }

        Log.d(TAG, "Base path ${originalPath.toBasePath()}")

        contentResolver.copyMedia(
            context = context,
            media = media.copy(
                uri = tempFile.toUri()
            ),
            destination = originalPath.getParentFromPath(),
            overwriteDate = false,
            basePath = originalPath.toBasePath(),
            currentVolumes = MediaStore.getExternalVolumeNames(context)
        )?.let {
            try {
                fileToBeRestored.delete()
                tempFile.delete()
                applicationDatabase.securedItemEntityDao().deleteEntityBySecuredPath(media.absolutePath)

                val thumbnailFile =
                    getSecuredCacheImageForFile(file = fileToBeRestored, context = context)
                thumbnailFile.delete()
                applicationDatabase.securedItemEntityDao()
                    .deleteEntityBySecuredPath(thumbnailFile.absolutePath)
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }
        }
    }

    onDone()
}

/** @param list is a list of the absolute path of every image to be deleted */
fun permanentlyDeleteSecureFolderImageList(list: List<String>, context: Context) {
    try {
        list.forEach { path ->
            File(path).let { file ->
                file.delete()
                getSecuredCacheImageForFile(file = file, context = context).delete()
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

fun renameImage(context: Context, uri: Uri, newName: String) {
    CoroutineScope(Dispatchers.IO).launch {
        async {
            val contentResolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaColumns.DISPLAY_NAME, newName)
            }

            contentResolver.update(uri, contentValues, null)
            contentResolver.notifyChange(uri, null)
        }.await()
    }
}

fun renameDirectory(
    context: Context,
    absolutePath: String,
    base: String,
    newName: String,
) {
    try {
        val dir =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_DOCUMENTS_AUTHORITY,
                "$base:${absolutePath.toRelativePath(true)}"
            )

        Log.d(TAG, "Dir is $dir")

        val newDirectory = DocumentFile.fromTreeUri(context, dir)
        newDirectory?.renameTo(newName)
    } catch (e: Throwable) {
        Log.e(TAG, "Couldn't rename directory $absolutePath to $newName")
        e.printStackTrace()
    }
}

/** @param destination where to move said files to, should be relative*/
fun moveImageListToPath(
    context: Context,
    list: List<MediaStoreData>,
    destination: String,
    basePath: String,
    overwriteDate: Boolean
) {
    CoroutineScope(Dispatchers.IO).launch {
        val contentResolver = context.contentResolver

        async {
            list.forEach { media ->
                contentResolver.copyMedia(
                    context = context,
                    media = media,
                    destination = destination,
                    basePath = basePath,
                    overwriteDate = overwriteDate,
                    currentVolumes = MediaStore.getExternalVolumeNames(context)
                )?.let {
                    contentResolver.delete(media.uri, null)
                }
            }
        }.await()
    }
}

/** @param destination where to copy said files to, should be relative
@param overrideDisplayName should not contain file extension */
fun copyImageListToPath(
    context: Context,
    list: List<MediaStoreData>,
    destination: String,
    basePath: String,
    overwriteDate: Boolean,
    overrideDisplayName: ((displayName: String) -> String)? = null
) {
    CoroutineScope(Dispatchers.IO).launch {
        val contentResolver = context.contentResolver

        async {
            list.forEach { media ->
                Log.d(TAG, "Media modified date: ${media.dateModified}")
                contentResolver.copyMedia(
                    context = context,
                    media = media,
                    destination = destination,
                    overwriteDate = overwriteDate,
                    basePath = basePath,
                    overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
                    currentVolumes = MediaStore.getExternalVolumeNames(context)
                )
            }
        }.await()
    }
}

// TODO: scroll left one image
suspend fun modificationsToBitmap(
    modifications: List<Modification>,
    adjustmentColorMatrix: ColorMatrix,
    image: ImageBitmap,
    maxSize: Size,
    rotation: Float,
    textMeasurer: TextMeasurer,
): ImageBitmap {
    val defaultTextStyle = DrawableText.Styles.Default.style

    val blurredImage =
        if (modifications.any { it is DrawableBlur } && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            image.asAndroidBitmap().blur(blurRadius = 32f)
        } else {
            null
        }

    return withContext(Dispatchers.IO) {
        val rotationMatrix = android.graphics.Matrix().apply {
            postRotate(rotation)
        }

        val unadjustedImage = Bitmap.createBitmap(
            image.asAndroidBitmap(),
            0,
            0,
            image.width,
            image.height
        ).copy(Bitmap.Config.ARGB_8888, true)

        val colorFilteredPaint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(adjustmentColorMatrix)
        }
        val savedImage = createBitmap(unadjustedImage.width, unadjustedImage.height).asImageBitmap()
        val adjustCanvas = android.graphics.Canvas(savedImage.asAndroidBitmap())
        adjustCanvas.drawBitmap(unadjustedImage, 0f, 0f, colorFilteredPaint.asFrameworkPaint())

        val size = IntSize(
            savedImage.width,
            savedImage.height
        )

        val ratio = 1f / min(maxSize.width / size.width, maxSize.height / size.height)
        val drawScope = CanvasDrawScope()
        val canvas = androidx.compose.ui.graphics.Canvas(savedImage)

        drawScope.draw(
            Density(1f),
            LayoutDirection.Ltr,
            canvas,
            size.toSize()
        ) {
            modifications.forEach { modification ->
                when (modification) {
                    is DrawablePath -> {
                        val (path, paint) = modification
                        scale(ratio, Offset(0.5f, 0.5f)) {
                            drawPath(
                                path = path,
                                style = Stroke(
                                    width = paint.strokeWidth,
                                    cap = paint.strokeCap,
                                    join = paint.strokeJoin,
                                    miter = paint.strokeMiterLimit,
                                    pathEffect = paint.pathEffect
                                ),
                                blendMode = paint.blendMode,
                                color = paint.color,
                                alpha = paint.alpha
                            )
                        }
                    }

                    is DrawableBlur -> {
                        val (path, pathPaint) = modification

                        val offscreenBitmap = createBitmap(size.width, size.height)
                        val offscreenCanvas = android.graphics.Canvas(offscreenBitmap)

                        offscreenCanvas.scale(ratio, ratio, 0.5f, 0.5f)
                        offscreenCanvas.drawPath(path.asAndroidPath(), pathPaint.asFrameworkPaint())

                        drawIntoCanvas { canvas ->
                            val frameworkCanvas = canvas.nativeCanvas

                            val rectSize = android.graphics.RectF(
                                0f, 0f,
                                size.width.toFloat(), size.height.toFloat()
                            )

                            frameworkCanvas.saveLayer(rectSize, pathPaint.asFrameworkPaint())

                            frameworkCanvas.drawBitmap(
                                offscreenBitmap,
                                0f, 0f,
                                null
                            )

                            frameworkCanvas.drawBitmap(
                                blurredImage!!,
                                0f, 0f,
                                android.graphics.Paint().apply {
                                    // blendMode = BlendMode.DstIn
                                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                                }
                            )

                            frameworkCanvas.restore()
                        }
                    }

                    is DrawableText -> {
                        val (text, position, paint, textRotation, textSize) = modification

                        scale(ratio, Offset(0.5f, 0.5f)) {
                            rotate(textRotation, position + textSize.toOffset() / 2f) {
                                translate(position.x, position.y) {
                                    val textLayout = textMeasurer.measure(
                                        text = text,
                                        style = TextStyle(
                                            color = paint.color,
                                            fontSize = TextUnit(
                                                paint.strokeWidth,
                                                TextUnitType.Sp
                                            ), // TextUnit(text.paint.strokeWidth * 0.8f * ratio, TextUnitType.Sp)
                                            textAlign = defaultTextStyle.textAlign,
                                            platformStyle = defaultTextStyle.platformStyle,
                                            lineHeightStyle = defaultTextStyle.lineHeightStyle,
                                            baselineShift = defaultTextStyle.baselineShift
                                        )
                                    )

                                    drawText(
                                        textLayoutResult = textLayout,
                                        color = paint.color,
                                        alpha = paint.alpha,
                                        blendMode = paint.blendMode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val rotatedImage = Bitmap.createBitmap(
            savedImage.asAndroidBitmap(),
            0,
            0,
            image.width,
            image.height,
            rotationMatrix,
            false
        ).copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()

        return@withContext rotatedImage
    }
}

suspend fun saveToFile(
    absolutePath: String,
    overwrite: Boolean,
    context: Context,
    dateTaken: Long,
    rotatedImage: ImageBitmap,
    uri: Uri
) {
    val original = File(absolutePath)
    val format = when (original.extension) {
        "webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
        "jpeg", "jpg" -> Bitmap.CompressFormat.JPEG
        else -> Bitmap.CompressFormat.PNG
    }

    val currentTime = System.currentTimeMillis()

    val displayName = "${original.nameWithoutExtension}-edited"

    if (!overwrite) {
        val newUri = context.contentResolver.copyMedia(
            context = context,
            media = MediaStoreData(
                type = MediaType.Image,
                uri = uri,
                mimeType = "image/$format",
                absolutePath = absolutePath,
                dateModified = currentTime,
                dateTaken = dateTaken + 1,
                displayName = displayName,
                id = 0L
            ),
            destination = original.absolutePath.getParentFromPath(),
            overrideDisplayName = displayName,
            overwriteDate = true,
            basePath = absolutePath.toBasePath(),
            currentVolumes = MediaStore.getExternalVolumeNames(context)
        )

        val contentValues = ContentValues().apply {
            put(MediaColumns.DATE_ADDED, currentTime)
            put(MediaStore.Images.Media.DATE_TAKEN, dateTaken + 1)
        }

        val outputStream = newUri?.let { context.contentResolver.openOutputStream(newUri) }

        if (newUri != null && outputStream != null) {
            rotatedImage.asAndroidBitmap()
                .compress(format, 100, outputStream)
            outputStream.close()

            try {
                context.contentResolver.update(newUri, contentValues, null)
            } catch (e: Throwable) {
                Log.d(TAG, e.toString())
                e.printStackTrace()
            }
            File(absolutePath).setLastModified(currentTime)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = context.resources.getString(R.string.editing_done),
                    icon = R.drawable.checkmark_thin,
                    duration = SnackbarDuration.Short
                )
            )
        } else {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = context.resources.getString(R.string.editing_failed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Long
                )
            )
        }
    } else {
        val outputStream = context.contentResolver.openOutputStream(uri)

        if (outputStream != null) {
            rotatedImage.asAndroidBitmap()
                .compress(format, 100, outputStream)
            outputStream.close()

            // update date modified and invalidate cache by proxy
            File(absolutePath).setLastModified(currentTime)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = context.resources.getString(R.string.editing_done),
                    icon = R.drawable.checkmark_thin,
                    duration = SnackbarDuration.Short
                )
            )
        } else {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = context.resources.getString(R.string.editing_failed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Long
                )
            )
        }
    }
}