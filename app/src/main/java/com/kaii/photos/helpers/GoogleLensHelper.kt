package com.kaii.photos.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

private const val TAG = "GoogleLensHelper"
private const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
private const val GOOGLE_PHOTOS_PACKAGE = "com.google.android.apps.photos"
private const val GOOGLE_LENS_ACTIVITY = "com.google.android.apps.search.lens.LensActivity"
private const val LENS_URI = "google.lens.uri"
private const val LENS_ACTION = "com.google.android.gms.vision.SCAN"

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

    // Multi-layered approach with fallbacks - prioritizing direct Google app launch
    return tryDirectGoogleLensLaunch(context, contentUri) || // Try direct launch methods first
           trySendToGoogleApp(context, contentUri) ||        // Then try sending to Google app
           trySendToGooglePhotos(context, contentUri) ||     // Then try Google Photos
           tryShowChooser(context, contentUri) ||            // Then show chooser as fallback
           tryViewWithGoogleApp(context, contentUri) ||      // Then try VIEW intents
           tryWebSearch(context)                             // Web search as last resort
}

/**
 * Secondary Approach: Integration with Google app using ACTION_SEND intent
 */
private fun trySendToGoogleApp(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, GOOGLE_PACKAGE)) {
        Log.d(TAG, "Google app not installed, skipping secondary approach")
        return false
    }

    return try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(GOOGLE_PACKAGE)
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Check if the intent can be resolved
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched Google app with ACTION_SEND")
            return true
        }

        Log.d(TAG, "Google app doesn't support ACTION_SEND for images")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error sending to Google app: ${e.message}")
        false
    }
}

/**
 * Tertiary Approach: Send the image to Google Photos
 */
private fun trySendToGooglePhotos(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, GOOGLE_PHOTOS_PACKAGE)) {
        Log.d(TAG, "Google Photos not installed, skipping tertiary approach")
        return false
    }

    return try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(GOOGLE_PHOTOS_PACKAGE)
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra("lens", true) // Hint to open in Lens mode
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched Google Photos with ACTION_SEND")
            return true
        }

        Log.d(TAG, "Google Photos doesn't support ACTION_SEND for images")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error sending to Google Photos: ${e.message}")
        false
    }
}

/**
 * Quaternary Approach: Show a chooser dialog for the user to select an app
 *
 * This approach is used as a fallback when direct methods fail:
 * - It doesn't rely on a specific package being installed
 * - It allows the user to choose from all compatible apps
 * - It works even if Google Lens is integrated into a different app on the user's device
 */
private fun tryShowChooser(context: Context, contentUri: Uri): Boolean {
    return try {
        // Create the base intent for sharing
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Create a chooser with a custom title
        val chooser = Intent.createChooser(sendIntent, "Search with Google Lens")

        // Add flags to ensure proper launching
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Check if there are apps that can handle this intent
        if (chooser.resolveActivity(context.packageManager) != null) {
            // Launch the chooser dialog
            context.startActivity(chooser)
            Log.d(TAG, "Successfully launched chooser with ACTION_SEND")
            return true
        }

        Log.d(TAG, "No apps available to handle ACTION_SEND for images")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to launch chooser: ${e.message}")
        false
    }
}

/**
 * Quinary Approach: Try to use ACTION_VIEW with the Google app
 */
private fun tryViewWithGoogleApp(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, GOOGLE_PACKAGE)) {
        Log.d(TAG, "Google app not installed, skipping quinary approach")
        return false
    }

    // Try multiple known intents for Google Lens in the Google app
    val intents = listOf(
        // Method 1: Using specific action for Lens
        Intent("com.google.android.apps.lens.SEARCH_BY_IMAGE").apply {
            setPackage(GOOGLE_PACKAGE)
            setDataAndType(contentUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },

        // Method 2: Using ACTION_VIEW with lens parameter
        Intent(Intent.ACTION_VIEW).apply {
            setPackage(GOOGLE_PACKAGE)
            setDataAndType(contentUri, "image/*")
            putExtra("lens", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    )

    // Try each intent until one works
    for (intent in intents) {
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Successfully launched Google app with ACTION_VIEW")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with Google app intent: ${e.message}")
            // Continue to the next intent
        }
    }

    Log.d(TAG, "Google app doesn't support any of the ACTION_VIEW methods")
    return false
}

/**
 * Senary Approach: Open the web version of Google Lens as a last resort
 */
private fun tryWebSearch(context: Context): Boolean {
    return try {
        // Try to open Google Lens website
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com"))

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)

            Toast.makeText(
                context,
                "Opening Google Lens website as fallback",
                Toast.LENGTH_SHORT
            ).show()

            Log.d(TAG, "Successfully opened Google Lens website")
            return true
        }

        Log.d(TAG, "No web browser available to open Google Lens website")
        false
    } catch (e: ActivityNotFoundException) {
        // Show error message if even the web browser isn't available
        Toast.makeText(
            context,
            "Google Lens is not available on this device",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(TAG, "Error opening web browser: ${e.message}")
        false
    }
}

/**
 * Primary Approach: Direct launch of Google Lens using component name
 *
 * This approach directly targets the Google Lens activity within the Google app,
 * bypassing any chooser dialogs or intermediate steps.
 */
private fun tryDirectGoogleLensLaunch(context: Context, contentUri: Uri): Boolean {
    if (!isPackageInstalled(context, GOOGLE_PACKAGE)) {
        Log.d(TAG, "Google app not installed, skipping direct launch approach")
        return false
    }

    // Try multiple direct launch methods
    return tryComponentNameLaunch(context, contentUri) ||
           tryLensActionLaunch(context, contentUri) ||
           tryGoogleVisionLaunch(context, contentUri)
}

/**
 * Method 1: Using component name to directly target the Lens activity
 */
private fun tryComponentNameLaunch(context: Context, contentUri: Uri): Boolean {
    return try {
        val intent = Intent().apply {
            component = android.content.ComponentName(GOOGLE_PACKAGE, GOOGLE_LENS_ACTIVITY)
            action = Intent.ACTION_VIEW
            setDataAndType(contentUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched Google Lens directly with component name")
            return true
        }

        Log.d(TAG, "Could not resolve Google Lens activity with component name")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens with component name: ${e.message}")
        false
    }
}

/**
 * Method 2: Using the specific Lens action
 */
private fun tryLensActionLaunch(context: Context, contentUri: Uri): Boolean {
    return try {
        val intent = Intent("com.google.android.apps.lens.SEARCH_BY_IMAGE").apply {
            setPackage(GOOGLE_PACKAGE)
            setDataAndType(contentUri, "image/*")
            putExtra(LENS_URI, contentUri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched Google Lens with specific action")
            return true
        }

        Log.d(TAG, "Could not resolve Google Lens with specific action")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens with specific action: ${e.message}")
        false
    }
}

/**
 * Method 3: Using Google Vision scan action
 */
private fun tryGoogleVisionLaunch(context: Context, contentUri: Uri): Boolean {
    return try {
        val intent = Intent(LENS_ACTION).apply {
            setPackage(GOOGLE_PACKAGE)
            putExtra(LENS_URI, contentUri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched Google Lens with vision scan action")
            return true
        }

        Log.d(TAG, "Could not resolve Google Lens with vision scan action")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens with vision scan action: ${e.message}")
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
