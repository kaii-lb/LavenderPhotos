package com.kaii.photos.permissions.favourites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kaii.photos.database.entities.MediaStoreData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CloudFavouritesState(
    initialFavourite: Boolean,
    private val setFavourite: suspend (favourite: Boolean) -> Unit
) : GenericFavouritesState {
    private var nextState = false

    private val _state = MutableStateFlow(initialFavourite)
    override val state = _state.asStateFlow()

    override suspend fun favourite(
        favourite: Boolean
    ) = withContext(Dispatchers.IO) {
        nextState = favourite

        setFavourite(favourite)
    }
}

@Composable
fun rememberCloudFavouritesState(
    media: MediaStoreData,
    setFavourite: suspend (favourite: Boolean) -> Unit
): CloudFavouritesState = remember {
    CloudFavouritesState(
        initialFavourite = media.favourited,
        setFavourite = setFavourite
    )
}