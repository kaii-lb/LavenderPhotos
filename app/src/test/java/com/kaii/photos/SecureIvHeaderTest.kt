package com.kaii.photos

import com.kaii.photos.helpers.SecureIvRecovery
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureIvHeaderTest {
    private val jpegMagic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val pngMagic =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val webpMagic = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val gifMagic = byteArrayOf(0x47, 0x49, 0x46, 0x38)
    private val webmMagic = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())

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
    fun magicForRecognisesGatedFormatsAndIgnoresUngatedOnes() {
        assertArrayEquals(jpegMagic, SecureIvRecovery.magicFor("image/jpeg"))
        assertArrayEquals(jpegMagic, SecureIvRecovery.magicFor("image/jpg"))
        assertArrayEquals(pngMagic, SecureIvRecovery.magicFor("image/png"))
        assertArrayEquals(webpMagic, SecureIvRecovery.magicFor("image/webp"))
        assertArrayEquals(gifMagic, SecureIvRecovery.magicFor("image/gif"))
        assertArrayEquals(webmMagic, SecureIvRecovery.magicFor("video/webm"))
        // mp4/mov have no byte-0 magic (size-prefixed boxes), so they stay ungated
        assertNull(SecureIvRecovery.magicFor("video/mp4"))
        assertNull(SecureIvRecovery.magicFor("video/quicktime"))
        assertNull(SecureIvRecovery.magicFor(null))
    }

    @Test
    fun magicForReturnsDefensiveCopy() {
        val first = SecureIvRecovery.magicFor("image/png")!!
        first[0] = 0x00
        // mutating the returned array must not corrupt the next caller's magic
        assertArrayEquals(pngMagic, SecureIvRecovery.magicFor("image/png"))
    }

    @Test
    fun webmFirstBlockWithCorrectMagicIsAccepted() {
        val firstBlock = byteArrayOf(
            0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(),
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F,
            0x42, 0x86.toByte(), 0x81.toByte(), 0x01
        )
        assertTrue(SecureIvRecovery.headerMatchesMagic(firstBlock, webmMagic))
    }

    @Test
    fun computeInSampleSizeReturnsOneWhenAlreadyAtOrBelowTarget() {
        // both dimensions already <= target: no downsampling
        assertEquals(1, SecureIvRecovery.computeInSampleSize(64, 64, 64))
        assertEquals(1, SecureIvRecovery.computeInSampleSize(40, 50, 64))
        // just over target on one axis is still not enough to justify halving below target
        assertEquals(1, SecureIvRecovery.computeInSampleSize(100, 100, 64))
    }

    @Test
    fun computeInSampleSizeGrowsByPowersOfTwo() {
        // loop steps while width/(sample*2) >= target AND height/(sample*2) >= target
        // 128/2 = 64 >= 64 -> sample 2; 128/4 = 32 < 64 -> stop
        assertEquals(2, SecureIvRecovery.computeInSampleSize(128, 128, 64))
        // 256/2=128, /4=64 >=64 -> sample 4; /8=32 <64 -> stop
        assertEquals(4, SecureIvRecovery.computeInSampleSize(256, 256, 64))
        // 512 -> sample 8
        assertEquals(8, SecureIvRecovery.computeInSampleSize(512, 512, 64))
    }

    @Test
    fun computeInSampleSizeGatedByTheSmallerDimension() {
        // wide but short: height short-circuits growth so the wide axis stays under-sampled
        // 1000x50, target 64: 50/2 < 64 immediately -> sample stays 1
        assertEquals(1, SecureIvRecovery.computeInSampleSize(1000, 50, 64))
        // 1000x200, target 64: 200/2=100>=64 -> 2; 200/4=50<64 stop -> sample 2
        assertEquals(2, SecureIvRecovery.computeInSampleSize(1000, 200, 64))
    }

    @Test
    fun computeInSampleSizeHandlesZeroAndTinyDimensions() {
        // a zero dimension can never satisfy width/(sample*2) >= target, so sample stays 1
        assertEquals(1, SecureIvRecovery.computeInSampleSize(0, 0, 64))
        assertEquals(1, SecureIvRecovery.computeInSampleSize(1, 1, 64))
    }
}
