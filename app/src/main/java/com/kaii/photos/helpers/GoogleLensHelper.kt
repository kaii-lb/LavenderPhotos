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

// Primary Google app package
private const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
// Alternative Google app packages that might be present on different devices
private val ALTERNATIVE_GOOGLE_PACKAGES = listOf(
    "com.google.android.googlequicksearchbox",  // Standard Google app
    "com.google.android.quicksearchbox",        // Older Google search app
    "com.google.android.launcher",              // Google Now Launcher
    "com.google.android.apps.nexuslauncher",    // Pixel Launcher
    "com.google.android.gms"                    // Google Play Services
)

// Google Photos package
private const val GOOGLE_PHOTOS_PACKAGE = "com.google.android.apps.photos"
// Alternative Google Photos packages
private val ALTERNATIVE_PHOTOS_PACKAGES = listOf(
    "com.google.android.apps.photos",           // Standard Google Photos
    "com.google.android.apps.plus"              // Google+ (older devices)
)

// Google Lens activity and constants
private const val GOOGLE_LENS_ACTIVITY = "com.google.android.apps.search.lens.LensActivity"
private const val LENS_URI = "google.lens.uri"
private const val LENS_ACTION = "com.google.android.gms.vision.SCAN"

// Debug flag - set to true for detailed logging
private const val DEBUG = true

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
    if (DEBUG) {
        Log.d(TAG, "Attempting to send to Google app...")
    }

    val (installed, packageName) = isPackageInstalled(context, GOOGLE_PACKAGE, ALTERNATIVE_GOOGLE_PACKAGES)
    if (!installed) {
        Log.d(TAG, "No Google app found, skipping secondary approach")
        return false
    }

    if (DEBUG) {
        Log.d(TAG, "Using Google package: $packageName for ACTION_SEND")
    }

    return try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(packageName)
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            // Add hints that this is for lens/search
            putExtra("lens", true)
            putExtra("vision_mode", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if the intent can be resolved
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched Google app with ACTION_SEND")
            return true
        }

        if (DEBUG) {
            Log.d(TAG, "Google app doesn't support ACTION_SEND for images")
        }
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error sending to Google app: ${e.message}")
        if (DEBUG) {
            e.printStackTrace()
        }
        false
    }
}

/**
 * Tertiary Approach: Send the image to Google Photos
 */
private fun trySendToGooglePhotos(context: Context, contentUri: Uri): Boolean {
    if (DEBUG) {
        Log.d(TAG, "Attempting to send to Google Photos...")
    }

    val (installed, packageName) = isPackageInstalled(context, GOOGLE_PHOTOS_PACKAGE, ALTERNATIVE_PHOTOS_PACKAGES)
    if (!installed) {
        Log.d(TAG, "No Google Photos app found, skipping tertiary approach")
        return false
    }

    if (DEBUG) {
        Log.d(TAG, "Using Google Photos package: $packageName")
    }

    return try {
        // Try multiple approaches with Google Photos

        // Approach 1: ACTION_SEND with lens hint
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            setPackage(packageName)
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra("lens", true) // Hint to open in Lens mode
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (sendIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(sendIntent)
            Log.d(TAG, "Successfully launched Google Photos with ACTION_SEND")
            return true
        }

        if (DEBUG) {
            Log.d(TAG, "Google Photos doesn't support ACTION_SEND for images, trying VIEW")
        }

        // Approach 2: ACTION_VIEW with lens hint
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setPackage(packageName)
            setDataAndType(contentUri, "image/*")
            putExtra("lens", true) // Hint to open in Lens mode
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (viewIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(viewIntent)
            Log.d(TAG, "Successfully launched Google Photos with ACTION_VIEW")
            return true
        }

        Log.d(TAG, "Google Photos doesn't support any of our approaches")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error sending to Google Photos: ${e.message}")
        if (DEBUG) {
            e.printStackTrace()
        }
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
    if (DEBUG) {
        Log.d(TAG, "Attempting to use ACTION_VIEW with Google app...")
    }

    val (installed, packageName) = isPackageInstalled(context, GOOGLE_PACKAGE, ALTERNATIVE_GOOGLE_PACKAGES)
    if (!installed) {
        Log.d(TAG, "No Google app found, skipping quinary approach")
        return false
    }

    if (DEBUG) {
        Log.d(TAG, "Using Google package: $packageName for ACTION_VIEW")
    }

    // Try multiple known intents for Google Lens in the Google app
    val intents = listOf(
        // Method 1: Using specific action for Lens
        Intent("com.google.android.apps.lens.SEARCH_BY_IMAGE").apply {
            setPackage(packageName)
            setDataAndType(contentUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },

        // Method 2: Using ACTION_VIEW with lens parameter
        Intent(Intent.ACTION_VIEW).apply {
            setPackage(packageName)
            setDataAndType(contentUri, "image/*")
            putExtra("lens", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },

        // Method 3: Using ACTION_PROCESS_TEXT (sometimes works for images)
        Intent(Intent.ACTION_PROCESS_TEXT).apply {
            setPackage(packageName)
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra("lens", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )

    // Try each intent until one works
    for ((index, intent) in intents.withIndex()) {
        try {
            if (DEBUG) {
                Log.d(TAG, "Trying intent method ${index + 1}")
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Successfully launched Google app with intent method ${index + 1}")
                return true
            } else if (DEBUG) {
                Log.d(TAG, "Could not resolve intent method ${index + 1}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with Google app intent method ${index + 1}: ${e.message}")
            if (DEBUG) {
                e.printStackTrace()
            }
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
    if (DEBUG) {
        Log.d(TAG, "Attempting to open web version of Google Lens as last resort...")
    }

    return try {
        // Try multiple web URLs
        val lensUrls = listOf(
            "https://lens.google.com",
            "https://google.com/search?tbm=isch&q=search+by+image",
            "https://images.google.com"
        )

        for (url in lensUrls) {
            if (DEBUG) {
                Log.d(TAG, "Trying to open URL: $url")
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)

                Toast.makeText(
                    context,
                    "Opening Google Lens website as fallback",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d(TAG, "Successfully opened URL: $url")
                return true
            } else if (DEBUG) {
                Log.d(TAG, "Could not resolve URL: $url")
            }
        }

        Log.d(TAG, "No web browser available to open any Google Lens website")

        // Show error message as final fallback
        Toast.makeText(
            context,
            "Google Lens is not available on this device",
            Toast.LENGTH_SHORT
        ).show()

        false
    } catch (e: Exception) {
        // Show error message if even the web browser isn't available
        Toast.makeText(
            context,
            "Google Lens is not available on this device",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(TAG, "Error opening web browser: ${e.message}")
        if (DEBUG) {
            e.printStackTrace()
        }
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
    if (DEBUG) {
        Log.d(TAG, "Attempting direct Google Lens launch...")
    }

    val (installed, packageName) = isPackageInstalled(context, GOOGLE_PACKAGE, ALTERNATIVE_GOOGLE_PACKAGES)
    if (!installed) {
        Log.d(TAG, "No Google app found, skipping direct launch approach")
        return false
    }

    if (DEBUG) {
        Log.d(TAG, "Using Google package: $packageName for direct launch")
    }

    // Try multiple direct launch methods with the found package
    return tryComponentNameLaunch(context, contentUri, packageName!!) ||
           tryLensActionLaunch(context, contentUri, packageName) ||
           tryGoogleVisionLaunch(context, contentUri, packageName)
}

/**
 * Method 1: Using component name to directly target the Lens activity
 */
private fun tryComponentNameLaunch(context: Context, contentUri: Uri, packageName: String): Boolean {
    if (DEBUG) {
        Log.d(TAG, "Trying component name launch with package: $packageName")
    }

    return try {
        // Try multiple possible Lens activities
        val lensActivities = listOf(
            GOOGLE_LENS_ACTIVITY,
            "com.google.android.apps.lens.MainActivity",
            "com.google.android.gms.lens.MainActivity"
        )

        for (activity in lensActivities) {
            if (DEBUG) {
                Log.d(TAG, "Attempting to launch activity: $activity")
            }

            val intent = Intent().apply {
                component = android.content.ComponentName(packageName, activity)
                action = Intent.ACTION_VIEW
                setDataAndType(contentUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Successfully launched Google Lens directly with component name: $activity")
                return true
            } else if (DEBUG) {
                Log.d(TAG, "Could not resolve activity: $activity")
            }
        }

        Log.d(TAG, "Could not resolve any Google Lens activity with component name")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens with component name: ${e.message}")
        if (DEBUG) {
            e.printStackTrace()
        }
        false
    }
}

/**
 * Method 2: Using the specific Lens action
 */
private fun tryLensActionLaunch(context: Context, contentUri: Uri, packageName: String): Boolean {
    if (DEBUG) {
        Log.d(TAG, "Trying Lens action launch with package: $packageName")
    }

    return try {
        // Try multiple known Lens actions
        val lensActions = listOf(
            "com.google.android.apps.lens.SEARCH_BY_IMAGE",
            "android.intent.action.LENS_SEARCH_BY_IMAGE",
            "com.google.android.gms.vision.SEARCH"
        )

        for (action in lensActions) {
            if (DEBUG) {
                Log.d(TAG, "Attempting to use action: $action")
            }

            val intent = Intent(action).apply {
                setPackage(packageName)
                setDataAndType(contentUri, "image/*")
                putExtra(LENS_URI, contentUri.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Successfully launched Google Lens with action: $action")
                return true
            } else if (DEBUG) {
                Log.d(TAG, "Could not resolve action: $action")
            }
        }

        Log.d(TAG, "Could not resolve Google Lens with any specific action")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens with specific action: ${e.message}")
        if (DEBUG) {
            e.printStackTrace()
        }
        false
    }
}

/**
 * Method 3: Using Google Vision scan action
 */
private fun tryGoogleVisionLaunch(context: Context, contentUri: Uri, packageName: String): Boolean {
    if (DEBUG) {
        Log.d(TAG, "Trying Google Vision scan launch with package: $packageName")
    }

    return try {
        // Try multiple vision scan approaches
        val visionActions = listOf(
            LENS_ACTION,
            "com.google.android.gms.vision.SCAN",
            "com.google.android.gms.actions.SEARCH_BY_IMAGE"
        )

        for (action in visionActions) {
            if (DEBUG) {
                Log.d(TAG, "Attempting to use vision action: $action")
            }

            val intent = Intent(action).apply {
                setPackage(packageName)
                putExtra(LENS_URI, contentUri.toString())
                // Try both with and without data type
                if (action == LENS_ACTION) {
                    setDataAndType(contentUri, "image/*")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Successfully launched Google Lens with vision action: $action")
                return true
            } else if (DEBUG) {
                Log.d(TAG, "Could not resolve vision action: $action")
            }
        }

        // Try one more approach with ACTION_SEND
        if (DEBUG) {
            Log.d(TAG, "Trying ACTION_SEND with vision extras")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            setPackage(packageName)
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra("vision_mode", true)  // Hint to use vision/lens mode
            putExtra("lens", true)         // Another hint for lens
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (sendIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(sendIntent)
            Log.d(TAG, "Successfully launched Google Lens with ACTION_SEND and vision extras")
            return true
        }

        Log.d(TAG, "Could not resolve Google Lens with any vision approach")
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error launching Google Lens with vision approach: ${e.message}")
        if (DEBUG) {
            e.printStackTrace()
        }
        false
    }
}

/**
 * Check if a package is installed on the device
 *
 * @param context The context to use for checking
 * @param packageName The package name to check for
 * @param alternativePackages Optional list of alternative package names to check if the primary one is not found
 * @return A Pair containing a boolean indicating if any package was found and the actual package name that was found
 */
private fun isPackageInstalled(
    context: Context,
    packageName: String,
    alternativePackages: List<String> = emptyList()
): Pair<Boolean, String?> {
    // First try the primary package name
    try {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        if (DEBUG) {
            Log.d(TAG, "Found primary package: $packageName (${packageInfo.versionName})")
        }
        return Pair(true, packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        if (DEBUG) {
            Log.d(TAG, "Primary package not found: $packageName")
        }
    }

    // If primary package not found, try alternatives
    for (altPackage in alternativePackages) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(altPackage, 0)
            if (DEBUG) {
                Log.d(TAG, "Found alternative package: $altPackage (${packageInfo.versionName})")
            }
            return Pair(true, altPackage)
        } catch (e: PackageManager.NameNotFoundException) {
            if (DEBUG) {
                Log.d(TAG, "Alternative package not found: $altPackage")
            }
        }
    }

    // Try a different approach using resolveActivity for implicit intents
    try {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo != null) {
            val foundPackage = resolveInfo.activityInfo.packageName
            if (DEBUG) {
                Log.d(TAG, "Found package via intent resolution: $foundPackage")
            }
            return Pair(true, foundPackage)
        }
    } catch (e: Exception) {
        if (DEBUG) {
            Log.e(TAG, "Error checking via intent resolution: ${e.message}")
        }
    }

    // If we get here, no package was found
    if (DEBUG) {
        Log.d(TAG, "No suitable package found for $packageName or alternatives")

        // List all installed packages that might be related to Google
        try {
            val installedPackages = context.packageManager.getInstalledPackages(0)
            Log.d(TAG, "Listing all Google-related packages:")
            installedPackages
                .filter { it.packageName.contains("google", ignoreCase = true) }
                .forEach {
                    Log.d(TAG, "Found Google package: ${it.packageName} (${it.versionName})")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing installed packages: ${e.message}")
        }
    }

    return Pair(false, null)
}
