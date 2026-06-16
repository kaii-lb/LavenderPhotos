package com.kaii.photos.helpers

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.database.entities.SecuredItemEntity
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Recovers AES-CBC IVs lost to the failed-secure catch bug (db stored ByteArray(0)).
 *
 * In CBC the IV only affects the first 16-byte plaintext block, so a zero-IV decrypt is byte-perfect
 * from offset 16 on and only wrong in bytes 0..15 — the file itself is intact. The original IV is
 * then `D_K(C0) xor P0`, where D_K(C0) is the zero-IV first block and P0 the true first 16 plaintext
 * bytes. We try candidate P0 values (format constants + the real first block of good siblings) and
 * validate each by actually decoding the reconstructed file, so a wrong guess is rejected rather than
 * persisted. For PNG the first 16 bytes are invariant so recovery is exact; for JPEG/WebM bytes 0..15
 * are format metadata, so a validating-but-imperfect candidate can leave those few bytes slightly off
 * while the media still decodes — never silent file corruption, but not a bit-exact guarantee either.
 *
 * Extend by adding a [Kind] and its constants in [constantsFor].
 */
object SecureIvRecovery {
    private const val TAG = "com.kaii.photos.helpers.SecureIvRecovery"

    /** Cap on good siblings sampled as P0 candidates, to bound cost on large folders. */
    private const val MAX_SIBLING_CANDIDATES = 24

    /** securedPaths that failed recovery this session, so we don't re-probe them on every load/open. */
    private val unrecoverable = ConcurrentHashMap.newKeySet<String>()

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
     *
     * Memory is bounded regardless of file size: the zero-IV plaintext is streamed to a temp file
     * once, then each candidate only patches its first 16 bytes (bytes 16+ are identical).
     */
    fun recover(
        context: Context,
        securedFile: File,
        mimeType: String?,
        dao: SecuredMediaItemEntityDao
    ): ByteArray? {
        if (securedFile.absolutePath in unrecoverable) return null

        val kind = mimeType?.lowercase()?.let {
            when {
                it.contains("png") -> Kind.PNG
                it.contains("jpeg") || it.contains("jpg") -> Kind.JPEG
                it.contains("webm") || it.contains("video") -> Kind.VIDEO
                it.contains("image") -> Kind.IMAGE_OTHER
                else -> null
            }
        } ?: return null

        if (securedFile.length() < 32) return null

        // D_K(C0): zero-IV decrypt of just the first block (only 32 cipher bytes read)
        val zeroFirstBlock = try {
            securedFile.inputStream().use { s -> ByteArray(32).takeIf { s.read(it) >= 32 } }
                ?.let { EncryptionManager.decryptFirstBlock(it, ByteArray(16)) }
        } catch (e: Throwable) {
            Log.e(TAG, "recover: first-block decrypt failed for ${securedFile.name}", e)
            null
        } ?: return markUnrecoverable(securedFile)

        // stream the full zero-IV decrypt to a temp file once; it's byte-correct from offset 16 on,
        // so each candidate just patches the first 16 bytes in place and re-validates. don't cache a
        // temp-creation failure as unrecoverable — it's transient (e.g. low disk) and may work later
        val tempPlain = try {
            File.createTempFile("iv_recovery_", ".tmp", context.cacheDir)
        } catch (e: Throwable) {
            Log.e(TAG, "recover: could not create temp file for ${securedFile.name}", e)
            return null
        }
        try {
            try {
                EncryptionManager.decryptInputStream(
                    inputStream = securedFile.inputStream(),
                    outputStream = tempPlain.outputStream(),
                    fileSize = securedFile.length(),
                    iv = ByteArray(16)
                )
            } catch (e: Throwable) {
                Log.e(TAG, "recover: zero-iv decrypt failed for ${securedFile.name}", e)
                return markUnrecoverable(securedFile)
            }

            // constants first, then siblings; de-duplicated to avoid re-validating the same guess
            val candidates = LinkedHashSet<List<Byte>>()
            constantsFor(kind).forEach { candidates.add(it.toList()) }
            siblingFirstBlocks(securedFile, dao).forEach { candidates.add(it.toList()) }

            for (candidate in candidates) {
                val p0 = candidate.toByteArray()
                RandomAccessFile(tempPlain, "rw").use { it.seek(0); it.write(p0) }

                if (!validates(context, tempPlain, kind)) continue

                val iv = ByteArray(16) { i -> (zeroFirstBlock[i].toInt() xor p0[i].toInt()).toByte() }
                // all-zero is this app's not-ready/corrupt sentinel and also signals a bad guess
                if (iv.all { it.toInt() == 0 }) continue

                Log.d(TAG, "recover: validated IV for ${securedFile.name} ($kind)")
                return iv
            }
        } finally {
            tempPlain.delete()
        }

        Log.e(TAG, "recover: no candidate validated for ${securedFile.name} ($kind)")
        return markUnrecoverable(securedFile)
    }

    private fun markUnrecoverable(securedFile: File): ByteArray? {
        unrecoverable.add(securedFile.absolutePath)
        return null
    }

    /**
     * Cheap trust check for a stored 16-byte IV: decrypt the first ciphertext block and compare it to
     * the format's magic bytes. In CBC a wrong IV corrupts only those leading bytes, so this catches a
     * valid-length-but-wrong IV without a full-file decrypt. Returns true (trust it) for formats with no
     * magic we gate on, a non-16-byte IV, or an unreadable file; false means the caller should re-recover.
     */
    fun ivProducesValidHeader(securedFile: File, iv: ByteArray, mimeType: String?): Boolean {
        if (iv.size != 16) return true
        val magic = magicFor(mimeType) ?: return true

        val head = try {
            securedFile.inputStream().use { s -> ByteArray(32).takeIf { s.read(it) >= 32 } }
        } catch (e: Throwable) {
            Log.e(TAG, "ivProducesValidHeader: read failed for ${securedFile.name}", e)
            null
        } ?: return true

        val firstBlock = EncryptionManager.decryptFirstBlock(head, iv) ?: return true
        return headerMatchesMagic(firstBlock, magic)
    }

    /** Pure: does [firstBlock] begin with every byte of [magic]? Exposed for unit testing. */
    fun headerMatchesMagic(firstBlock: ByteArray, magic: ByteArray): Boolean {
        if (firstBlock.size < magic.size) return false
        for (i in magic.indices) if (firstBlock[i] != magic[i]) return false
        return true
    }

    /** Invariant leading bytes per gated format, or null when we don't gate the format. */
    fun magicFor(mimeType: String?): ByteArray? {
        val m = mimeType?.lowercase() ?: return null
        return when {
            m.contains("png") -> PNG_MAGIC
            m.contains("jpeg") || m.contains("jpg") -> JPEG_MAGIC
            else -> null
        }
    }

    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    private val PNG_MAGIC = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    private enum class Kind { PNG, JPEG, VIDEO, IMAGE_OTHER }

    /** Validate the reconstructed plaintext file: full (downsampled) image decode, or video metadata. */
    private fun validates(context: Context, plaintext: File, kind: Kind): Boolean = when (kind) {
        Kind.VIDEO -> validatesVideo(plaintext)
        else -> validatesImage(plaintext)
    }

    private fun validatesImage(plaintext: File): Boolean {
        // header-only bounds (inJustDecodeBounds) can pass on a wrong-IV candidate whose leading bytes
        // still parse; force an actual downsampled decode so only real pixels are accepted.
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return try {
            BitmapFactory.decodeFile(plaintext.absolutePath, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return false

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = computeInSampleSize(opts.outWidth, opts.outHeight, 64)
            }
            val bitmap = BitmapFactory.decodeFile(plaintext.absolutePath, decodeOpts)
            (bitmap != null).also { bitmap?.recycle() }
        } catch (e: Throwable) {
            false
        }
    }

    /** Smallest power-of-two sample size that brings both dimensions at/below [target]. */
    private fun computeInSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) {
            sample *= 2
        }
        return sample
    }

    private fun validatesVideo(plaintext: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(plaintext.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            hasVideo == "yes" || (width?.toIntOrNull() ?: 0) > 0
        } catch (e: Exception) {
            false
        } finally {
            try { retriever.release() } catch (_: Exception) {}
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
