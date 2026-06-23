package com.kaii.photos.widgets

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


class QrCodeState {
    private val writer = QRCodeWriter()
    private var size = 512
    private var content = ""

    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    /** @param size in pixels */
    fun setSize(size: Int) {
        this.size = size
    }

    fun setContent(content: String) {
        this.content = content
        generate()
    }

    private fun generate() {
        if (content.isBlank() || size <= 0) {
            bitmap = null
            return
        }

        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height

        val pixels = IntArray(width * height)
        for (y in 0..<height) {
            val offset = y * width
            for (x in 0..<width) {
                pixels[offset + x] =
                    if (bitMatrix.get(x, y)) Color.Black.toArgb()
                    else Color.White.toArgb()
            }
        }

        bitmap = createBitmap(width, height).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}

@Composable
fun rememberQrCodeState(): QrCodeState {
    return remember {
        QrCodeState()
    }
}