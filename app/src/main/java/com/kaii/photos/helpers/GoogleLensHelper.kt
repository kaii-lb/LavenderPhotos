package com.kaii.photos.helpers

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

private const val TAG = "GoogleLensHelper"

/**
 * Helper function to launch Google Lens with the given image URI
 *
 * @param uri The URI of the image to search with Google Lens
 * @param context The context to use for launching the intent
 * @return True if Google Lens was launched successfully, false otherwise
 */
fun searchWithGoogleLens(uri: Uri, context: Context): Boolean {
    // Get the content URI using FileProvider
    val contentUri = if (uri.scheme == "content") {
        uri
    } else {
        try {
            val file = File(uri.path ?: return false)
            FileProvider.getUriForFile(
                context,
                "com.kaii.photos.LavenderPhotos.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating content URI: ${e.message}")
            Toast.makeText(
                context,
                "Error preparing image for Google Lens",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    // Try multiple methods to launch Google Lens
    return tryGoogleLensApp(context, contentUri) ||
           tryGoogleApp(context, contentUri) ||
           tryGooglePhotosApp(context, contentUri) ||
           tryWebSearch(context)
}

/**
 * Try to launch the dedicated Google Lens app if available
 */
private fun tryGoogleLensApp(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, "com.google.ar.lens")) {
        return false
    }

    return try {
        val intent = Intent("com.google.ar.lens.main.SEARCH_BY_IMAGE")
        intent.setPackage("com.google.ar.lens")
        intent.setDataAndType(contentUri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens app: ${e.message}")
        false
    }
}

/**
 * Try to launch Google Lens through the Google app
 */
private fun tryGoogleApp(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, "com.google.android.googlequicksearchbox")) {
        return false
    }

    // Try multiple known intents for Google Lens in the Google app
    val intents = listOf(
        // Method 1: Using specific action
        Intent("com.google.android.apps.lens.SEARCH_BY_IMAGE").apply {
            setPackage("com.google.android.googlequicksearchbox")
            setDataAndType(contentUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },

        // Method 2: Using component name
        Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.apps.lens.MainActivity"
            )
            setDataAndType(contentUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },

        // Method 3: Using VIEW action with specific package
        Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.google.android.googlequicksearchbox")
            setDataAndType(contentUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("lens", true)
        }
    )

    // Try each intent until one works
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error with Google app intent: ${e.message}")
            // Continue to the next intent
        }
    }

    return false
}

/**
 * Try to launch Google Lens through Google Photos
 */
private fun tryGooglePhotosApp(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, "com.google.android.apps.photos")) {
        return false
    }

    return try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage("com.google.android.apps.photos")
        intent.setDataAndType(contentUri, "image/*")
        intent.putExtra("lens", true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Photos for Lens: ${e.message}")
        false
    }
}

/**
 * Fall back to web search if all else fails
 */
private fun tryWebSearch(context: Context): Boolean {
    return try {
        // Try to open Google Lens website
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com"))
        context.startActivity(intent)

        Toast.makeText(
            context,
            "Opening Google Lens website as fallback",
            Toast.LENGTH_SHORT
        ).show()
        true
    } catch (e: ActivityNotFoundException) {
        // Show error message if even the web browser isn't available
        Toast.makeText(
            context,
            "Google Lens is not available on this device",
            Toast.LENGTH_SHORT
        ).show()
        false
    }
}

/**
 * Check if a package is installed on the device
 */
private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
