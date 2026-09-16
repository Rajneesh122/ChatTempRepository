package com.rws.learningproject01.core.storage

import com.rws.learningproject01.core.model.CanvasSpec

object PsdImporter {

    data class PsdImportResult(
        val title: String,
        val canvas: CanvasSpec,
        val layers: List<PsdLayer>,
    )

    data class PsdLayer(
        val name: String,
        val pixels: ByteArray,
        val width: Int,
        val height: Int,
        val offsetX: Int,
        val offsetY: Int,
    )

    fun import(bytes: ByteArray, fileName: String): PsdImportResult {
        var offset = 0
        val signature = readAscii(bytes, offset, 4)
        offset += 4
        require(signature == "8BPS") { "Not a valid PSD file" }
        offset += 2 // version
        offset += 6
        offset += 2 // channels
        val height = readInt32(bytes, offset)
        offset += 4
        val width = readInt32(bytes, offset)
        offset += 4
        offset += 2 // depth
        offset += 2 // color mode
        val colorModeLen = readInt32(bytes, offset)
        offset += 4 + colorModeLen
        val resourceLen = readInt32(bytes, offset)
        offset += 4
        val resourceEnd = offset + resourceLen
        val layerNames = mutableListOf<String>()
        if (resourceLen > 0) {
            parseLayerNames(bytes, offset, resourceEnd, layerNames)
        }
        offset = resourceEnd
        val layerMaskLen = readInt32(bytes, offset)
        offset += 4 + layerMaskLen
        val compression = readInt16(bytes, offset)
        offset += 2
        val composite = when (compression) {
            0 -> readRawRgba(bytes, offset, width, height)
            1 -> readRleRgba(bytes, offset, width, height)
            else -> ByteArray(width * height * 4)
        }
        val title = fileName.substringBeforeLast('.').ifBlank { "Imported" }
        val layers = if (layerNames.isEmpty()) {
            listOf(PsdLayer("Background", composite, width, height, 0, 0))
        } else {
            layerNames.map { name ->
                PsdLayer(name, composite.copyOf(), width, height, 0, 0)
            }
        }
        return PsdImportResult(title, CanvasSpec(width = width, height = height), layers)
    }

    private fun parseLayerNames(bytes: ByteArray, start: Int, resourceEnd: Int, names: MutableList<String>) {
        var offset = start
        while (offset < resourceEnd - 12) {
            val blockId = readInt16(bytes, offset)
            offset += 2
            offset = readPascalString(bytes, offset).second
            val blockSize = readInt32(bytes, offset)
            offset += 4
            val blockStart = offset
            if (blockId == 0x0C4C && blockSize > 0) {
                val layerInfoLen = readInt32(bytes, offset)
                offset += 4
                if (layerInfoLen > 0) {
                    val layerCount = kotlin.math.abs(readInt16(bytes, offset))
                    offset += 2
                    repeat(layerCount.coerceAtMost(64)) {
                        offset += 16
                        offset += 2
                        val extraLen = readInt32(bytes, offset)
                        offset += 4 + extraLen
                        val nameLen = bytes[offset].toInt() and 0xFF
                        offset += 1
                        val nameBytes = bytes.copyOfRange(offset, (offset + nameLen).coerceAtMost(bytes.size))
                        offset += nameLen
                        names.add(decodeAscii(nameBytes).trim())
                        offset += (255 - nameLen).coerceAtLeast(0)
                    }
                }
            }
            offset = (blockStart + blockSize).coerceAtMost(resourceEnd)
        }
    }

    private fun readRawRgba(bytes: ByteArray, start: Int, width: Int, height: Int): ByteArray {
        var offset = start
        val channels = 4
        val planeSize = width * height
        val planes = Array(channels) { ByteArray(planeSize) }
        repeat(channels) { channel ->
            bytes.copyInto(planes[channel], 0, offset, offset + planeSize)
            offset += planeSize
        }
        return planesToRgba(planes, width, height)
    }

    private fun readRleRgba(bytes: ByteArray, start: Int, width: Int, height: Int): ByteArray {
        var offset = start
        val channels = 4
        val planeSize = width * height
        val planes = Array(channels) { ByteArray(planeSize) }
        repeat(channels) { channel ->
            repeat(height) { row ->
                readInt16(bytes, offset)
                offset += 2
            }
            var pixelOffset = 0
            repeat(height) {
                val rowEnd = pixelOffset + width
                while (pixelOffset < rowEnd && offset < bytes.size) {
                    val header = bytes[offset++].toInt()
                    if (header < 128) {
                        val count = header + 1
                        repeat(count) {
                            if (pixelOffset < rowEnd && offset < bytes.size) {
                                planes[channel][pixelOffset++] = bytes[offset++]
                            }
                        }
                    } else {
                        val value = bytes[offset++]
                        val count = 257 - header
                        repeat(count) {
                            if (pixelOffset < rowEnd) planes[channel][pixelOffset++] = value
                        }
                    }
                }
            }
        }
        return planesToRgba(planes, width, height)
    }

    private fun planesToRgba(planes: Array<ByteArray>, width: Int, height: Int): ByteArray {
        val rgba = ByteArray(width * height * 4)
        val planeSize = width * height
        for (i in 0 until planeSize) {
            rgba[i * 4] = planes[1][i]
            rgba[i * 4 + 1] = planes[2][i]
            rgba[i * 4 + 2] = planes[3][i]
            rgba[i * 4 + 3] = planes[0][i]
        }
        return rgba
    }

    private fun readAscii(bytes: ByteArray, offset: Int, length: Int): String =
        decodeAscii(bytes, offset, length)

    private fun readPascalString(bytes: ByteArray, offset: Int): Pair<String, Int> {
        if (offset >= bytes.size) return "" to offset
        val len = bytes[offset].toInt() and 0xFF
        var newOffset = offset + 1
        if (len == 0 || newOffset + len > bytes.size) return "" to newOffset
        val value = decodeAscii(bytes, newOffset, len)
        newOffset += len
        if ((len + 1) % 2 != 0 && newOffset < bytes.size) newOffset++
        return value to newOffset
    }

    private fun readInt16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun readInt32(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
}
