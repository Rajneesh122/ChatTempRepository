package com.rws.learningproject01.platform

import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.core.storage.TiffWriter
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import okio.FileSystem
import okio.Path
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object ThumbnailGenerator {
    actual fun generatePlaceholder(width: Int, height: Int, output: Path) {
        val maxEdge = 512
        val scale = minOf(maxEdge.toFloat() / width, maxEdge.toFloat() / height, 1f)
        val thumbW = (width * scale).toInt().coerceAtLeast(1)
        val thumbH = (height * scale).toInt().coerceAtLeast(1)
        val bitmap = ImageBitmap(thumbW, thumbH)
        val pixels = bitmap.getPixels()
        for (i in pixels.indices step 4) {
            pixels[i] = 0x1E
            pixels[i + 1] = 0x1E
            pixels[i + 2] = 0x1E
            pixels[i + 3] = 0xFF.toByte()
        }
        bitmap.setPixels(pixels)
        FileSystem.SYSTEM.write(output) { write(bitmap.compressPng()) }
        bitmap.recycle()
    }
}

@OptIn(ExperimentalForeignApi::class)
actual object ExportPlatform {
    actual fun exportBitmap(bitmap: ImageBitmap, format: ExportFormat, output: Path, jpegQuality: Int) {
        output.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        val bytes = when (format) {
            ExportFormat.PNG -> bitmap.compressPng()
            ExportFormat.JPEG -> bitmap.compressJpeg(jpegQuality)
            ExportFormat.TIFF -> TiffWriter.writeRgbBytes(bitmap.width, bitmap.height, bitmap.getPixels())
            ExportFormat.GIF -> bitmap.compressPng()
            ExportFormat.PDF -> renderPdf(bitmap)
            ExportFormat.MP4, ExportFormat.HEVC -> error("Use exportAnimationFrames for video formats")
        }
        FileSystem.SYSTEM.write(output) { write(bytes) }
    }

    actual fun exportAnimationFrames(
        frames: List<ImageBitmap>,
        format: ExportFormat,
        output: Path,
        fps: Int,
        jpegQuality: Int,
        force4K: Boolean,
    ) {
        output.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        when (format) {
            ExportFormat.GIF, ExportFormat.MP4, ExportFormat.HEVC ->
                VideoExportServiceIos.encodeFrames(frames, output, fps, format, force4K)
            else -> {
                val dir = output.parent!!.resolve(output.name.substringBeforeLast('.') + "_frames")
                FileSystem.SYSTEM.createDirectories(dir)
                frames.forEachIndexed { index, frame ->
                    val frameFile = dir.resolve("frame_${index.toString().padStart(4, '0')}.png")
                    FileSystem.SYSTEM.write(frameFile) { write(frame.compressPng()) }
                }
            }
        }
    }

    actual fun thumbnailExtension(): String = "png"

    private fun renderPdf(bitmap: ImageBitmap): ByteArray {
        val renderer = UIGraphicsPDFRenderer(bounds = platform.CoreGraphics.CGRectMake(0.0, 0.0, bitmap.width.toDouble(), bitmap.height.toDouble()))
        val data = renderer.PDFDataWithActions { context ->
            context?.beginPage()
            val image = bitmap.asUIImage()
            image.drawAtPoint(platform.CoreGraphics.CGPointMake(0.0, 0.0))
        }
        return data?.toByteArray() ?: ByteArray(0)
    }
}

@OptIn(ExperimentalForeignApi::class)
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
