package com.kaii.photos.presentation.selection

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R

enum class SelectionEvent(
    @param:StringRes val message: Int,
    @param:DrawableRes val icon: Int
) {
    LimitReached(
        message = R.string.selection_limit_reached,
        icon = R.drawable.error_2
    )
}