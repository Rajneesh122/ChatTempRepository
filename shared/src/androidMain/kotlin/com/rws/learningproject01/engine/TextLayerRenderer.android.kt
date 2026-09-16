package com.rws.learningproject01.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(buffer))
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = content.fontSize
            color = parseColor(content.color)
            typeface = Typeface.DEFAULT
        }
        val x = meta.transform.x.coerceIn(0f, width.toFloat())
        val y = (meta.transform.y + content.fontSize).coerceIn(content.fontSize, height.toFloat())
        canvas.drawText(content.text, x, y, paint)
        bitmap.copyPixelsToBuffer(java.nio.ByteBuffer.wrap(buffer))
        bitmap.recycle()
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
