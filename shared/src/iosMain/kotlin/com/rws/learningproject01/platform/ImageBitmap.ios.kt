package com.rws.learningproject01.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class ImageBitmap actual constructor(
    actual val width: Int,
    actual val height: Int,
) {
    private var image: UIImage = createEmptyImage(width, height)

    actual fun getPixels(): ByteArray {
        val cgImage = image.CGImage ?: return ByteArray(width * height * 4)
        val bytesPerRow = width * 4
        val buffer = ByteArray(bytesPerRow * height)
        buffer.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                pinned.addressOf(0),
                width.toULong(),
                height.toULong(),
                8u,
                bytesPerRow.toULong(),
                CGColorSpaceCreateDeviceRGB(),
                CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            )
            if (context != null) {
                CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cgImage)
            }
        }
        return buffer
    }

    actual fun setPixels(rgba: ByteArray) {
        image = createImageFromRgba(width, height, rgba)
    }

    actual fun compressPng(quality: Int): ByteArray =
        UIImagePNGRepresentation(image)?.toByteArray() ?: ByteArray(0)

    actual fun compressJpeg(quality: Int): ByteArray =
        UIImageJPEGRepresentation(image, quality / 100.0)?.toByteArray() ?: ByteArray(0)

    actual fun compressWebp(quality: Int): ByteArray = compressPng(quality)

    actual fun recycle() {
        image = createEmptyImage(1, 1)
    }

    fun asUIImage(): UIImage = image
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun createImageBitmapFromRgba(width: Int, height: Int, rgba: ByteArray): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    bitmap.setPixels(rgba)
    return bitmap
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun scaleImageBitmap(source: ImageBitmap, width: Int, height: Int): ImageBitmap {
    val cgImage = source.asUIImage().CGImage
    val result = ImageBitmap(width, height)
    if (cgImage != null) {
        result.setPixels(source.getPixels())
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
private fun createEmptyImage(width: Int, height: Int): UIImage {
    val bytesPerRow = width * 4
    val buffer = ByteArray(bytesPerRow * height)
    return createImageFromRgba(width, height, buffer)
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun createImageFromRgba(width: Int, height: Int, rgba: ByteArray): UIImage {
    val bytesPerRow = width * 4
    rgba.usePinned { pinned ->
        val context = CGBitmapContextCreate(
            pinned.addressOf(0),
            width.toULong(),
            height.toULong(),
            8u,
            bytesPerRow.toULong(),
            CGColorSpaceCreateDeviceRGB(),
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        ) ?: return UIImage()
        val cgImage = CGBitmapContextCreateImage(context) ?: return UIImage()
        return UIImage(cGImage = cgImage)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}
