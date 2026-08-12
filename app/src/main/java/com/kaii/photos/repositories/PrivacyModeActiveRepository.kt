package com.kaii.photos.repositories

import com.kaii.photos.datastore.preferences.SettingsPermissionsImpl
import com.kaii.photos.domain.authentication.PromptAuthAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class PrivacyModeActiveRepository @Inject constructor(
    private val settings: SettingsPermissionsImpl
) {
    private val _events = Channel<PromptAuthAction>()
    val events = _events.receiveAsFlow()

    private suspend fun useBiometricPrompt(): Boolean = settings.getPassword().first() == null

    suspend fun unlock() {
        if (useBiometricPrompt()) {
            _events.send(PromptAuthAction.UseBiometrics)
        } else {
            _events.send(PromptAuthAction.UsePassword)
        }
    }

    fun markUnlocked() {
        settings.setPrivacyModeActive(active = false)
    }
}