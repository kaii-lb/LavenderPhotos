package com.kaii.photos.helpers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.toActivity(): Activity? {
    var context = this
    while (context is ContextWrapper && context !is Activity) {
        context = context.baseContext
    }

    return context as? Activity
}