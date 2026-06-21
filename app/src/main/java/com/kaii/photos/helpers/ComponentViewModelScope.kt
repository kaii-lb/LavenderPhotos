package com.kaii.photos.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner

@Composable
fun ComponentViewModelScope(
    key: Any,
    saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder(),
    content: @Composable () -> Unit
) {
    saveableStateHolder.SaveableStateProvider(key) {
        val storeOwner = rememberViewModelStoreOwner()

        CompositionLocalProvider(
            value = LocalViewModelStoreOwner provides storeOwner,
            content = content
        )
    }
}