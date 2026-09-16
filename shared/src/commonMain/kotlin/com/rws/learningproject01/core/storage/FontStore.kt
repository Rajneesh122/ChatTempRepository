package com.rws.learningproject01.core.storage

import com.rws.learningproject01.platform.PlatformContext

expect class FontStore(context: PlatformContext) {
    fun listFontIds(): List<String>
    fun importFont(bytes: ByteArray, fileName: String): String
    fun rasterizeText(
        buffer: ByteArray,
        width: Int,
        height: Int,
        text: String,
        fontSize: Float,
        color: Int,
        x: Float,
        y: Float,
        fontId: String,
    )
}
