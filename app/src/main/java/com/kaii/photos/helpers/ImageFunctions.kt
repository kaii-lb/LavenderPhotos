package com.kaii.photos.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaStoreDataForIds
import com.kaii.photos.mediastore.insertMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/** @param destination where to copy said files to, should be relative
@param overrideDisplayName should not contain file extension */
suspend fun copyImageListToPath(
    context: Context,
    list: List<SelectionManager.SelectedItem>,
    destination: String,
    overrideDisplayName: (originalName: String) -> String
): MutableList<Uri> = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver

    val items = getMediaStoreDataForIds(
        ids = list.fastMap { it.id }.toSet(),
        context = context
    )

    val newUris = mutableListOf<Uri>()
    items.forEach { media ->
        contentResolver.insertMedia(
            context = context,
            media = media.copy(
                displayName = overrideDisplayName(media.displayName)
            ),
            destination = destination
        )?.let {
            contentResolver.copyUriToUri(media.uri.toUri(), it)
            newUris.add(it)
        }
    }

    return@withContext newUris
}