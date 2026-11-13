package com.kaii.photos

import com.kaii.photos.helpers.AlbumCoverCrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCoverCropTest {
    @Test
    fun landscapeStartsCentered() {
        assertEquals(AlbumCoverCrop(500, 0, 1000), AlbumCoverCrop.calculate(2000, 1000, 1f, 0.5f, 0.5f))
    }

    @Test
    fun portraitStartsCentered() {
        assertEquals(AlbumCoverCrop(0, 500, 1000), AlbumCoverCrop.calculate(1000, 2000, 1f, 0.5f, 0.5f))
    }

    @Test
    fun zoomAndPanCanReachEveryCorner() {
        assertEquals(AlbumCoverCrop(0, 0, 500), AlbumCoverCrop.calculate(2000, 1000, 2f, 0f, 0f))
        assertEquals(AlbumCoverCrop(1500, 500, 500), AlbumCoverCrop.calculate(2000, 1000, 2f, 1f, 1f))
    }

    @Test
    fun extremeDragsAndZoomStayInsideImage() {
        for ((width, height) in listOf(1 to 1, 1 to 100, 101 to 1, 301 to 799, 2048 to 1535)) {
            for (zoom in listOf(-1f, 1f, 1.3f, 3f, 5f, 100f)) {
                for (position in listOf(-100f, 0f, 0.333f, 0.5f, 1f, 100f)) {
                    val crop = AlbumCoverCrop.calculate(width, height, zoom, position, position)
                    assertTrue(crop.size > 0 && crop.left >= 0 && crop.top >= 0)
                    assertTrue(crop.left + crop.size <= width)
                    assertTrue(crop.top + crop.size <= height)
                }
            }
        }
    }

    @Test
    fun maximumZoomStillKeepsAValidSquare() {
        assertEquals(AlbumCoverCrop(400, 400, 200), AlbumCoverCrop.calculate(1000, 1000, 5f, 0.5f, 0.5f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyImagesAreRejected() {
        AlbumCoverCrop.calculate(0, 1000, 1f, 0.5f, 0.5f)
    }
}
