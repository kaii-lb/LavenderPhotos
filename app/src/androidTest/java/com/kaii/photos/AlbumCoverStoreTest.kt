package com.kaii.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kaii.photos.datastore.AlbumCoverStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AlbumCoverStoreTest {
    private lateinit var context: Context
    private lateinit var store: AlbumCoverStore
    private val firstAlbum = "cover-test-${UUID.randomUUID()}"
    private val secondAlbum = "cover-test-${UUID.randomUUID()}"
    private lateinit var bitmap: Bitmap

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = AlbumCoverStore(context)
        bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
    }

    @After
    fun cleanup() = runBlocking {
        store.remove(setOf(firstAlbum, secondAlbum))
        bitmap.recycle()
    }

    @Test
    fun savedCopyIsReadableFromANewStoreInstance() = runBlocking {
        store.save(firstAlbum, bitmap)
        val filename = AlbumCoverStore(context).get().first()[firstAlbum]
        assertNotNull(filename)
        val saved = file(filename!!)
        assertTrue(saved.isFile)
        val decoded = BitmapFactory.decodeFile(saved.absolutePath)
        assertNotNull(decoded)
        assertEquals(64, decoded.width)
        assertEquals(64, decoded.height)
        decoded.recycle()
    }

    @Test
    fun replacingCoverUsesANewCacheKeyAndDeletesPreviousCopy() = runBlocking {
        store.save(firstAlbum, bitmap)
        val old = store.get().first().getValue(firstAlbum)
        bitmap.eraseColor(Color.BLUE)
        store.save(firstAlbum, bitmap)
        val replacement = store.get().first().getValue(firstAlbum)
        assertFalse(old == replacement)
        assertFalse(file(old).exists())
        assertTrue(file(replacement).isFile)
    }

    @Test
    fun resetDeletesOnlyTheChosenAlbumsCover() = runBlocking {
        store.save(firstAlbum, bitmap)
        store.save(secondAlbum, bitmap)
        val first = store.get().first().getValue(firstAlbum)
        val second = store.get().first().getValue(secondAlbum)
        store.remove(setOf(firstAlbum))
        val remaining = store.get().first()
        assertFalse(firstAlbum in remaining)
        assertEquals(second, remaining[secondAlbum])
        assertFalse(file(first).exists())
        assertTrue(file(second).isFile)
    }

    @Test
    fun resetIsSafeWhenThereIsNoCustomCover() = runBlocking {
        store.remove(setOf(firstAlbum))
        store.remove(setOf(firstAlbum))
        assertFalse(firstAlbum in store.get().first())
    }

    private fun file(filename: String) = File(File(context.filesDir, AlbumCoverStore.DIRECTORY), filename)
}
