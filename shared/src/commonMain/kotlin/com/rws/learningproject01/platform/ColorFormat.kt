package com.rws.learningproject01.platform

fun argbToHex(color: Int): String {
    val value = color.toUInt().toString(16).padStart(8, '0').uppercase()
    return "#$value"
}
