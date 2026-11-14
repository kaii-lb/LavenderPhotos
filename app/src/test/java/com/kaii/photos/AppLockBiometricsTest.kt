package com.kaii.photos

import com.kaii.photos.permissions.auth.canUnlockAppWithBiometrics
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockBiometricsTest {
    @Test
    fun enablesBiometricsOnlyWithPinConsentAndEnrollment() {
        assertTrue(canUnlockAppWithBiometrics(hasPin = true, optedIn = true, hasBiometrics = true))

        assertFalse(canUnlockAppWithBiometrics(hasPin = false, optedIn = true, hasBiometrics = true))
        assertFalse(canUnlockAppWithBiometrics(hasPin = true, optedIn = false, hasBiometrics = true))
        assertFalse(canUnlockAppWithBiometrics(hasPin = true, optedIn = true, hasBiometrics = false))
        assertFalse(canUnlockAppWithBiometrics(hasPin = false, optedIn = false, hasBiometrics = false))
    }
}
