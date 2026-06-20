package com.kaii.photos.helpers

import android.os.Bundle
import androidx.navigation.NavType
import kotlin.io.encoding.Base64

class NullableByteArrayNavType : NavType<ByteArray?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): ByteArray? {
        return bundle.getByteArray(key)
    }

    override fun parseValue(value: String): ByteArray? {
        if (value == "null") return null
        return Base64.UrlSafe.decode(value)
    }

    override fun put(bundle: Bundle, key: String, value: ByteArray?) {
        bundle.putByteArray(key, value)
    }

    override fun serializeAsValue(value: ByteArray?): String {
        if (value == null) return "null"
        return Base64.UrlSafe.encode(value)
    }
}