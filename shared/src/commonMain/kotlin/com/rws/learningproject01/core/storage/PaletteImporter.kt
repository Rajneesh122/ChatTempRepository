package com.rws.learningproject01.core.storage

object PaletteImporter {

    fun importFromBytes(bytes: ByteArray, fileName: String): List<Int> =
        when {
            fileName.endsWith(".ase", ignoreCase = true) -> parseAse(bytes)
            fileName.endsWith(".aco", ignoreCase = true) -> parseAco(bytes)
            else -> parseAse(bytes).ifEmpty { parseAco(bytes) }
        }

    fun parseAse(bytes: ByteArray): List<Int> {
        if (bytes.size < 12) return emptyList()
        var offset = 0
        val header = readString(bytes, offset, 4)
        offset += 4
        if (header != "ASEF") return emptyList()
        offset += 2 // version major
        offset += 2 // version minor
        val blockCount = readInt16(bytes, offset)
        offset += 4
        val colors = mutableListOf<Int>()
        repeat(blockCount.coerceAtMost(1024)) {
            if (offset + 6 > bytes.size) return@repeat
            val blockType = readInt16(bytes, offset)
            offset += 2
            val blockLength = readInt32(bytes, offset)
            offset += 4
            if (blockType == 0x0001) {
                val nameLen = readInt16(bytes, offset)
                offset += 2
                if (nameLen > 0 && offset + nameLen * 2 <= bytes.size) {
                    offset += nameLen * 2
                }
            } else if (blockType == 0x0002 && offset + 10 <= bytes.size) {
                val colorModel = readInt16(bytes, offset)
                offset += 2
                when (colorModel) {
                    0 -> {
                        val r = readInt16(bytes, offset) / 65535f
                        offset += 2
                        val g = readInt16(bytes, offset) / 65535f
                        offset += 2
                        val b = readInt16(bytes, offset) / 65535f
                        offset += 2
                        colors.add(floatRgbToArgb(r, g, b))
                    }
                    1 -> {
                        val c = readInt16(bytes, offset)
                        offset += 2
                        val m = readInt16(bytes, offset)
                        offset += 2
                        val y = readInt16(bytes, offset)
                        offset += 2
                        val k = readInt16(bytes, offset)
                        offset += 2
                        colors.add(cmykToArgb(c, m, y, k))
                    }
                    else -> offset += 6
                }
                val nameLen = readInt16(bytes, offset)
                offset += 2
                if (nameLen > 0 && offset + nameLen * 2 <= bytes.size) {
                    offset += nameLen * 2
                }
            } else {
                offset = (offset + blockLength - 6).coerceAtMost(bytes.size)
            }
        }
        return colors
    }

    fun parseAco(bytes: ByteArray): List<Int> {
        if (bytes.size < 4) return emptyList()
        var offset = 0
        val version = readInt16(bytes, offset)
        offset += 2
        val count = readInt16(bytes, offset)
        offset += 2
        val colors = mutableListOf<Int>()
        repeat(count.coerceAtMost(1024)) {
            if (offset + 10 > bytes.size) return@repeat
            val colorSpace = readInt16(bytes, offset)
            offset += 2
            when (colorSpace) {
                0 -> {
                    val r = readInt16(bytes, offset) / 65535f
                    offset += 2
                    val g = readInt16(bytes, offset) / 65535f
                    offset += 2
                    val b = readInt16(bytes, offset) / 65535f
                    offset += 2
                    colors.add(floatRgbToArgb(r, g, b))
                }
                2 -> {
                    val c = readInt16(bytes, offset)
                    offset += 2
                    val m = readInt16(bytes, offset)
                    offset += 2
                    val y = readInt16(bytes, offset)
                    offset += 2
                    val k = readInt16(bytes, offset)
                    offset += 2
                    colors.add(cmykToArgb(c, m, y, k))
                }
                else -> offset += 6
            }
            if (version == 2 && offset + 4 <= bytes.size) {
                val nameLen = readInt32(bytes, offset)
                offset += 4
                if (nameLen > 0 && offset + nameLen * 2 <= bytes.size) {
                    offset += nameLen * 2
                }
            }
        }
        return colors
    }

    private fun readString(bytes: ByteArray, offset: Int, length: Int): String =
        decodeAscii(bytes, offset, length)

    private fun readInt16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun readInt32(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private fun floatRgbToArgb(r: Float, g: Float, b: Float): Int {
        val ri = (r.coerceIn(0f, 1f) * 255).toInt()
        val gi = (g.coerceIn(0f, 1f) * 255).toInt()
        val bi = (b.coerceIn(0f, 1f) * 255).toInt()
        return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
    }

    private fun cmykToArgb(c: Int, m: Int, y: Int, k: Int): Int {
        val cf = c / 65535f
        val mf = m / 65535f
        val yf = y / 65535f
        val kf = k / 65535f
        val r = 255 * (1 - cf) * (1 - kf)
        val g = 255 * (1 - mf) * (1 - kf)
        val b = 255 * (1 - yf) * (1 - kf)
        return floatRgbToArgb(r / 255f, g / 255f, b / 255f)
    }
}
