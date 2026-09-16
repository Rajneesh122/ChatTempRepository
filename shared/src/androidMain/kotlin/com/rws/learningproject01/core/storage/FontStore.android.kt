package com.rws.learningproject01.core.storage

import android.graphics.Typeface
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
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(buffer))
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = fontSize
            this.color = color
            typeface = loadTypeface(fontId)
        }
        canvas.drawText(text, x, y + fontSize, paint)
        bitmap.copyPixelsToBuffer(java.nio.ByteBuffer.wrap(buffer))
        bitmap.recycle()
    }

    private fun loadTypeface(fontId: String): Typeface {
        if (fontId == "default") return Typeface.DEFAULT
        val file = if (fs.exists(fontsRoot)) {
            fs.list(fontsRoot).find { it.name.substringBeforeLast('.') == fontId }
        } else {
            null
        }
        return file?.let { Typeface.createFromFile(it.toString()) } ?: Typeface.DEFAULT
    }
}
