package com.kaii.photos.helpers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
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
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.setAttribute
import kotlin.math.min

private const val TAG = "IMAGE_FUNCTIONS"

enum class ImageFunctions {
    LoadNormalImage,
    LoadTrashedImage,
    LoadSecuredImage
}

fun permanentlyDeletePhotoList(context: Context, list: List<Uri>) {
    val contentResolver = context.contentResolver

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

fun setTrashedOnPhotoList(context: Context, list: List<Uri>, trashed: Boolean) {
    val contentResolver = context.contentResolver

    val trashedValues = ContentValues().apply {
        put(MediaColumns.IS_TRASHED, trashed)
    }

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                contentResolver.update(it, trashedValues, null)
            }

            delay(3000)
        }
    }
}

fun shareImage(uri: Uri, context: Context) {
    val contentResolver = context.contentResolver

    CoroutineScope(Dispatchers.IO).launch {
        val mimeType = contentResolver.getType(uri)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType
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
    Files.move(Path(absolutePath), Path(copyToPath), StandardCopyOption.REPLACE_EXISTING)

    val lastModified = System.currentTimeMillis()
    CoroutineScope(EmptyCoroutineContext + CoroutineName("hide_file_context")).launch {
        Path(copyToPath).setAttribute(
            BasicFileAttributes::lastModifiedTime.name,
            FileTime.fromMillis(lastModified)
        )

        applicationDatabase.securedItemEntityDao().insertEntity(
            SecuredItemEntity(
                originalPath = absolutePath,
                securedPath = copyToPath
            )
        )
        applicationDatabase.mediaEntityDao().deleteEntityById(id)
    }
    File(copyToPath).lastModified()
}

/** @param path is the secured folder path (/data/ path) to this item */
fun moveImageOutOfLockedFolder(path: String) {
    val fileToBeRevived = File(path)
    val absolutePath = fileToBeRevived.absolutePath

    CoroutineScope(Dispatchers.IO).launch {
        val reverseCemetery =
            applicationDatabase.securedItemEntityDao().getOriginalPathFromSecuredPath(path)
                ?: (getAppRestoredFromLockedFolderDirectory() + fileToBeRevived.name)

        Files.move(Path(absolutePath), Path(reverseCemetery), StandardCopyOption.REPLACE_EXISTING)

        val lastModified = System.currentTimeMillis()
        Path(reverseCemetery).setAttribute(
            BasicFileAttributes::lastModifiedTime.name,
            FileTime.fromMillis(lastModified)
        )
        File(reverseCemetery).lastModified()
        applicationDatabase.securedItemEntityDao().deleteEntityBySecuredPath(path)
    }
}

/** @param list is a list of the absolute path of every image to be deleted */
fun permanentlyDeleteSecureFolderImageList(list: List<String>) {
    try {
        list.forEach { path ->
            File(path).delete()
        }
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
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

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                val path = destination.removeSuffix("/") + "/"
                val contentValues = ContentValues().apply {
                    put(MediaColumns.RELATIVE_PATH, path)
                }
                contentResolver.update(it.uri, contentValues, null)
            }

            delay(3000)
        }
    }
}

/** @param destination where to copy said files to, should be relative*/
fun copyImageListToPath(context: Context, list: List<MediaStoreData>, destination: String) {
    val contentResolver = context.contentResolver

    val chunks = list.chunked(100)

    CoroutineScope(Dispatchers.IO).launch {
        for (chunk in chunks) {
            chunk.forEach {
                val file = File(it.absolutePath)
                val path = "/storage/emulated/0/" + destination.removeSuffix("/") + "/${file.name}"
                val inputStream = contentResolver.openInputStream(it.uri)
                val outputStream = File(path).outputStream()

                inputStream?.copyTo(outputStream)

                outputStream.close()
                inputStream?.close()
            }
        }
    }
}

suspend fun savePathListToBitmap(
    modifications: List<Modification>,
    absolutePath: String,
    image: ImageBitmap,
    maxSize: Size,
    rotation: Float,
    textMeasurer: TextMeasurer,
) {
    val defaultTextStyle = DrawableText.Styles.Default.style

    withContext(Dispatchers.IO) {
        val rotationMatrix = android.graphics.Matrix().apply {
            postRotate(rotation)
        }

//        var image = originalImage
//        modifications.filterIsInstance<CropRectangle>().forEach { (topLeftOffset, topRightOffset, bottomLeftOffset, bottomRightOffset, size) ->
//            val ratio = 1f / min(
//                size.width.toFloat() / image.width.toFloat(),
//                size.height.toFloat() / image.height.toFloat()
//            )
//
//            val x = (topLeftOffset.x * ratio).toInt()
//            val y = (topLeftOffset.y * ratio).toInt()
//            var width = ((topRightOffset.x - topLeftOffset.x) * ratio)
//                .toInt()
//                .coerceIn(0, image.width)
//            var height = ((bottomLeftOffset.y - topLeftOffset.y) * ratio)
//                .toInt()
//                .coerceIn(0, image.height)
//
//            val widthExtra = (x + width) - image.width
//            val heightExtra = (y + height) - image.height
//
//            if (widthExtra > 0) width -= widthExtra
//            if (heightExtra > 0) height -= heightExtra
//
//            if (width > 0 && height > 0) {
//                image = Bitmap
//                    .createBitmap(
//                        image,
//                        x,
//                        y,
//                        width,
//                        height
//                    )
//            }
//        }

        val savedImage = Bitmap.createBitmap(
            image.asAndroidBitmap(),
            0,
            0,
            image.width,
            image.height
        )
            .copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
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
        // change the "edited at" thing to make more sense, like copy(1) copy(2) or something
        val newPath = original.absolutePath.replace(
            original.name,
            original.nameWithoutExtension + "-edited-at-" + System.currentTimeMillis() + ".png"
        )

        val newFile = File(newPath)
        val fileOutputStream = FileOutputStream(newFile)

        rotatedImage.asAndroidBitmap()
            .compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()
    }
}
