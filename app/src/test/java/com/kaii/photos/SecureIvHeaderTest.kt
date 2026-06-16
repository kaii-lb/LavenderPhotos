package com.kaii.photos

import com.kaii.photos.helpers.SecureIvRecovery
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureIvHeaderTest {
    private val jpegMagic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val pngMagic =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test
    fun jpegFirstBlockWithCorrectMagicIsAccepted() {
        val firstBlock = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01
        )
        assertTrue(SecureIvRecovery.headerMatchesMagic(firstBlock, jpegMagic))
    }

    @Test
    fun jpegFirstBlockFromWrongIvIsRejected() {
        val garbage = byteArrayOf(
            0x13, 0x37, 0x42, 0x00, 0x11, 0x22, 0x33, 0x44,
            0x55, 0x66, 0x77, 0x88.toByte(), 0x99.toByte(), 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()
        )
        assertFalse(SecureIvRecovery.headerMatchesMagic(garbage, jpegMagic))
    }

    @Test
    fun pngFirstBlockWithCorrectSignatureIsAccepted() {
        val firstBlock = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        assertTrue(SecureIvRecovery.headerMatchesMagic(firstBlock, pngMagic))
    }

    @Test
    fun pngSignatureDoesNotSatisfyJpegMagic() {
        val pngBlock = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        )
        assertFalse(SecureIvRecovery.headerMatchesMagic(pngBlock, jpegMagic))
    }

    @Test
    fun blockShorterThanMagicIsRejected() {
        assertFalse(SecureIvRecovery.headerMatchesMagic(byteArrayOf(0xFF.toByte(), 0xD8.toByte()), jpegMagic))
    }

    @Test
    fun magicForRecognisesJpegAndPngAndIgnoresOthers() {
        assertArrayEquals(jpegMagic, SecureIvRecovery.magicFor("image/jpeg"))
        assertArrayEquals(jpegMagic, SecureIvRecovery.magicFor("image/jpg"))
        assertArrayEquals(pngMagic, SecureIvRecovery.magicFor("image/png"))
        assertNull(SecureIvRecovery.magicFor("video/webm"))
        assertNull(SecureIvRecovery.magicFor("image/gif"))
        assertNull(SecureIvRecovery.magicFor(null))
    }
}
