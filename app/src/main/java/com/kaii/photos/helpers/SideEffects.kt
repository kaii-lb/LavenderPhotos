package com.kaii.photos.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.savedstate.SavedState
import com.kaii.photos.LocalNavController

@Composable
fun OnBackPressedEffect(
    onDestinationChange: (destination: NavDestination) -> Unit
) {
    val navController = LocalNavController.current
    DisposableEffect(Unit) {
        val listener = object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: SavedState?
            ) {
                onDestinationChange(destination)
            }
        }
        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}