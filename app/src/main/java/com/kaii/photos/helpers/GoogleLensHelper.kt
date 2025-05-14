package com.kaii.photos.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper function to launch Google Lens with the given image URI
 *
 * @param uri The URI of the image to search with Google Lens
 * @param context The context to use for launching the intent
 * @return True if Google Lens was launched successfully, false otherwise
 */
fun searchWithGoogleLens(uri: Uri, context: Context): Boolean {
    try {
        // Create an intent with the Google Lens action
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage("com.google.android.googlequicksearchbox")

        // Get the content URI using FileProvider
        val contentUri = if (uri.scheme == "content") {
            uri
        } else {
            val file = File(uri.path ?: return false)
            FileProvider.getUriForFile(
                context,
                "com.kaii.photos.LavenderPhotos.fileprovider",
                file
            )
        }

        // Set the data and type for the intent
        intent.setDataAndType(contentUri, "image/*")

        // Add flags to grant read permission to the receiving app
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Add the lens activity if available
        intent.action = "com.google.android.apps.lens.SEARCH_BY_IMAGE"

        // Launch the intent
        context.startActivity(intent)
        return true
    } catch (e: ActivityNotFoundException) {
        // Google app is not installed or doesn't support Lens
        try {
            // Try to open Google Lens directly if available
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com"))
            context.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            // Show error message
            Toast.makeText(
                context,
                "Google Lens is not available on this device",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    } catch (e: Exception) {
        // Handle other exceptions
        Toast.makeText(
            context,
            "Error launching Google Lens: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
        return false
    }
}
