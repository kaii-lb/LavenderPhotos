package com.kaii.photos.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import com.kaii.photos.LocalNavController
import com.kaii.photos.helpers.Screens

@Composable
inline fun <reified VM : ViewModel> NavBackStackEntry.sharedViewModel(
    screenScope: Screens? = null
): VM {
    val navController = LocalNavController.current
    val navGraphRoute = destination.parent?.route
    val parentEntry = remember(this) {
        when {
            screenScope != null -> navController.getBackStackEntry(screenScope)
            navGraphRoute != null -> navController.getBackStackEntry(navGraphRoute)
            else -> null
        }
    }

    return if (parentEntry != null) hiltViewModel(viewModelStoreOwner = parentEntry)
    else hiltViewModel()
}

@Composable
inline fun <reified VM : ViewModel, reified VMF> NavBackStackEntry.sharedViewModel(
    screenScope: Screens? = null,
    noinline creationCallback: (factory: VMF) -> VM
): VM {
    val navController = LocalNavController.current
    val navGraphRoute = destination.parent?.route
    val parentEntry = remember(this) {
        when {
            screenScope != null -> navController.getBackStackEntry(screenScope)
            navGraphRoute != null -> navController.getBackStackEntry(navGraphRoute)
            else -> null
        }
    }

    return if (parentEntry != null) hiltViewModel(viewModelStoreOwner = parentEntry, creationCallback = creationCallback)
    else hiltViewModel(creationCallback = creationCallback)
}