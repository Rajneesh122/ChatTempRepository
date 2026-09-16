package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.LayerMeta
import com.rws.learningproject01.platform.ImageBitmap
import com.rws.learningproject01.platform.createImageBitmapFromRgba

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
        val rgba = ByteArray(width * height * 4)
        if (includeBackground) {
            val a = (backgroundColor shr 24) and 0xFF
            val r = (backgroundColor shr 16) and 0xFF
            val g = (backgroundColor shr 8) and 0xFF
            val b = backgroundColor and 0xFF
            for (i in 0 until width * height) {
                val idx = i * 4
                rgba[idx] = r.toByte()
                rgba[idx + 1] = g.toByte()
                rgba[idx + 2] = b.toByte()
                rgba[idx + 3] = a.toByte()
            }
        }
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
            blendLayer(rgba, flat, width, height, opacity)
        }
        return createImageBitmapFromRgba(width, height, rgba)
    }

    private fun blendLayer(dst: ByteArray, src: ByteArray, width: Int, height: Int, opacity: Float) {
        val alphaScale = opacity.coerceIn(0f, 1f)
        for (i in 0 until width * height) {
            val idx = i * 4
            val srcA = (src[idx + 3].toInt() and 0xFF) / 255f * alphaScale
            if (srcA <= 0f) continue
            val dstA = (dst[idx + 3].toInt() and 0xFF) / 255f
            val outA = srcA + dstA * (1f - srcA)
            if (outA <= 0f) continue
            val srcR = (src[idx].toInt() and 0xFF) / 255f
            val srcG = (src[idx + 1].toInt() and 0xFF) / 255f
            val srcB = (src[idx + 2].toInt() and 0xFF) / 255f
            val dstR = (dst[idx].toInt() and 0xFF) / 255f
            val dstG = (dst[idx + 1].toInt() and 0xFF) / 255f
            val dstB = (dst[idx + 2].toInt() and 0xFF) / 255f
            dst[idx] = ((srcR * srcA + dstR * dstA * (1f - srcA)) / outA * 255).toInt().coerceIn(0, 255).toByte()
            dst[idx + 1] = ((srcG * srcA + dstG * dstA * (1f - srcA)) / outA * 255).toInt().coerceIn(0, 255).toByte()
            dst[idx + 2] = ((srcB * srcA + dstB * dstA * (1f - srcA)) / outA * 255).toInt().coerceIn(0, 255).toByte()
            dst[idx + 3] = (outA * 255).toInt().coerceIn(0, 255).toByte()
        }
    }
}
