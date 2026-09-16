package com.rws.learningproject01.core.storage

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** Minimal GIF89a encoder for animation export. */
object GifEncoder {

    fun encode(frames: List<Bitmap>, output: File, delayMs: Int) {
        require(frames.isNotEmpty()) { "No frames to encode" }
        val width = frames.first().width
        val height = frames.first().height
        FileOutputStream(output).use { out ->
            writeHeader(out, width, height)
            frames.forEach { frame ->
                writeGraphicControl(out, delayMs)
                writeImageDescriptor(out, width, height)
                writeLzwImageData(out, frame, width, height)
            }
            out.write(0x3B)
        }
    }

    private fun writeHeader(out: FileOutputStream, width: Int, height: Int) {
        out.write("GIF89a".toByteArray())
        writeShort(out, width)
        writeShort(out, height)
        out.write(0xF7)
        out.write(0x00)
        out.write(0x00)
        repeat(256 * 3) { out.write(0) }
    }

    private fun writeGraphicControl(out: FileOutputStream, delayMs: Int) {
        out.write(0x21)
        out.write(0xF9)
        out.write(0x04)
        out.write(0x00)
        writeShort(out, (delayMs / 10).coerceAtLeast(1))
        out.write(0x00)
        out.write(0x00)
    }

    private fun writeImageDescriptor(out: FileOutputStream, width: Int, height: Int) {
        out.write(0x2C)
        writeShort(out, 0)
        writeShort(out, 0)
        writeShort(out, width)
        writeShort(out, height)
        out.write(0x00)
    }

    private fun writeLzwImageData(out: FileOutputStream, bitmap: Bitmap, width: Int, height: Int) {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val indexed = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val argb = pixels[i]
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            indexed[i] = ((r + g + b) / 3).toByte()
        }
        out.write(0x08)
        val block = indexed.take(255).toByteArray()
        out.write(block.size)
        out.write(block)
        out.write(0x00)
    }

    private fun writeShort(out: FileOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}
