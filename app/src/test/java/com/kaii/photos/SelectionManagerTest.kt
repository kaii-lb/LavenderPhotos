package com.kaii.photos

import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.mediastore.MediaType
import io.github.kaii_lb.lavender.immichintegration.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class SelectionManagerTest {
    @Test
    fun selectionCommitsWhenClearDoesNotIntervene() = runBlocking {
        val lookup = DelayedLookup()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = selectionManager(scope, lookup)
        val item = media(id = 1)

        manager.addMedia(item)
        lookup.awaitRequest()
        lookup.complete(mapOf(item.id to item.toSelectedItem()))
        scope.awaitChildren()

        assertEquals(listOf(item.id), manager.selection.first().map { it.id })
        assertTrue(manager.enabled.first())
    }

    @Test
    fun clearRejectsStaleSelectionAndAllowsNewItemAtSamePosition() = runBlocking {
        val lookup = DelayedLookup()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = selectionManager(scope, lookup)
        val deletedItem = media(id = 1)
        val newItem = media(id = 2)

        manager.addMedia(deletedItem)
        lookup.awaitRequest()
        manager.clear()
        lookup.complete(mapOf(deletedItem.id to deletedItem.toSelectedItem()))
        scope.awaitChildren()

        assertTrue(manager.selection.first().isEmpty())
        assertFalse(manager.enabled.first())

        manager.addMedia(newItem)
        lookup.awaitRequest()
        lookup.complete(mapOf(newItem.id to newItem.toSelectedItem()))
        scope.awaitChildren()

        assertEquals(listOf(newItem.id), manager.selection.first().map { it.id })
        assertTrue(manager.enabled.first())
    }

    @Test
    fun clearRejectsStaleSectionSelection() = runBlocking {
        val lookup = DelayedLookup()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = selectionManager(scope, lookup)
        val item = media(id = 1)
        val section = PhotoLibraryUIModel.Section(
            title = "Today",
            timestamp = item.getDateTakenDay(TimeZone.getDefault())
        )

        manager.toggle(section)
        lookup.awaitRequest()
        manager.clear()
        lookup.complete(mapOf(item.id to item.toSelectedItem()))
        scope.awaitChildren()

        assertTrue(manager.selection.first().isEmpty())
        assertFalse(manager.isSelected(section))
        assertFalse(manager.enabled.first())
    }

    @Test
    fun clearRejectsStaleBulkSelection() = runBlocking {
        val lookup = DelayedLookup()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = selectionManager(scope, lookup)
        val item = media(id = 1)

        val selectionJob = manager.updateSelection(added = listOf(item), removed = emptyList())
        lookup.awaitRequest()
        manager.clear()
        lookup.complete(mapOf(item.id to item.toSelectedItem()))
        selectionJob.join()

        assertTrue(manager.selection.first().isEmpty())
        assertFalse(manager.enabled.first())
    }

    @Test
    fun clearRejectsStaleRangeSelection() = runBlocking {
        val lookup = DelayedLookup()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = selectionManager(scope, lookup)
        val item = media(id = 1)

        val selectionJob = manager.addAll(items = listOf(item.toUiModel()))
        lookup.awaitRequest()
        manager.clear()
        lookup.complete(mapOf(item.id to item.toSelectedItem()))
        selectionJob.join()

        assertTrue(manager.selection.first().isEmpty())
        assertFalse(manager.enabled.first())
    }

    private fun selectionManager(scope: CoroutineScope, lookup: DelayedLookup) = SelectionManager(
        sortMode = MediaItemSortMode.DateTaken,
        scope = scope,
        getMediaInDate = { _, _ -> lookup.getMedia() }
    )

    private suspend fun CoroutineScope.awaitChildren() {
        coroutineContext[Job]?.children?.toList()?.joinAll()
    }

    private fun media(id: Long) = MediaStoreData(
        id = id,
        uri = "content://media/$id",
        absolutePath = "/storage/emulated/0/Pictures/$id.jpg",
        parentPath = "/storage/emulated/0/Pictures",
        displayName = "$id.jpg",
        dateTaken = 1_700_000_000,
        dateModified = 1_700_000_000,
        mimeType = "image/jpeg",
        type = MediaType.Image,
        immichUrl = null,
        hash = null,
        size = 1,
        favourited = false,
        duration = null
    )

    private fun MediaStoreData.toSelectedItem() = SelectionManager.SelectedItem(
        id = id,
        uri = uri,
        immichUrl = immichUrl,
        isImage = type == MediaType.Image,
        absolutePath = absolutePath,
        parentPath = parentPath
    )

    private fun MediaStoreData.toUiModel() = object : PhotoLibraryUIModel.MediaImpl {
        override val item = this@toUiModel
        override val auth: Auth
            get() = error("Not used by SelectionManager")
        override val endpoint: String? = null
    }

    private class DelayedLookup {
        private val requests = Channel<Unit>(Channel.UNLIMITED)
        private val responses = Channel<Map<Long, SelectionManager.SelectedItem>>(Channel.UNLIMITED)

        suspend fun getMedia(): Map<Long, SelectionManager.SelectedItem> {
            requests.send(Unit)
            return responses.receive()
        }

        suspend fun awaitRequest() = requests.receive()

        suspend fun complete(media: Map<Long, SelectionManager.SelectedItem>) {
            responses.send(media)
        }
    }
}
