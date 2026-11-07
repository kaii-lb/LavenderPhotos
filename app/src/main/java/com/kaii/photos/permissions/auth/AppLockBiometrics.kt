package com.kaii.photos.permissions.auth

import android.content.Context
import android.hardware.biometrics.BiometricManager

fun hasStrongBiometrics(context: Context): Boolean =
    context.getSystemService(BiometricManager::class.java)?.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    ) == BiometricManager.BIOMETRIC_SUCCESS

/** The PIN is always retained as a recovery option for app-lock biometric failures. */
fun canUnlockAppWithBiometrics(
    hasPin: Boolean,
    optedIn: Boolean,
    hasBiometrics: Boolean
): Boolean = hasPin && optedIn && hasBiometrics
