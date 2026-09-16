package com.rws.learningproject01.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.rws.learningproject01.core.model.BlendMode
import com.rws.learningproject01.platform.ImageBitmap
import com.rws.learningproject01.platform.createImageBitmapFromRgba
import java.nio.ByteBuffer

actual object CanvasCompositor {
    actual fun compositeToBitmap(
        layers: List<LayerData>,
        layerOrder: List<String>,
        width: Int,
        height: Int,
        includeBackground: Boolean,
        backgroundColor: Int,
        frameOverrides: Map<String, Pair<Boolean, Float>>,
    ): ImageBitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (includeBackground) {
            canvas.drawColor(backgroundColor)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        layerOrder.forEach { layerId ->
            val layer = layers.find { it.id == layerId } ?: return@forEach
            val override = frameOverrides[layer.id]
            val visible = override?.first ?: layer.meta.visible
            val opacity = override?.second ?: layer.meta.opacity
            if (!visible || opacity <= 0f) return@forEach
            val flat = layer.flattenToRgba(width, height)
            if (layer.meta.type == "text") {
                TextLayerRenderer.rasterizeOntoBuffer(flat, width, height, layer.meta)
            }
            val layerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            layerBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(flat))
            paint.alpha = (opacity * 255).toInt().coerceIn(0, 255)
            applyBlendMode(paint, layer.meta.blendMode)
            canvas.drawBitmap(layerBitmap, 0f, 0f, paint)
            paint.xfermode = null
            layerBitmap.recycle()
        }
        val rgba = ByteArray(width * height * 4)
        bitmap.copyPixelsToBuffer(ByteBuffer.wrap(rgba))
        bitmap.recycle()
        return createImageBitmapFromRgba(width, height, rgba)
    }

    private fun applyBlendMode(paint: Paint, blendModeId: String) {
        val mode = when (BlendMode.fromId(blendModeId)) {
            BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            BlendMode.DARKEN -> PorterDuff.Mode.DARKEN
            BlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            else -> null
        }
        if (mode != null) {
            paint.xfermode = PorterDuffXfermode(mode)
        }
    }
}
