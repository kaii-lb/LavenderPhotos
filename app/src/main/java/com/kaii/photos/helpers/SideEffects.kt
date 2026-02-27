package com.kaii.photos.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.kaii.photos.LocalNavController

@Composable
fun OnBackPressedEffect(
    onDestinationChange: (destination: NavDestination) -> Unit
) {
    val navController = LocalNavController.current
    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ -> onDestinationChange(destination) }
        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}