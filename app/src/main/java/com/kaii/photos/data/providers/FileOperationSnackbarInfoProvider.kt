package com.kaii.photos.data.providers

import android.content.res.Resources
import com.kaii.photos.R
import com.kaii.photos.domain.files.FileOperationAction

class FileOperationSnackbarInfoProvider(
    private val resources: Resources
) {
    fun getBodyFor(
        action: FileOperationAction.LongOperationType,
        current: Int,
        total: Int
    ): String = when (action) {
        FileOperationAction.LongOperationType.Copy -> {
            resources.getString(R.string.media_copy_snackbar_body, current, total)
        }

        FileOperationAction.LongOperationType.Move -> {
            resources.getString(R.string.media_move_snackbar_body, current, total)
        }

        FileOperationAction.LongOperationType.Trash -> {
            resources.getString(R.string.media_delete_snackbar_body, current, total)
        }

        FileOperationAction.LongOperationType.Delete -> {
            resources.getString(R.string.media_delete_snackbar_body, current, total)
        }

        FileOperationAction.LongOperationType.Share -> {
            resources.getString(R.string.media_share_snackbar_body, current, total)
        }

        FileOperationAction.LongOperationType.Secure -> {
            resources.getString(R.string.media_secure_snackbar_body, current, total)
        }

        FileOperationAction.LongOperationType.Restore -> {
            resources.getString(R.string.media_restore_snackbar_body, current, total)
        }
    }

    fun getMessageFor(
        action: FileOperationAction.LongOperationType,
    ): String = when (action) {
        FileOperationAction.LongOperationType.Copy -> {
            resources.getString(R.string.media_copy_snackbar_title)
        }

        FileOperationAction.LongOperationType.Move -> {
            resources.getString(R.string.media_move_snackbar_title)
        }

        FileOperationAction.LongOperationType.Trash -> {
            resources.getString(R.string.media_delete_snackbar_title)
        }

        FileOperationAction.LongOperationType.Delete -> {
            resources.getString(R.string.media_delete_snackbar_title)
        }

        FileOperationAction.LongOperationType.Share -> {
            resources.getString(R.string.media_share_snackbar_title)
        }

        FileOperationAction.LongOperationType.Secure -> {
            resources.getString(R.string.media_secure_snackbar_title)
        }

        FileOperationAction.LongOperationType.Restore -> {
            resources.getString(R.string.media_restore_snackbar_title)
        }
    }

    fun getIconFor(
        action: FileOperationAction.LongOperationType
    ): Int = when (action) {
        FileOperationAction.LongOperationType.Copy -> {
            R.drawable.copy
        }

        FileOperationAction.LongOperationType.Move -> {
            R.drawable.cut
        }

        FileOperationAction.LongOperationType.Trash -> {
            R.drawable.delete
        }

        FileOperationAction.LongOperationType.Delete -> {
            R.drawable.delete_forever
        }

        FileOperationAction.LongOperationType.Share -> {
            R.drawable.share
        }

        FileOperationAction.LongOperationType.Secure -> {
            R.drawable.secure_folder
        }

        FileOperationAction.LongOperationType.Restore -> {
            R.drawable.unlock
        }
    }
}