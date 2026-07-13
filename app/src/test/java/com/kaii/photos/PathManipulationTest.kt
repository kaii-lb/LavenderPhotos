package com.kaii.photos

import android.provider.MediaStore
import com.kaii.photos.helpers.toBasePath
import com.kaii.photos.helpers.toRelativePath
import com.kaii.photos.helpers.volumeName
import org.junit.Assert
import org.junit.Test

class PathManipulationTest {
    val baseStorageDir = "/storage/emulated/0"

    val currentVolumes = setOf(
        MediaStore.VOLUME_EXTERNAL,
        "A1B2-C3D4"
    )

    @Test
    fun testToBasePathInternalStorage() {
        val path = "/storage/emulated/0/Download/documents/file.txt"
        Assert.assertEquals("/storage/emulated/0", path.toBasePath(baseStorageDir))
    }

    @Test
    fun testToBasePathTreeDocumentUri() {
        val path = "/tree/primary:Download/document/file.txt"
        Assert.assertEquals("/storage/emulated/0", path.toBasePath(baseStorageDir))
    }

    @Test
    fun testToBasePathExternalStorage() {
        val path = "/storage/A1B2-C3D4/Android/data/com.example/files"
        Assert.assertEquals("/storage/A1B2-C3D4", path.toBasePath(baseStorageDir))
    }

    @Test
    fun testToBasePathExternalTreeDocumentUri() {
        val path = "/tree/A1B2-C3D4:Download/document/file.txt"
        Assert.assertEquals("/storage/A1B2-C3D4", path.toBasePath(baseStorageDir))
    }

    @Test
    fun testToRelativePathWithPrefix() {
        Assert.assertEquals(
            "Download/Folder/photo.jpg",
            "/storage/emulated/0/Download/Folder/photo.jpg".toRelativePath(baseStorageDir)
        )

        Assert.assertEquals(
            "Download/Folder",
            "/storage/emulated/0/Download/Folder".toRelativePath(baseStorageDir)
        )
    }

    @Test
    fun testToRelativePathTreeUris() {
        Assert.assertEquals(
            "Download/Resume.pdf",
            "/tree/primary:Download/document/primary:Download/Resume.pdf".toRelativePath(baseStorageDir)
        )

        Assert.assertEquals(
            "Download/Quick Share",
            "/tree/primary:Download/document/primary:Download/Quick Share".toRelativePath(baseStorageDir)
        )
    }

    @Test
    fun testVolumeNameTreeDocumentUri() {
        Assert.assertEquals(
            "external",
            "/tree/primary:Download/document/primary:Download/Resume.pdf".volumeName(currentVolumes, baseStorageDir)
        )

        Assert.assertEquals(
            "A1B2-C3D4",
            "/tree/A1B2-C3D4:Download/document/primary:Download/Resume.pdf".volumeName(currentVolumes, baseStorageDir)
        )
    }
}