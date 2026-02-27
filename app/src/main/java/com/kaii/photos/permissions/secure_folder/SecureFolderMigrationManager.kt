package com.kaii.photos.permissions.secure_folder

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.SettingsAlbumsListImpl
import com.kaii.photos.helpers.AppDirectories
import com.kaii.photos.helpers.DataAndBackupHelper
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.copyImageListToPath
import com.kaii.photos.helpers.filename
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.moveMediaToSecureFolder
import com.kaii.photos.helpers.relativePath
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.mediastore.getUriFromAbsolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

private const val TAG = "com.kaii.photos.permissions.secure_folder.SecureFolderManager"

class SecureFolderMigrationManager(
    private val context: Context,
    private val appDatabase: MediaDatabase,
    private val albums: SettingsAlbumsListImpl
) {
    var uris = emptyList<Uri>()
        private set

    val needsMigrationFromOld: Boolean
        get() = context.getDir(AppDirectories.OldSecureFolder.path, Context.MODE_PRIVATE).listFiles()?.isNotEmpty() == true

    suspend fun needsMigrationFromUnencrypted() = withContext(Dispatchers.IO) {
        File(context.appSecureFolderDir)
            .listFiles()?.any {
                try {
                    val hasIV = appDatabase.securedItemEntityDao().getIvFromSecuredPath(it.absolutePath) != null
                    Log.e(TAG, "${it.name} has IV? $hasIV")
                    !hasIV // we want items that don't have an IV, that means they have not been encrypted yet
                } catch (e: Throwable) {
                    Log.e(TAG, "${it.name} has no IV, error: ${e.message}")
                    e.printStackTrace()
                    true
                }
            } == true
    }

    // TODO: move again to Android/data for space purposes
    suspend fun migrateFromOldDirectory() = withContext(Dispatchers.IO) {
        val oldDir = context.getDir(AppDirectories.OldSecureFolder.path, Context.MODE_PRIVATE)
        val oldFiles = oldDir.listFiles()

        if (oldFiles == null || oldFiles.isEmpty()) return@withContext

        Log.d(TAG, "Exporting backup of old secure folder items")

        val helper = DataAndBackupHelper(applicationDatabase = appDatabase)
        val success = helper.exportRawSecureFolderItems(
            context = context,
            secureFolder = oldDir
        )

        if (success) {
            val exportDir = helper.getRawExportDir(context = context)
            albums.add(
                listOf(
                    AlbumInfo(
                        id = exportDir.relativePath.hashCode(),
                        name = exportDir.relativePath.filename(),
                        paths = setOf(exportDir.relativePath)
                    )
                )
            )

            oldFiles.forEach {
                Log.d(TAG, "item in old dir ${it.name}")

                val destination = File(context.appSecureFolderDir, it.name)
                if (!destination.exists()) {
                    it.copyTo(destination)
                    it.delete()
                }
            }
        } else {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = context.resources.getString(R.string.secure_export_failed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Long
                )
            )
        }
    }

    suspend fun setupMigrationFromUnencrypted() = withContext(Dispatchers.IO) {
        val unencryptedFiles = File(context.appSecureFolderDir)
            .listFiles()?.filter {
                try {
                    val hasIV = appDatabase.securedItemEntityDao().getIvFromSecuredPath(it.absolutePath) != null
                    Log.e(TAG, "${it.name} has IV? $hasIV")
                    !hasIV // we want items that don't have an IV, that means they have not been encrypted yet
                } catch (e: Throwable) {
                    Log.e(TAG, "${it.name} has no IV, error: ${e.message}")
                    e.printStackTrace()
                    true
                }
            } ?: return@withContext

        val restoredFilesDir = context.appRestoredFilesDir

        uris = unencryptedFiles.mapNotNull { file ->
            val destination = File(restoredFilesDir, file.name)
            if (!destination.exists()) {
                file.copyTo(destination)
            }

            val uri = context.contentResolver.getUriFromAbsolutePath(
                absolutePath = destination.absolutePath,
                type =
                    if (Files.probeContentType(Path(destination.absolutePath)).startsWith("image")) MediaType.Image
                    else MediaType.Video
            )

            Log.d(TAG, "Uri for file ${file.name} is $uri")

            uri
        }
    }

    suspend fun migrateFromUnencrypted(
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val mediaItems = uris.mapNotNull { uri ->
            context.contentResolver.getMediaStoreDataFromUri(uri = uri)
        }

        Log.d(TAG, "Creating a backup of the secure folder media...")
        copyImageListToPath(
            list = mediaItems.fastMap {
                SelectionManager.SelectedItem(
                    id = it.id,
                    isImage = it.type == MediaType.Image,
                    parentPath = it.parentPath
                )
            },
            context = context,
            destination = context.appRestoredFilesDir,
            overwriteDate = true,
            overrideDisplayName = { displayName ->
                val extension = displayName.replaceBeforeLast(".", "")

                val name = displayName.replace(extension, ".backup")
                Log.d(TAG, "Final name of file is $name")
                name
            },
            onSingleItemDone = { _ -> }
        )

        val path = context.appRestoredFilesDir.toRelativePath()
        albums.add(
            listOf(
                AlbumInfo(
                    id = path.hashCode(),
                    name = path.filename(),
                    paths = setOf(path)
                )
            )
        )

        Log.d(TAG, "Encrypting secure folder media...")
        moveMediaToSecureFolder(
            list = mediaItems,
            context = context,
            applicationDatabase = appDatabase,
            onDone = {
                uris = emptyList()
                onDone()
            }
        )
    }
}

@Composable
fun rememberSecureFolderManager(): SecureFolderMigrationManager {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    return remember(mainViewModel, context) {
        SecureFolderMigrationManager(
            context = context,
            appDatabase = MediaDatabase.getInstance(context = context),
            albums = mainViewModel.settings.albums
        )
    }
}