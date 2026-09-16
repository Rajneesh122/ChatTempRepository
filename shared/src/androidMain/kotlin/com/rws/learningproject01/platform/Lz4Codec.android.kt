package com.rws.learningproject01.platform

import net.jpountz.lz4.LZ4Factory

actual object Lz4Codec {
    private val factory = LZ4Factory.fastestInstance()
    private val compressor = factory.fastCompressor()
    private val decompressor = factory.fastDecompressor()

    actual fun compress(raw: ByteArray): ByteArray {
        val maxCompressed = compressor.maxCompressedLength(raw.size)
        val compressed = ByteArray(maxCompressed)
        val length = compressor.compress(raw, 0, raw.size, compressed, 0, maxCompressed)
        return compressed.copyOf(length)
    }

    actual fun decompress(compressed: ByteArray, uncompressedSize: Int): ByteArray {
        val restored = ByteArray(uncompressedSize)
        decompressor.decompress(compressed, 0, restored, 0, uncompressedSize)
        return restored
    }
}
