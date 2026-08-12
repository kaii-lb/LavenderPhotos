package com.kaii.photos.domain.authentication

import kotlinx.coroutines.flow.Flow

interface BiometricPromptManager {
    val events: Flow<PromptAuthResult>

    fun authenticate()
}