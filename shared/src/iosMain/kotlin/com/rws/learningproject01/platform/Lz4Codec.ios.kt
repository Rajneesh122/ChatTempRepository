package com.rws.learningproject01.platform

actual object Lz4Codec {
    actual fun compress(raw: ByteArray): ByteArray = Lz4Pure.compress(raw)

    actual fun decompress(compressed: ByteArray, uncompressedSize: Int): ByteArray =
        Lz4Pure.decompress(compressed, uncompressedSize)
}
