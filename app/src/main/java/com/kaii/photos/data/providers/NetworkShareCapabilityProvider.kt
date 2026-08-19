package com.kaii.photos.data.providers

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract

object NetworkShareCapabilityProvider {
    private val BUILT_IN_AUTHORITIES = setOf(
        "com.android.externalstorage.documents",
        "com.android.providers.downloads.documents",
        "com.android.providers.media.documents",
        "com.android.mtp.documents",
        "com.android.shell.documents",
        "com.android.traceur.documents"
    )

    var  hasNetworkCapabilities = false

    fun init(context: Context) {
        val intent = Intent(DocumentsContract.PROVIDER_INTERFACE)

        hasNetworkCapabilities = context.packageManager
            .queryIntentContentProviders(intent, 0)
            .any { it.providerInfo.authority !in BUILT_IN_AUTHORITIES }
    }
}