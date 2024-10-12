package com.kaii.photos.helpers

import android.content.Context
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberVibratorManager() : VibratorManager {
    val vibrator = LocalContext.current.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    return remember {
        vibrator
    }
}

fun VibratorManager.vibrateShort() {
    this.defaultVibrator.vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    )
}

fun VibratorManager.vibrateLong() {
    this.defaultVibrator.vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    )
}

