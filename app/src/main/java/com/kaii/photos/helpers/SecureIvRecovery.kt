package com.kaii.photos.helpers

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.entities.SecuredItemEntity
import java.io.File

/**
 * Recovers AES-CBC IVs lost to the failed-secure catch bug (db stored ByteArray(0)).
 *
 * In CBC the IV only affects the first 16-byte plaintext block, so a zero-IV decrypt is byte-perfect
 * from offset 16 on and only wrong in bytes 0..15 — the file itself is intact. The original IV is
 * then `D_K(C0) xor P0`, where D_K(C0) is the zero-IV first block and P0 the true first 16 plaintext
 * bytes. We try candidate P0 values (format constants + the real first block of good siblings) and
 * validate each by actually decoding the reconstructed file, so a wrong guess is rejected rather than
 * persisted. Worst case is "no recovery", never corruption.
 *
 * Extend by adding a [Kind] and its constants in [constantsFor].
 */
object SecureIvRecovery {
    private const val TAG = "com.kaii.photos.helpers.SecureIvRecovery"

    /** Cap on good siblings sampled as P0 candidates, to bound cost on large folders. */
    private const val MAX_SIBLING_CANDIDATES = 24

    /** [recover] then persist the result so future operations skip recovery. Off-main-thread only. */
    suspend fun recoverAndPersist(
        context: Context,
        securedFile: File,
        mimeType: String?,
        dao: SecuredMediaItemEntityDao
    ): ByteArray? {
        val iv = recover(context, securedFile, mimeType, dao) ?: return null
        dao.insertEntity(
            SecuredItemEntity(
                originalPath = dao.getOriginalPathFromSecuredPath(securedFile.absolutePath)
                    ?: securedFile.absolutePath,
                securedPath = securedFile.absolutePath,
                iv = iv
            )
        )
        return iv
    }

    /**
     * Recover a validated 16-byte IV for [securedFile], or null if the format isn't recoverable or
     * no candidate produced a decodable file. Does file IO + crypto + decode; off-main-thread only.
     */
    fun recover(
        context: Context,
        securedFile: File,
        mimeType: String?,
        dao: SecuredMediaItemEntityDao
    ): ByteArray? {
        val encryptedBytes = try {
            securedFile.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "recover: failed to read ${securedFile.name}", e)
            return null
        }
        if (encryptedBytes.size < 32) return null

        val kind = mimeType?.lowercase()?.let {
            when {
                it.contains("png") -> Kind.PNG
                it.contains("jpeg") || it.contains("jpg") -> Kind.JPEG
                it.contains("webm") || it.contains("video") -> Kind.VIDEO
                it.contains("image") -> Kind.IMAGE_OTHER
                else -> null
            }
        } ?: return null

        // zero-IV decrypt: correct everywhere except bytes 0..15
        val zeroDecrypted = try {
            EncryptionManager.decryptBytes(encryptedBytes, ByteArray(16))
        } catch (e: Exception) {
            Log.e(TAG, "recover: zero-iv decrypt failed for ${securedFile.name}", e)
            return null
        }
        if (zeroDecrypted.size < 16) return null
        val zeroFirstBlock = zeroDecrypted.copyOfRange(0, 16) // == D_K(C0)

        // constants first, then siblings; de-duplicated to avoid re-validating the same guess
        val candidates = LinkedHashSet<List<Byte>>()
        constantsFor(kind).forEach { candidates.add(it.toList()) }
        siblingFirstBlocks(securedFile, dao).forEach { candidates.add(it.toList()) }

        for (candidate in candidates) {
            val p0 = candidate.toByteArray()
            val reconstructed = zeroDecrypted.copyOf().also { System.arraycopy(p0, 0, it, 0, 16) }

            if (!validates(context, reconstructed, kind)) continue

            val iv = ByteArray(16) { i -> (zeroFirstBlock[i].toInt() xor p0[i].toInt()).toByte() }
            if (iv.all { it.toInt() == 0 }) continue // keystore rejects all-zero ivs; also a bad guess

            Log.d(TAG, "recover: validated IV for ${securedFile.name} ($kind)")
            return iv
        }

        Log.e(TAG, "recover: no candidate validated for ${securedFile.name} ($kind)")
        return null
    }

    private enum class Kind { PNG, JPEG, VIDEO, IMAGE_OTHER }

    /** Decode-validate the reconstructed plaintext (cheap bounds/metadata check, no full render). */
    private fun validates(context: Context, plaintext: ByteArray, kind: Kind): Boolean = when (kind) {
        Kind.VIDEO -> validatesVideo(context, plaintext)
        else -> validatesImage(plaintext)
    }

    private fun validatesImage(plaintext: ByteArray): Boolean {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return try {
            BitmapFactory.decodeByteArray(plaintext, 0, plaintext.size, opts)
            opts.outWidth > 0 && opts.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun validatesVideo(context: Context, plaintext: ByteArray): Boolean {
        val temp = File.createTempFile("iv_recovery_", ".tmp", context.cacheDir)
        val retriever = MediaMetadataRetriever()
        return try {
            temp.writeBytes(plaintext)
            retriever.setDataSource(temp.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            hasVideo == "yes" || (width?.toIntOrNull() ?: 0) > 0
        } catch (e: Exception) {
            false
        } finally {
            try { retriever.release() } catch (_: Exception) {}
            temp.delete()
        }
    }

    /** Real first 16 plaintext bytes of good siblings with the same extension. */
    private fun siblingFirstBlocks(securedFile: File, dao: SecuredMediaItemEntityDao): List<ByteArray> {
        val parent = securedFile.parentFile ?: return emptyList()
        val ext = securedFile.extension.lowercase()
        val siblings = parent.listFiles()
            ?.filter { it != securedFile && it.isFile && it.extension.lowercase() == ext }
            ?: return emptyList()

        val result = ArrayList<ByteArray>()
        for (sibling in siblings) {
            if (result.size >= MAX_SIBLING_CANDIDATES) break
            val siblingIv = dao.getIvFromSecuredPath(sibling.absolutePath) ?: continue
            if (siblingIv.size != 16 || siblingIv.all { it.toInt() == 0 }) continue
            val head = try {
                sibling.inputStream().use { stream ->
                    ByteArray(32).takeIf { stream.read(it) >= 32 }
                }
            } catch (e: Exception) { null } ?: continue

            EncryptionManager.decryptFirstBlock(head, siblingIv)?.let { result.add(it) }
        }
        return result
    }

    /** Known first-16-byte plaintext candidates per format. */
    private fun constantsFor(kind: Kind): List<ByteArray> = when (kind) {
        Kind.PNG -> listOf(EncryptionManager.KNOWN_PNG_FIRST_BLOCK)
        Kind.JPEG -> JPEG_FIRST_BLOCKS
        Kind.VIDEO -> WEBM_FIRST_BLOCKS
        Kind.IMAGE_OTHER -> emptyList()
    }

    /** Common JPEG headers (JFIF / EXIF); siblings are the primary source, these are fallbacks. */
    private val JPEG_FIRST_BLOCKS = listOf(
        byteArrayOf( // JFIF APP0, no density units
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01
        ),
        byteArrayOf( // JFIF APP0, version 1.1 with aspect-ratio units
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48
        )
    )

    /** EBML/WebM header prefixes produced by common muxers (ffmpeg / libwebm). */
    private val WEBM_FIRST_BLOCKS = listOf(
        byteArrayOf( // 8-byte EBML size descriptor (value 0x1F)
            0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(),
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F,
            0x42, 0x86.toByte(), 0x81.toByte(), 0x01
        ),
        byteArrayOf( // 1-byte EBML size descriptor (0x9F)
            0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(),
            0x9F.toByte(), 0x42, 0x86.toByte(), 0x81.toByte(),
            0x01, 0x42, 0xF7.toByte(), 0x81.toByte(), 0x01,
            0x42, 0xF2.toByte(), 0x81.toByte()
        )
    )
}
