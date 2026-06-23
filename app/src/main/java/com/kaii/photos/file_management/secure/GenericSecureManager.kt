package com.kaii.photos.file_management.secure

import android.content.Context
import com.kaii.photos.helpers.grid_management.SelectionManager

interface GenericSecureManager {
    /** returns files to be permanently deleted */
    suspend fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): List<SelectionManager.SelectedItem>

    /** return success state of the operation */
    suspend fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ): Boolean
}