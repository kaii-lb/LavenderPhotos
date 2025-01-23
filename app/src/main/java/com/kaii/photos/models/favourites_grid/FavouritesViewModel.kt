package com.kaii.photos.models.favourites_grid

import android.net.Uri
import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.MainActivity
import com.kaii.photos.database.entities.FavouritedItemEntity
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class FavouritesViewModel() : ViewModel() {
    private val dao = MainActivity.applicationDatabase.favouritedItemEntityDao()

    val mediaFlow by lazy {
        getMediaDataFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    private fun getMediaDataFlow(): Flow<List<MediaStoreData>> {
        val list = dao.getAll().flowOn(Dispatchers.IO).map { list ->
            list.map { entity ->
                MediaStoreData(
                    displayName = entity.displayName,
                    dateTaken = entity.dateTaken,
                    absolutePath = entity.absolutePath,
                    mimeType = entity.mimeType,
                    uri = Uri.parse(entity.uri),
                    type = entity.type,
                    id = entity.id,
                    dateModified = entity.dateModified,
                )
            }
        }

        return list
    }

    fun addToFavourites(mediaItem: MediaStoreData) {
        val dateModified = System.currentTimeMillis() / 1000
        viewModelScope.launch {
            dao.insertEntity(
                FavouritedItemEntity(
                    id = mediaItem.id,
                    dateTaken = mediaItem.dateTaken,
                    dateModified = dateModified,
                    mimeType = mediaItem.mimeType ?: "image/*", // TODO: context.contentResolver.getType(mediaItem.uri),
                    type = mediaItem.type,
                    absolutePath = mediaItem.absolutePath,
                    displayName = mediaItem.displayName ?: "Media",
                    uri = mediaItem.uri.toString()
                )
            )
        }
    }

    fun removeFromFavourites(mediaItemId: Long) {
        viewModelScope.launch {
            dao.deleteEntityById(mediaItemId)
        }
    }

    fun isInFavourites(mediaItemId: Long): StateFlow<Boolean> {
        val isInDB = MutableStateFlow(false)

        viewModelScope.launch {
            dao.isInDB(mediaItemId).collect {
                isInDB.value = it
            }
        }

        return isInDB
    }
}
