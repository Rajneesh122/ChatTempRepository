package com.rws.learningproject01.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.core.storage.GifEncoder
import com.rws.learningproject01.core.storage.TiffWriter
import com.rws.learningproject01.core.storage.VideoExportService
import okio.FileSystem
import okio.Path
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual object ThumbnailGenerator {
    actual fun generatePlaceholder(width: Int, height: Int, output: Path) {
        val maxEdge = 512
        val scale = minOf(maxEdge.toFloat() / width, maxEdge.toFloat() / height, 1f)
        val thumbW = (width * scale).toInt().coerceAtLeast(1)
        val thumbH = (height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFF1E1E1E.toInt())
        val bytes = bitmap.compressToBytes()
        bitmap.recycle()
        FileSystem.SYSTEM.write(output) { write(bytes) }
    }

    private fun Bitmap.compressToBytes(): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.PNG
        }
        compress(format, 80, stream)
        return stream.toByteArray()
    }
}

actual object ExportPlatform {
    actual fun exportBitmap(bitmap: ImageBitmap, format: ExportFormat, output: Path, jpegQuality: Int) {
        val androidBitmap = bitmap.asAndroidBitmap()
        val file = File(output.toString())
        file.parentFile?.mkdirs()
        when (format) {
            ExportFormat.PNG -> writeBitmap(androidBitmap, Bitmap.CompressFormat.PNG, 100, file)
            ExportFormat.JPEG -> writeBitmap(androidBitmap, Bitmap.CompressFormat.JPEG, jpegQuality, file)
            ExportFormat.TIFF -> writeTiff(androidBitmap, file)
            ExportFormat.GIF -> GifEncoder.encode(listOf(androidBitmap), file, delayMs = 100)
            ExportFormat.PDF -> writePdf(androidBitmap, file)
            ExportFormat.MP4, ExportFormat.HEVC -> error("Use exportAnimationFrames for video formats")
        }
    }

    actual fun exportAnimationFrames(
        frames: List<ImageBitmap>,
        format: ExportFormat,
        output: Path,
        fps: Int,
        jpegQuality: Int,
        force4K: Boolean,
    ) {
        val androidFrames = frames.map { it.asAndroidBitmap() }
        val file = File(output.toString())
        file.parentFile?.mkdirs()
        when (format) {
            ExportFormat.GIF -> GifEncoder.encode(androidFrames, file, delayMs = (1000 / fps).coerceAtLeast(20))
            ExportFormat.MP4, ExportFormat.HEVC ->
                VideoExportService.encodeFrames(
                    androidFrames,
                    file,
                    fps,
                    useHevc = format == ExportFormat.HEVC,
                    force4K = force4K,
                )
            else -> {
                val dir = File(file.parentFile, file.nameWithoutExtension + "_frames").apply { mkdirs() }
                androidFrames.forEachIndexed { index, frame ->
                    val frameFile = File(dir, "frame_${index.toString().padStart(4, '0')}.png")
                    writeBitmap(frame, Bitmap.CompressFormat.PNG, 100, frameFile)
                }
            }
        }
    }

    actual fun thumbnailExtension(): String = "webp"

    private fun writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, output: File) {
        output.outputStream().use { stream ->
            bitmap.compress(format, quality, stream)
        }
    }

    private fun writePdf(bitmap: Bitmap, output: File) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        document.finishPage(page)
        output.outputStream().use { document.writeTo(it) }
        document.close()
    }

    private fun writeTiff(bitmap: Bitmap, output: File) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val buffer = ByteBuffer.allocate(pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        pixels.forEach { argb ->
            buffer.put((argb and 0xFF).toByte())
            buffer.put(((argb shr 8) and 0xFF).toByte())
            buffer.put(((argb shr 16) and 0xFF).toByte())
            buffer.put(((argb shr 24) and 0xFF).toByte())
        }
        output.outputStream().use { out ->
            out.write(TiffWriter.writeRgbBytes(width, height, buffer.array()))
        }
    }
}
