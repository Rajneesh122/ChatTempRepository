package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.LayerMeta

actual object TextLayerRenderer {
    actual fun rasterizeOntoBuffer(
        buffer: ByteArray,
        width: Int,
        height: Int,
        meta: LayerMeta,
        fontId: String,
    ) {
        val content = meta.textContent ?: return
        val color = parseColor(content.color)
        val x = meta.transform.x.coerceIn(0f, width.toFloat())
        val y = meta.transform.y.coerceIn(0f, height.toFloat())
        // Simplified bitmap font rasterization placeholder; full CoreText path uses FontStore on iOS.
        val text = content.text
        var cursorX = x.toInt()
        val cursorY = (y + content.fontSize).toInt().coerceIn(0, height - 1)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        text.forEach { _ ->
            if (cursorX >= width) return@forEach
            val idx = (cursorY * width + cursorX) * 4
            if (idx + 3 < buffer.size) {
                buffer[idx] = r.toByte()
                buffer[idx + 1] = g.toByte()
                buffer[idx + 2] = b.toByte()
                buffer[idx + 3] = 0xFF.toByte()
            }
            cursorX += (content.fontSize * 0.6f).toInt().coerceAtLeast(1)
        }
    }

    actual fun createTextLayerMeta(
        name: String,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float,
        color: Int,
        width: Int,
        height: Int,
    ): LayerMeta = createTextLayerMetaCommon(name, text, x, y, fontSize, color, width, height)
}
