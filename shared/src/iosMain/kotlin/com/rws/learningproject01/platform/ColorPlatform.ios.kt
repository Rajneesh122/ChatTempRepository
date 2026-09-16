package com.rws.learningproject01.platform

actual fun parseColorHex(hex: String): Int {
    val normalized = hex.removePrefix("#")
    return when (normalized.length) {
        6 -> {
            val rgb = normalized.toLong(16).toInt()
            0xFF000000.toInt() or rgb
        }
        8 -> normalized.toLong(16).toInt()
        else -> 0xFF000000.toInt()
    }
}
