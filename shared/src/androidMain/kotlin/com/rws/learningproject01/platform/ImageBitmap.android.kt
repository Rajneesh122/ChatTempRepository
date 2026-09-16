package com.rws.learningproject01.platform

import android.graphics.Bitmap
import java.nio.ByteBuffer

actual class ImageBitmap actual constructor(
    actual val width: Int,
    actual val height: Int,
) {
    private val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    internal fun asAndroidBitmap(): Bitmap = bitmap

    actual fun getPixels(): ByteArray {
        val buffer = ByteArray(width * height * 4)
        val intPixels = IntArray(width * height)
        bitmap.getPixels(intPixels, 0, width, 0, 0, width, height)
        for (i in intPixels.indices) {
            val argb = intPixels[i]
            val idx = i * 4
            buffer[idx] = ((argb shr 16) and 0xFF).toByte()
            buffer[idx + 1] = ((argb shr 8) and 0xFF).toByte()
            buffer[idx + 2] = (argb and 0xFF).toByte()
            buffer[idx + 3] = ((argb shr 24) and 0xFF).toByte()
        }
        return buffer
    }

    actual fun setPixels(rgba: ByteArray) {
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgba))
    }

    actual fun compressPng(quality: Int): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality.coerceIn(0, 100), stream)
        return stream.toByteArray()
    }

    actual fun compressJpeg(quality: Int): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), stream)
        return stream.toByteArray()
    }

    actual fun compressWebp(quality: Int): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.PNG
        }
        bitmap.compress(format, quality.coerceIn(0, 100), stream)
        return stream.toByteArray()
    }

    actual fun recycle() {
        bitmap.recycle()
    }
}

actual fun createImageBitmapFromRgba(width: Int, height: Int, rgba: ByteArray): ImageBitmap {
    val image = ImageBitmap(width, height)
    image.setPixels(rgba)
    return image
}

actual fun scaleImageBitmap(source: ImageBitmap, width: Int, height: Int): ImageBitmap {
    val scaled = Bitmap.createScaledBitmap(source.asAndroidBitmap(), width, height, true)
    val result = ImageBitmap(width, height)
    val canvas = android.graphics.Canvas(result.asAndroidBitmap())
    canvas.drawBitmap(scaled, 0f, 0f, null)
    scaled.recycle()
    return result
}
