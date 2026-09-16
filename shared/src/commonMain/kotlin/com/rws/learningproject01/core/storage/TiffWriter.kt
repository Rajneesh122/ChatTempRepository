package com.rws.learningproject01.core.storage

import okio.FileSystem
import okio.Path

object TiffWriter {
    fun writeRgb(path: Path, width: Int, height: Int, rgba: ByteArray) {
        FileSystem.SYSTEM.write(path) { write(buildRgb(width, height, rgba)) }
    }

    fun writeRgbBytes(width: Int, height: Int, rgba: ByteArray): ByteArray =
        buildRgb(width, height, rgba)

    private fun buildRgb(width: Int, height: Int, rgba: ByteArray): ByteArray {
        val imageDataSize = width * height * 3
        val stripOffset = 8 + 2 + 12 * 8 + 4
        val totalSize = stripOffset + imageDataSize
        val out = ByteArray(totalSize)
        var pos = 0

        fun putShort(value: Int) {
            out[pos++] = (value and 0xFF).toByte()
            out[pos++] = ((value shr 8) and 0xFF).toByte()
        }

        fun putInt(value: Int) {
            out[pos++] = (value and 0xFF).toByte()
            out[pos++] = ((value shr 8) and 0xFF).toByte()
            out[pos++] = ((value shr 16) and 0xFF).toByte()
            out[pos++] = ((value shr 24) and 0xFF).toByte()
        }

        putShort(0x4949)
        putShort(42)
        putInt(8)
        putShort(8)
        putShort(256); putShort(3); putInt(1); putInt(width)
        putShort(257); putShort(3); putInt(1); putInt(height)
        putShort(258); putShort(3); putInt(3); putInt(stripOffset + imageDataSize)
        putShort(259); putShort(3); putInt(1); putInt(1)
        putShort(262); putShort(3); putInt(1); putInt(2)
        putShort(273); putShort(4); putInt(1); putInt(stripOffset)
        putShort(277); putShort(3); putInt(1); putInt(3)
        putShort(278); putShort(3); putInt(1); putInt(height)
        putInt(0)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = (y * width + x) * 4
                out[pos++] = rgba[idx]
                out[pos++] = rgba[idx + 1]
                out[pos++] = rgba[idx + 2]
            }
        }
        return out
    }
}
