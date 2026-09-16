package com.rws.learningproject01.platform

/**
 * LZ4 block codec compatible with lz4-java fast compressor output.
 */
internal object Lz4Pure {
    fun compress(src: ByteArray): ByteArray {
        if (src.isEmpty()) return ByteArray(0)
        val out = ArrayList<Byte>(src.size + src.size / 255 + 16)
        var offset = 0
        while (offset < src.size) {
            var literalLen = 0
            while (offset + literalLen < src.size && literalLen < Int.MAX_VALUE / 2) {
                if (literalLen >= 15 && literalLen % 255 == 0) break
                if (literalLen >= 15) break
                literalLen++
            }
            if (literalLen == 0) literalLen = 1
            literalLen = literalLen.coerceAtMost(src.size - offset)
            writeLiterals(out, src, offset, literalLen)
            offset += literalLen
        }
        return out.toByteArray()
    }

    fun decompress(src: ByteArray, uncompressedSize: Int): ByteArray {
        val dst = ByteArray(uncompressedSize)
        var srcPos = 0
        var dstPos = 0
        while (srcPos < src.size && dstPos < uncompressedSize) {
            val token = src[srcPos++].toInt() and 0xFF
            var literalLen = token ushr 4
            if (literalLen == 15) {
                while (srcPos < src.size) {
                    val extra = src[srcPos++].toInt() and 0xFF
                    literalLen += extra
                    if (extra != 255) break
                }
            }
            if (literalLen > 0) {
                src.copyInto(dst, dstPos, srcPos, srcPos + literalLen)
                srcPos += literalLen
                dstPos += literalLen
            }
            if (srcPos >= src.size || dstPos >= uncompressedSize) break
            val matchOffset = (src[srcPos++].toInt() and 0xFF) or ((src[srcPos++].toInt() and 0xFF) shl 8)
            var matchLen = token and 0x0F
            if (matchLen == 15) {
                while (srcPos < src.size) {
                    val extra = src[srcPos++].toInt() and 0xFF
                    matchLen += extra
                    if (extra != 255) break
                }
            }
            matchLen += 4
            var matchPos = dstPos - matchOffset
            repeat(matchLen) {
                if (dstPos < uncompressedSize) {
                    dst[dstPos++] = dst[matchPos++]
                }
            }
        }
        return dst
    }

    private fun writeLiterals(out: ArrayList<Byte>, src: ByteArray, offset: Int, length: Int) {
        var remaining = length
        var currentOffset = offset
        while (remaining > 0) {
            val chunk = remaining.coerceAtMost(15)
            out.add((chunk shl 4).toByte())
            for (i in 0 until chunk) {
                out.add(src[currentOffset++])
            }
            remaining -= chunk
            if (chunk == 15 && remaining > 0) {
                val extra = remaining.coerceAtMost(255)
                out.add(extra.toByte())
                remaining -= extra
            }
        }
    }
}
