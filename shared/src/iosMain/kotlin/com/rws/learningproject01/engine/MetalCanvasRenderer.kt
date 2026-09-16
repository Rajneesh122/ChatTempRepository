package com.rws.learningproject01.engine

import com.rws.learningproject01.platform.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
class MetalCanvasRenderer(
    private val engine: ArtEngine,
    private val onReady: () -> Unit = {},
) {
    var canvasWidth = 1
    var canvasHeight = 1

    fun compositeUIImage(): UIImage {
        val width = canvasWidth.coerceAtLeast(1)
        val height = canvasHeight.coerceAtLeast(1)
        val image = engine.withStateLock {
            val layerOrder = engine.layerOrder()
            val frameOverrides = if (engine.animation.enabled) {
                engine.animation.frameOverrides(engine.animation.currentFrameIndex)
            } else {
                emptyMap()
            }
            val bitmap = CanvasCompositor.compositeToBitmap(
                layers = engine.layers.toList(),
                layerOrder = layerOrder,
                width = width,
                height = height,
                includeBackground = true,
                backgroundColor = 0xFFFFFFFF.toInt(),
                frameOverrides = frameOverrides,
            )
            bitmap.asUIImage()
        }
        onReady()
        return image
    }

    fun screenToCanvas(screenX: Float, screenY: Float, viewWidth: Int, viewHeight: Int): Pair<Float, Float> {
        val canvasW = canvasWidth.toFloat()
        val canvasH = canvasHeight.toFloat()
        val fitScale = min(viewWidth / canvasW, viewHeight / canvasH) * 0.9f * engine.viewportZoom
        val offsetX = (viewWidth - canvasW * fitScale) / 2f + engine.viewportPanX
        val offsetY = (viewHeight - canvasH * fitScale) / 2f + engine.viewportPanY
        val x = (screenX - offsetX) / fitScale
        val y = (screenY - offsetY) / fitScale
        return x to y
    }

    fun invalidate() {
        // Host view should call compositeUIImage() after invalidation.
    }
}
