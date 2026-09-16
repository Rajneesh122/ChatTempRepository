package com.rws.learningproject01.platform

import android.graphics.Color

actual fun parseColorHex(hex: String): Int {
    val normalized = hex.removePrefix("#")
    return when (normalized.length) {
        6 -> Color.parseColor("#FF$normalized")
        8 -> Color.parseColor("#$normalized")
        else -> Color.BLACK
    }
}
