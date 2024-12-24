package com.kaii.photos.helpers

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.toSize
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.math.min

private const val TAG = "IMAGE_FUNCTIONS"

enum class ImageFunctions {
    LoadNormalImage,
    LoadTrashedImage,
    LoadSecuredImage
}

fun permanentlyDeletePhotoList(context: Context, list: List<Uri>) {
    val contentResolver = context.contentResolver

    val hasManageMediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.checkPermission(
            Manifest.permission.MANAGE_MEDIA,
            android.os.Process.myPid(),
            android.os.Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }

    if (hasManageMediaPermission) {
        val request = MediaStore.createDeleteRequest(contentResolver, list)

        (context as Activity).startIntentSenderForResult(request.intentSender, 69, null, 0, 0, 0)
    } else {
        val chunks = list.chunked(100)

        CoroutineScope(Dispatchers.IO).launch {
            for (chunk in chunks) {
                chunk.forEach {
                    contentResolver.delete(it, null)
                }

                delay(3000)
            }
        }
    }
}

fun setTrashedOnPhotoList(context: Context, list: List<Pair<Uri, String>>, trashed: Boolean) {
    val contentResolver = context.contentResolver

    val currentTime = System.currentTimeMillis()
    list.forEach { (_, path) ->
        File(path).setLastModified(currentTime)
    }

    val hasManageMediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.checkPermission(
            Manifest.permission.MANAGE_MEDIA,
            android.os.Process.myPid(),
            android.os.Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }

    if (hasManageMediaPermission) {
        val request = MediaStore.createTrashRequest(contentResolver, list.map { it.first }, trashed)

        (context as Activity).startIntentSenderForResult(request.intentSender, 69, null, 0, 0, 0)
    } else {
        val trashedValues = ContentValues().apply {
            put(MediaColumns.IS_TRASHED, trashed)
        }
        val chunks = list.chunked(25)

        CoroutineScope(Dispatchers.IO).launch {
            for (chunk in chunks) {
                chunk.forEach { (uri, _) ->
                    contentResolver.update(uri, trashedValues, null)
                }

                delay(3000)
            }
        }
    }
}

fun shareImage(uri: Uri, context: Context, mimeType: String? = null) {
    val contentResolver = context.contentResolver

    CoroutineScope(Dispatchers.IO).launch {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType ?: contentResolver.getType(uri)
        }

        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)

        val chooserIntent = Intent.createChooser(shareIntent, null)
        context.startActivity(chooserIntent)
    }
}

fun moveImageToLockedFolder(absolutePath: String, id: Long, context: Context) {
    val fileToBeHidden = File(absolutePath)
    val lockedFolderDir = context.getAppLockedFolderDirectory()
    val copyToPath = lockedFolderDir + fileToBeHidden.name
    Files.copy(Path(absolutePath), Path(copyToPath), StandardCopyOption.REPLACE_EXISTING)
    copyExifDataToFile(absolutePath, copyToPath)
	Files.delete(Path(absolutePath))

    val lastModified = System.currentTimeMillis()
    CoroutineScope(EmptyCoroutineContext + CoroutineName("hide_file_context")).launch {
        File(copyToPath).setLastModified(lastModified)

        applicationDatabase.securedItemEntityDao().insertEntity(
            SecuredItemEntity(
                originalPath = absolutePath,
                securedPath = copyToPath
            )
        )
        applicationDatabase.mediaEntityDao().deleteEntityById(id)
    }
}

/** @param path is the secured folder path (/data/ path) to this item */
fun moveImageOutOfLockedFolder(path: String) {
    val fileToBeRevived = File(path)
    val absolutePath = fileToBeRevived.absolutePath

    CoroutineScope(Dispatchers.IO).launch {
        val reverseCemetery =
            applicationDatabase.securedItemEntityDao().getOriginalPathFromSecuredPath(path)
                ?: (getAppRestoredFromLockedFolderDirectory() + fileToBeRevived.name)

        Files.copy(Path(absolutePath), Path(reverseCemetery), StandardCopyOption.REPLACE_EXISTING)
		copyExifDataToFile(absolutePath, reverseCemetery)
		Files.delete(Path(absolutePath))

        val lastModified = System.currentTimeMillis()
        File(reverseCemetery).setLastModified(lastModified)
        applicationDatabase.securedItemEntityDao().deleteEntityBySecuredPath(path)
    }
}

/** @param list is a list of the absolute path of every image to be deleted */
fun permanentlyDeleteSecureFolderImageList(list: List<String>) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            list.forEach { path ->
                File(path).delete()
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
        }
    }
}

fun renameImage(context: Context, uri: Uri, newName: String) {
    val contentResolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaColumns.DISPLAY_NAME, newName)
    }

    contentResolver.update(uri, contentValues, null)
    contentResolver.notifyChange(uri, null)
}

fun renameDirectory(path: String, newName: String) {
    val originalDir = File(path)
    val newDirAbsolutePath = path.replace(originalDir.name, newName)

    try {
        originalDir.renameTo(File(newDirAbsolutePath))
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

/** @param destination where to move said files to, should be relative*/
fun moveImageListToPath(context: Context, list: List<MediaStoreData>, destination: String) {
    val contentResolver = context.contentResolver

    CoroutineScope(Dispatchers.IO).launch {
        list.forEach { media ->
            val path = destination.removeSuffix("/") + "/"
            val contentValues = ContentValues().apply {
                put(MediaColumns.RELATIVE_PATH, path)
            }

            contentResolver.update(media.uri, contentValues, null)
            delay(50)
        }
    }
}

/** @param destination where to copy said files to, should be relative*/
fun copyImageListToPath(context: Context, list: List<MediaStoreData>, destination: String) {
    val contentResolver = context.contentResolver

    CoroutineScope(Dispatchers.IO).launch {
        list.forEach { media ->
            val file = File(media.absolutePath)
            val path = getBaseInternalStorageDirectory() + destination.removeSuffix("/") + "/${file.name}"
            val inputStream = contentResolver.openInputStream(media.uri)
            val outputStream = File(path).outputStream()

            if (inputStream != null) {
                inputStream.copyTo(outputStream)
            } else {
                Log.e(TAG, "The input stream for uri $media was null")
            }

            outputStream.close()
            inputStream?.close()

            delay(50)
        }
    }
}

suspend fun savePathListToBitmap(
    modifications: List<Modification>,
    adjustmentColorMatrix: ColorMatrix,
    absolutePath: String,
    dateTaken: Long,
    uri: Uri,
    image: ImageBitmap,
    maxSize: Size,
    rotation: Float,
    textMeasurer: TextMeasurer,
    overwrite: Boolean,
    context: Context
) {
    val defaultTextStyle = DrawableText.Styles.Default.style

    withContext(Dispatchers.IO) {
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
        val savedImage = Bitmap.createBitmap(unadjustedImage.width, unadjustedImage.height, Bitmap.Config.ARGB_8888).asImageBitmap()
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
                if (modification is DrawablePath) {
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
                } else if (modification is DrawableText) {
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

        val rotatedImage = Bitmap.createBitmap(
            savedImage.asAndroidBitmap(),
            0,
            0,
            image.width,
            image.height,
            rotationMatrix,
            false
        ).copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()

        val original = File(absolutePath)
        val format = when (original.extension) {
            "webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
            "jpeg", "jpg" -> Bitmap.CompressFormat.JPEG
            else -> Bitmap.CompressFormat.PNG
        }

        val currentTime = System.currentTimeMillis()

        // change the "edited at" thing to make more sense, like copy(1) copy(2) or something
        val displayName = "${original.nameWithoutExtension}-edited-at-$currentTime.png"
        val newPath = if (!overwrite) {
            original.absolutePath.replace(
                original.name,
                displayName
            )
        } else {
            original.absolutePath
        }

        if (!overwrite) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.DATE_MODIFIED, currentTime)
                put(MediaStore.Images.Media.DATE_ADDED, currentTime)
                put(MediaColumns.DATE_ADDED, currentTime)
                put(MediaStore.Images.Media.MIME_TYPE, "image/$format")
                put(MediaStore.Images.Media.DATE_TAKEN, dateTaken + 1)
                put(MediaStore.Images.Media.RELATIVE_PATH, original.absolutePath.replace(original.name, ""))
                put(MediaStore.Images.Media.DATA, newPath)
            }

            val copyUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            val outputStream = copyUri?.let {
                context.contentResolver.openOutputStream(it)
            } ?: FileOutputStream(File(newPath))

            rotatedImage.asAndroidBitmap()
                .compress(format, 100, outputStream)
            outputStream.close()
        } else {
            val outputStream = context.contentResolver.openOutputStream(uri) ?: FileOutputStream(File(newPath))

            rotatedImage.asAndroidBitmap()
                .compress(format, 100, outputStream)
            outputStream.close()

            // update date modified and invalidate cache by proxy
            context.contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.DATE_MODIFIED, currentTime)
                    put(MediaStore.Images.Media.DATE_ADDED, currentTime)
                },
                null
            )
        }
    }
}
