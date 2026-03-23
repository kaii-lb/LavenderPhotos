package com.kaii.photos.permissions.favourites

import kotlinx.coroutines.flow.StateFlow

interface GenericFavouritesState {
    val state: StateFlow<Boolean>

    suspend fun favourite(favourite: Boolean)
}