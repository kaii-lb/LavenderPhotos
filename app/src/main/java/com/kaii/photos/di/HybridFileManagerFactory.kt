package com.kaii.photos.di

import com.kaii.photos.file_management.managers.impl.HybridFileManager
import com.kaii.photos.file_management.managers.impl.LocalSourceFileManager
import dagger.assisted.AssistedFactory

@AssistedFactory
interface HybridFileManagerFactory {
    fun create(other: LocalSourceFileManager): HybridFileManager
}