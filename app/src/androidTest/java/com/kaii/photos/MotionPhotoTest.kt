package com.kaii.photos

import android.Manifest
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kaii.photos.helpers.appStorageDir
import org.junit.Rule
import org.junit.Test
import java.io.File

class MotionPhotoTest {
    @get:Rule
    val grantPermissionRule: GrantPermissionRule? = GrantPermissionRule.grant(
        Manifest.permission.READ_MEDIA_IMAGES
    )

    @Test
    fun printXmpData() {
        val exifInterface = ExifInterface("/storage/emulated/0/Pictures/Nightmean - Pixel 9 Pro XL - Ultra HDR.jpg")

        val xmpData = exifInterface.getAttribute(ExifInterface.TAG_XMP)

        assert(xmpData != null)

        val dir = InstrumentationRegistry.getInstrumentation().targetContext.appStorageDir
        val file = File(dir, "xmpdata.txt")

        File(dir).mkdirs()

        if (file.exists()) {
            file.delete()
            file.createNewFile()
        }

        file.writeText(xmpData!!)

        println("File's path ${file.absolutePath}")
    }
}