package com.rws.learningproject01.platform

import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE

expect object Lz4Codec {
    fun compress(raw: ByteArray): ByteArray
    fun decompress(compressed: ByteArray, uncompressedSize: Int): ByteArray
}

fun Lz4Codec.tileRawSize(tileSize: Int = DEFAULT_TILE_SIZE): Int =
    tileSize * tileSize * 4

fun Lz4Codec.rgbaTileBuffer(tileSize: Int = DEFAULT_TILE_SIZE): ByteArray =
    ByteArray(tileRawSize(tileSize))

fun Lz4Codec.clearTile(buffer: ByteArray) {
    buffer.fill(0)
}

fun encodeRgba(buffer: ByteArray, tileSize: Int = DEFAULT_TILE_SIZE): ByteArray {
    val rawSize = tileSize * tileSize * 4
    val header = byteArrayOf(
        (rawSize and 0xFF).toByte(),
        ((rawSize shr 8) and 0xFF).toByte(),
        ((rawSize shr 16) and 0xFF).toByte(),
        ((rawSize shr 24) and 0xFF).toByte(),
    )
    return header + Lz4Codec.compress(buffer)
}

fun decodeRgba(encoded: ByteArray): ByteArray {
    val size = (encoded[0].toInt() and 0xFF) or
        ((encoded[1].toInt() and 0xFF) shl 8) or
        ((encoded[2].toInt() and 0xFF) shl 16) or
        ((encoded[3].toInt() and 0xFF) shl 24)
    return Lz4Codec.decompress(encoded.copyOfRange(4, encoded.size), size)
}
