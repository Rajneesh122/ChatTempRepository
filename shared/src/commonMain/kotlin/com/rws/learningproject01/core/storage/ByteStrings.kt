package com.rws.learningproject01.core.storage

internal fun decodeAscii(bytes: ByteArray, offset: Int, length: Int): String {
    val end = (offset + length).coerceAtMost(bytes.size)
    return buildString(end - offset) {
        for (i in offset until end) {
            append((bytes[i].toInt() and 0xFF).toChar())
        }
    }
}

internal fun decodeAscii(bytes: ByteArray): String = decodeAscii(bytes, 0, bytes.size)
