package com.aks_labs.tulsi.helpers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Suppress("DEPRECATION")
@Composable
fun rememberVibratorManager() : Vibrator {
	val context = LocalContext.current

	return remember {
	   	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
	   	else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
	}
}

fun Vibrator.vibrateShort() {
    vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    )
}

fun Vibrator.vibrateLong() {
    vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    )
}



