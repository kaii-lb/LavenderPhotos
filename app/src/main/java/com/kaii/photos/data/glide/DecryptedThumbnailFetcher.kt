package com.kaii.photos.data.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.mediastore.SecureInfo
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class DecryptedThumbnailFetcher(
    private val item: SecureInfo
) : DataFetcher<InputStream> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            // all-zero iv means the thumbnail isn't ready yet; decrypting with it gives garbage, so
            // fail cleanly and let glide show the placeholder + retry instead of a half-decoded image
            if (item.iv.isEmpty() || item.iv.all { it.toInt() == 0 }) {
                callback.onLoadFailed(IllegalStateException("secure thumbnail IV not ready for ${item.absolutePath}"))
                return
            }

            val encryptedBytes = File(item.absolutePath).readBytes()

            val decryptedBytes = EncryptionManager.decryptBytes(
                bytes = encryptedBytes,
                iv = item.iv
            )

            // empty decrypt means corrupt/missing data; fail rather than hand glide an empty stream
            if (decryptedBytes.isEmpty()) {
                callback.onLoadFailed(IllegalStateException("secure decrypt produced no data for ${item.absolutePath}"))
                return
            }

            val inputStream = ByteArrayInputStream(decryptedBytes)

            callback.onDataReady(inputStream)
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {}

    override fun cancel() {}

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.LOCAL
}