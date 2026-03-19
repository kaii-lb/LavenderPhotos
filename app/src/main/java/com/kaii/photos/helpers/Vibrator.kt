package com.kaii.photos.helpers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
        VibrationEffect.createOneShot(30, 1)
    )
}

fun Vibrator.vibrateLong() {
    vibrate(
        VibrationEffect.createOneShot(60, 10)
    )
}

