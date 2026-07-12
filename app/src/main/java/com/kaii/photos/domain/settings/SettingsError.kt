package com.kaii.photos.domain.settings

import com.kaii.photos.domain.Error

enum class SettingsError : Error {
    IOError,
    LoadError
}