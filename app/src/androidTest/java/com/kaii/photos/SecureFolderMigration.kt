package com.kaii.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.SettingsAlbumsListImpl
import com.kaii.photos.helpers.AppDirectories
import com.kaii.photos.permissions.secure_folder.SecureFolderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(AndroidJUnit4::class)
class SecureFolderMigration {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val secureFolderManager = SecureFolderManager(
        context = context,
        appDatabase = MediaDatabase.getInstance(context),
        albums = SettingsAlbumsListImpl(
            context = context,
            viewModelScope = CoroutineScope(EmptyCoroutineContext)
        )
    )

    @Test
    fun testOldToNew() {
        val oldDir = context.getDir(AppDirectories.OldSecureFolder.path, Context.MODE_PRIVATE)
        if (!oldDir.exists()) oldDir.mkdirs()

        val outFile = File(oldDir, "catpic.png")

        if (outFile.parentFile?.exists() != true) outFile.mkdirs()

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cat_picture)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outFile.outputStream())

        Assert.assertEquals(secureFolderManager.needsMigrationFromOld, true)

        runBlocking {
            secureFolderManager.migrateFromOldDirectory()
        }

        Assert.assertEquals(secureFolderManager.needsMigrationFromOld, false)
    }

    @Test
    suspend fun testUnencrypted() {
        val dir = context.getDir(AppDirectories.SecureFolder.path, Context.MODE_PRIVATE)
        val file = File(dir, "catpic.png")

        if (file.exists()) file.delete()
        file.createNewFile()

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cat_picture)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())

        Assert.assertEquals(secureFolderManager.needsMigrationFromUnencrypted(), true)

        secureFolderManager.setupMigrationFromUnencrypted()
        secureFolderManager.migrateFromUnencrypted {}

        Assert.assertEquals(secureFolderManager.needsMigrationFromUnencrypted(), false)
    }
}