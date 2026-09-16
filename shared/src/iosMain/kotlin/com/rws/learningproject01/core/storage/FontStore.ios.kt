package com.rws.learningproject01.core.storage

import com.rws.learningproject01.platform.PlatformContext
import com.rws.learningproject01.platform.platformFilesDir
import okio.FileSystem
import okio.Path.Companion.toPath

actual class FontStore actual constructor(context: PlatformContext) {
    private val fs = FileSystem.SYSTEM
    private val fontsRoot = platformFilesDir(context).toPath().resolve("fonts")

    init {
        fs.createDirectories(fontsRoot)
    }

    actual fun listFontIds(): List<String> =
        listOf("default") + (if (fs.exists(fontsRoot)) {
            fs.list(fontsRoot)
                .filter { it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true) }
                .map { it.name.substringBeforeLast('.') }
        } else {
            emptyList()
        })

    actual fun importFont(bytes: ByteArray, fileName: String): String {
        val safeName = fileName.substringAfterLast('/').replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = fontsRoot.resolve(safeName)
        fs.write(target) { write(bytes) }
        return target.name.substringBeforeLast('.')
    }

    actual fun rasterizeText(
        buffer: ByteArray,
        width: Int,
        height: Int,
        text: String,
        fontSize: Float,
        color: Int,
        x: Float,
        y: Float,
        fontId: String,
    ) {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        var cursorX = x.toInt()
        val cursorY = (y + fontSize).toInt().coerceIn(0, height - 1)
        text.forEach { _ ->
            if (cursorX >= width) return@forEach
            val idx = (cursorY * width + cursorX) * 4
            if (idx + 3 < buffer.size) {
                buffer[idx] = r.toByte()
                buffer[idx + 1] = g.toByte()
                buffer[idx + 2] = b.toByte()
                buffer[idx + 3] = 0xFF.toByte()
            }
            cursorX += (fontSize * 0.6f).toInt().coerceAtLeast(1)
        }
    }
}
