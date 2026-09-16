package com.rws.learningproject01.engine

import com.rws.learningproject01.platform.argbToHex
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import com.rws.learningproject01.core.model.BlendMode
import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import com.rws.learningproject01.core.model.LayerMeta
import com.rws.learningproject01.core.model.MAX_UNDO_LEVELS
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ArtEngine(
    private val canvasWidth: Int,
    private val canvasHeight: Int,
) {
    val layers = mutableListOf<LayerData>()
    var activeLayerIndex: Int = 0
        private set
    var brush = BrushSettings()
        private set
    var activeTool: DrawingTool = DrawingTool.BRUSH
    var symmetryMode: SymmetryMode = SymmetryMode.OFF
    var quickShapeEnabled: Boolean = true
    var colorDropTolerance: Int = 32

    val animation = AnimationController()
    val timelapse = TimelapseRecorder(canvasWidth, canvasHeight)

    var viewportZoom: Float = 1f
    var viewportPanX: Float = 0f
    var viewportPanY: Float = 0f

    private val undoStack = ArrayDeque<UndoSnapshot>()
    private val redoStack = ArrayDeque<UndoSnapshot>()
    private var strokeActive = false
    private val currentStrokePoints = mutableListOf<StrokePoint>()
    private var smudgeColor: Int = 0xFF000000.toInt()

    internal val stateLock = SynchronizedObject()

    fun <T> withStateLock(block: () -> T): T = synchronized(stateLock, block)

    fun updateBrush(settings: BrushSettings) {
        brush = settings
    }

    fun addLayer(name: String): LayerMeta {
        val layer = LayerData.create(name, canvasWidth, canvasHeight, layers.size + 1)
        layers.add(layer)
        activeLayerIndex = layers.lastIndex
        return layer.meta
    }

    fun deleteLayer(index: Int): Boolean {
        if (layers.size <= 1 || index !in layers.indices) return false
        layers.removeAt(index)
        activeLayerIndex = activeLayerIndex.coerceIn(0, layers.lastIndex)
        return true
    }

    fun moveLayer(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in layers.indices || toIndex !in layers.indices || fromIndex == toIndex) return false
        val layer = layers.removeAt(fromIndex)
        layers.add(toIndex, layer)
        activeLayerIndex = layers.indexOfFirst { it.id == layer.id }
        return true
    }

    fun setActiveLayer(index: Int) {
        if (index in layers.indices) activeLayerIndex = index
    }

    fun getActiveLayer(): LayerData? = layers.getOrNull(activeLayerIndex)

    fun loadLayer(meta: LayerMeta, tiles: Map<Pair<Int, Int>, ByteArray>) {
        val layer = LayerData.fromMeta(meta, canvasWidth, canvasHeight)
        layer.tiles.putAll(tiles)
        layers.add(layer)
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        layers.getOrNull(index)?.let { layer ->
            layer.meta = layer.meta.copy(opacity = opacity.coerceIn(0f, 1f))
        }
    }

    fun setLayerBlendMode(index: Int, blendMode: BlendMode) {
        layers.getOrNull(index)?.let { layer ->
            layer.meta = layer.meta.copy(blendMode = blendMode.id)
        }
    }

    fun toggleLayerClipMask(index: Int) {
        layers.getOrNull(index)?.let { layer ->
            layer.meta = layer.meta.copy(clipToBelow = !layer.meta.clipToBelow)
        }
    }

    fun toggleLayerLock(index: Int) {
        layers.getOrNull(index)?.let { layer ->
            layer.meta = layer.meta.copy(locked = !layer.meta.locked)
        }
    }

    fun toggleLayerVisibility(index: Int) {
        layers.getOrNull(index)?.let { layer ->
            layer.meta = layer.meta.copy(visible = !layer.meta.visible)
        }
    }

    fun beginStroke() {
        val layer = getActiveLayer() ?: return
        if (layer.meta.locked) return
        if (strokeActive) return
        strokeActive = true
        currentStrokePoints.clear()
        timelapse.onStrokeBegin(layer.id, brush.color, brush.size)
        undoStack.addLast(UndoSnapshot(layer.id, layer.copyTiles()))
        redoStack.clear()
        while (undoStack.size > MAX_UNDO_LEVELS) undoStack.removeFirst()
    }

    fun addStrokePoint(point: StrokePoint) {
        val layer = getActiveLayer() ?: return
        if (layer.meta.locked) return
        if (activeTool == DrawingTool.COLOR_DROP || activeTool == DrawingTool.COLOR_PICKER || activeTool == DrawingTool.TEXT) return
        if (!strokeActive) beginStroke()

        val smoothed = smoothPoint(point)
        val last = currentStrokePoints.lastOrNull()
        if (last != null) {
            when (activeTool) {
                DrawingTool.SMUDGE -> interpolateSmudge(layer, last, smoothed)
                DrawingTool.ERASER -> interpolateStroke(layer, last, smoothed, isEraser = true)
                DrawingTool.BRUSH -> interpolateStroke(layer, last, smoothed, isEraser = false)
                DrawingTool.COLOR_DROP, DrawingTool.COLOR_PICKER, DrawingTool.TEXT -> Unit
            }
        } else {
            when (activeTool) {
                DrawingTool.SMUDGE -> smudgeStamp(layer, smoothed.x, smoothed.y, smoothed.pressure)
                DrawingTool.ERASER -> stamp(layer, smoothed.x, smoothed.y, smoothed.pressure, isEraser = true)
                DrawingTool.BRUSH -> stamp(layer, smoothed.x, smoothed.y, smoothed.pressure, isEraser = false)
                DrawingTool.COLOR_DROP, DrawingTool.COLOR_PICKER, DrawingTool.TEXT -> Unit
            }
        }
        currentStrokePoints.add(smoothed)
        timelapse.onStrokePoint(smoothed.x, smoothed.y, smoothed.pressure)
        layer.markDirty()
    }

    fun endStroke() {
        if (!strokeActive) return
        val layer = getActiveLayer() ?: return
        if (quickShapeEnabled && activeTool == DrawingTool.BRUSH && currentStrokePoints.size >= 2) {
            applyQuickShape(layer)
        }
        strokeActive = false
        timelapse.onStrokeEnd(layer.id)
        currentStrokePoints.clear()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val snapshot = undoStack.removeLast()
        val layer = layers.find { it.id == snapshot.layerId } ?: return false
        redoStack.addLast(UndoSnapshot(layer.id, layer.copyTiles()))
        layer.restoreTiles(snapshot.tiles)
        layer.markDirty()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val snapshot = redoStack.removeLast()
        val layer = layers.find { it.id == snapshot.layerId } ?: return false
        undoStack.addLast(UndoSnapshot(layer.id, layer.copyTiles()))
        layer.restoreTiles(snapshot.tiles)
        layer.markDirty()
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun collectDirtyTiles(): Map<String, List<Pair<Int, Int>>> =
        layers.associate { layer -> layer.id to layer.consumeDirtyTiles() }

    fun flattenActiveLayerPixels(): Map<String, ByteArray> =
        layers.associate { layer -> layer.id to layer.flattenToRgba(canvasWidth, canvasHeight) }

    fun addTextLayer(
        text: String = "Text",
        x: Float = 100f,
        y: Float = 100f,
        fontSize: Float = 48f,
        color: Int = 0xFF000000.toInt(),
    ): LayerMeta {
        val meta = TextLayerRenderer.createTextLayerMeta(
            name = "Text ${layers.size + 1}",
            text = text,
            x = x,
            y = y,
            fontSize = fontSize,
            color = color,
            width = canvasWidth,
            height = canvasHeight,
        )
        val layer = LayerData.fromMeta(meta, canvasWidth, canvasHeight)
        layers.add(layer)
        activeLayerIndex = layers.lastIndex
        rasterizeTextLayer(layer)
        return meta
    }

    fun updateActiveTextLayer(
        text: String? = null,
        fontSize: Float? = null,
        color: Int? = null,
        fontId: String? = null,
    ) {
        val layer = getActiveLayer() ?: return
        if (layer.meta.type != "text") return
        val content = layer.meta.textContent ?: return
        layer.meta = layer.meta.copy(
            textContent = content.copy(
                text = text ?: content.text,
                fontSize = fontSize ?: content.fontSize,
                color = color?.let { argbToHex(it) } ?: content.color,
                fontId = fontId ?: content.fontId,
            ),
        )
        rasterizeTextLayer(layer)
    }

    private fun rasterizeTextLayer(layer: LayerData) {
        val flat = layer.flattenToRgba(canvasWidth, canvasHeight)
        flat.fill(0)
        TextLayerRenderer.rasterizeOntoBuffer(flat, canvasWidth, canvasHeight, layer.meta)
        ColorDropFill.writePixelsToLayer(layer, flat, canvasWidth, canvasHeight)
        layer.markDirty()
    }

    fun layerMetas(): List<LayerMeta> = layers.map { it.meta }

    fun layerOrder(): List<String> = layers.map { it.id }

    fun pushLayerUndo(layer: LayerData) {
        undoStack.addLast(UndoSnapshot(layer.id, layer.copyTiles()))
        redoStack.clear()
        while (undoStack.size > MAX_UNDO_LEVELS) undoStack.removeFirst()
    }

    fun pickColorAt(x: Float, y: Float): Int? {
        val layer = getActiveLayer() ?: return null
        return sampleColorAt(layer, x, y)
    }

    fun performColorDrop(x: Float, y: Float): Boolean {
        val layer = getActiveLayer() ?: return false
        if (layer.meta.locked) return false
        pushLayerUndo(layer)
        val flat = layer.flattenToRgba(canvasWidth, canvasHeight)
        val filled = ColorDropFill.floodFill(
            pixels = flat,
            width = canvasWidth,
            height = canvasHeight,
            startX = x.toInt().coerceIn(0, canvasWidth - 1),
            startY = y.toInt().coerceIn(0, canvasHeight - 1),
            fillColor = brush.color,
            tolerance = colorDropTolerance,
        )
        if (!filled) return false
        ColorDropFill.writePixelsToLayer(layer, flat, canvasWidth, canvasHeight)
        return true
    }

    fun applyFilterToActiveLayer(filter: LayerFilterType, params: FilterParams): Boolean {
        val layer = getActiveLayer() ?: return false
        if (layer.meta.locked) return false
        pushLayerUndo(layer)
        val flat = layer.flattenToRgba(canvasWidth, canvasHeight)
        val filtered = LayerFilters.apply(flat, canvasWidth, canvasHeight, filter, params)
        ColorDropFill.writePixelsToLayer(layer, filtered, canvasWidth, canvasHeight)
        return true
    }

    private fun smoothPoint(point: StrokePoint): StrokePoint {
        val amount = brush.streamLine.coerceIn(0f, 1f)
        if (amount <= 0f || currentStrokePoints.isEmpty()) return point
        val last = currentStrokePoints.last()
        return StrokePoint(
            x = last.x + (point.x - last.x) * (1f - amount),
            y = last.y + (point.y - last.y) * (1f - amount),
            pressure = last.pressure + (point.pressure - last.pressure) * (1f - amount),
        )
    }

    private fun applyQuickShape(layer: LayerData) {
        if (currentStrokePoints.size < 2) return
        val first = currentStrokePoints.first()
        val last = currentStrokePoints.last()
        val dx = last.x - first.x
        val dy = last.y - first.y
        val length = sqrt(dx * dx + dy * dy)
        if (length < 20f) return

        var maxDeviation = 0f
        for (point in currentStrokePoints) {
            val deviation = perpendicularDistance(point, first, last)
            maxDeviation = max(maxDeviation, deviation)
        }
        if (maxDeviation > length * 0.08f) return

        val snapshot = undoStack.lastOrNull() ?: return
        layer.restoreTiles(snapshot.tiles)
        val linePoints = buildList {
            val steps = max(1, (length / (brush.size * brush.spacing)).toInt())
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                add(
                    StrokePoint(
                        x = first.x + dx * t,
                        y = first.y + dy * t,
                        pressure = first.pressure + (last.pressure - first.pressure) * t,
                    ),
                )
            }
        }
        var prev: StrokePoint? = null
        for (p in linePoints) {
            val previous = prev
            if (previous == null) stamp(layer, p.x, p.y, p.pressure, isEraser = false)
            else interpolateStroke(layer, previous, p, isEraser = false)
            prev = p
        }
        layer.markDirty()
    }

    private fun perpendicularDistance(point: StrokePoint, lineStart: StrokePoint, lineEnd: StrokePoint): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag == 0f) return 0f
        return abs(dy * point.x - dx * point.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / mag
    }

    private fun interpolateStroke(layer: LayerData, from: StrokePoint, to: StrokePoint, isEraser: Boolean) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val distance = sqrt(dx * dx + dy * dy)
        val step = max(brush.size * brush.spacing, 1f)
        val steps = max(1, (distance / step).toInt())
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val pressure = from.pressure + (to.pressure - from.pressure) * t
            stamp(layer, from.x + dx * t, from.y + dy * t, pressure, isEraser)
        }
    }

    private fun interpolateSmudge(layer: LayerData, from: StrokePoint, to: StrokePoint) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val distance = sqrt(dx * dx + dy * dy)
        val step = max(brush.size * brush.spacing, 1f)
        val steps = max(1, (distance / step).toInt())
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val pressure = from.pressure + (to.pressure - from.pressure) * t
            smudgeStamp(layer, from.x + dx * t, from.y + dy * t, pressure)
        }
    }

    private fun stamp(layer: LayerData, x: Float, y: Float, pressure: Float, isEraser: Boolean) {
        symmetryOffsets(x, y).forEach { (sx, sy) ->
            stampAt(layer, sx, sy, pressure, isEraser)
        }
    }

    private fun smudgeStamp(layer: LayerData, x: Float, y: Float, pressure: Float) {
        smudgeColor = sampleColorAt(layer, x, y) ?: smudgeColor
        symmetryOffsets(x, y).forEach { (sx, sy) ->
            smudgeAt(layer, sx, sy, pressure)
        }
    }

    private fun symmetryOffsets(x: Float, y: Float): List<Pair<Float, Float>> {
        return when (symmetryMode) {
            SymmetryMode.OFF -> listOf(x to y)
            SymmetryMode.VERTICAL -> listOf(x to y, canvasWidth - x to y)
            SymmetryMode.HORIZONTAL -> listOf(x to y, x to canvasHeight - y)
            SymmetryMode.QUADRANT -> listOf(
                x to y,
                canvasWidth - x to y,
                x to canvasHeight - y,
                canvasWidth - x to canvasHeight - y,
            )
        }
    }

    private fun stampAt(layer: LayerData, x: Float, y: Float, pressure: Float, isEraser: Boolean) {
        if (x < 0 || y < 0 || x >= canvasWidth || y >= canvasHeight) return
        val radius = brush.size * pressure * 0.5f
        val colMin = floor((x - radius) / DEFAULT_TILE_SIZE).toInt()
        val colMax = floor((x + radius) / DEFAULT_TILE_SIZE).toInt()
        val rowMin = floor((y - radius) / DEFAULT_TILE_SIZE).toInt()
        val rowMax = floor((y + radius) / DEFAULT_TILE_SIZE).toInt()
        val maxCol = (canvasWidth + DEFAULT_TILE_SIZE - 1) / DEFAULT_TILE_SIZE
        val maxRow = (canvasHeight + DEFAULT_TILE_SIZE - 1) / DEFAULT_TILE_SIZE

        for (col in colMin..colMax) {
            for (row in rowMin..rowMax) {
                if (col < 0 || row < 0 || col >= maxCol || row >= maxRow) continue
                val tile = layer.getOrCreateTile(col, row)
                paintTile(tile, col, row, x, y, radius, pressure, isEraser, layer.meta.flags.alphaLock)
                layer.markTileDirty(col, row)
            }
        }
    }

    private fun smudgeAt(layer: LayerData, x: Float, y: Float, pressure: Float) {
        if (x < 0 || y < 0 || x >= canvasWidth || y >= canvasHeight) return
        val radius = brush.size * pressure * 0.5f
        val strength = brush.smudgeStrength * brush.opacity * pressure
        val colMin = floor((x - radius) / DEFAULT_TILE_SIZE).toInt()
        val colMax = floor((x + radius) / DEFAULT_TILE_SIZE).toInt()
        val rowMin = floor((y - radius) / DEFAULT_TILE_SIZE).toInt()
        val rowMax = floor((y + radius) / DEFAULT_TILE_SIZE).toInt()
        val maxCol = (canvasWidth + DEFAULT_TILE_SIZE - 1) / DEFAULT_TILE_SIZE
        val maxRow = (canvasHeight + DEFAULT_TILE_SIZE - 1) / DEFAULT_TILE_SIZE
        val r = (smudgeColor shr 16) and 0xFF
        val g = (smudgeColor shr 8) and 0xFF
        val b = smudgeColor and 0xFF

        for (col in colMin..colMax) {
            for (row in rowMin..rowMax) {
                if (col < 0 || row < 0 || col >= maxCol || row >= maxRow) continue
                val tile = layer.getOrCreateTile(col, row)
                smudgeTile(tile, col, row, x, y, radius, strength, r, g, b)
                layer.markTileDirty(col, row)
            }
        }
    }

    private fun sampleColorAt(layer: LayerData, x: Float, y: Float): Int? {
        val col = floor(x / DEFAULT_TILE_SIZE).toInt()
        val row = floor(y / DEFAULT_TILE_SIZE).toInt()
        val tile = layer.tiles[col to row] ?: return null
        val px = (x.toInt() % DEFAULT_TILE_SIZE).coerceIn(0, DEFAULT_TILE_SIZE - 1)
        val py = (y.toInt() % DEFAULT_TILE_SIZE).coerceIn(0, DEFAULT_TILE_SIZE - 1)
        val idx = (py * DEFAULT_TILE_SIZE + px) * 4
        val a = tile[idx + 3].toInt() and 0xFF
        if (a == 0) return null
        val r = tile[idx].toInt() and 0xFF
        val g = tile[idx + 1].toInt() and 0xFF
        val b = tile[idx + 2].toInt() and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun paintTile(
        tile: ByteArray,
        col: Int,
        row: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        pressure: Float,
        isEraser: Boolean,
        alphaLock: Boolean,
    ) {
        val tileOriginX = col * DEFAULT_TILE_SIZE
        val tileOriginY = row * DEFAULT_TILE_SIZE
        val alphaScale = brush.opacity * pressure
        val hardness = brush.hardness
        val color = brush.color
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        for (py in 0 until DEFAULT_TILE_SIZE) {
            for (px in 0 until DEFAULT_TILE_SIZE) {
                val worldX = tileOriginX + px + 0.5f
                val worldY = tileOriginY + py + 0.5f
                val dx = worldX - centerX
                val dy = worldY - centerY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > radius) continue

                val norm = dist / radius
                val soft = 1f - norm
                val stampAlpha = (soft * soft * (hardness + (1f - hardness) * soft) * alphaScale)
                    .coerceIn(0f, 1f)
                if (stampAlpha <= 0f) continue

                val idx = (py * DEFAULT_TILE_SIZE + px) * 4
                if (isEraser) {
                    val existingA = tile[idx + 3].toInt() and 0xFF
                    val newA = (existingA * (1f - stampAlpha)).toInt().coerceIn(0, 255)
                    tile[idx + 3] = newA.toByte()
                    if (newA == 0) {
                        tile[idx] = 0
                        tile[idx + 1] = 0
                        tile[idx + 2] = 0
                    }
                } else {
                    if (alphaLock) {
                        val existingA = tile[idx + 3].toInt() and 0xFF
                        if (existingA == 0) continue
                        blendPixel(tile, idx, r, g, b, stampAlpha, preserveAlpha = true)
                    } else {
                        blendPixel(tile, idx, r, g, b, stampAlpha)
                    }
                }
            }
        }
    }

    private fun smudgeTile(
        tile: ByteArray,
        col: Int,
        row: Int,
        centerX: Float,
        centerY: Float,
        radius: Float,
        strength: Float,
        r: Int,
        g: Int,
        b: Int,
    ) {
        val tileOriginX = col * DEFAULT_TILE_SIZE
        val tileOriginY = row * DEFAULT_TILE_SIZE
        for (py in 0 until DEFAULT_TILE_SIZE) {
            for (px in 0 until DEFAULT_TILE_SIZE) {
                val worldX = tileOriginX + px + 0.5f
                val worldY = tileOriginY + py + 0.5f
                val dx = worldX - centerX
                val dy = worldY - centerY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > radius) continue
                val norm = 1f - (dist / radius)
                val mix = (norm * strength).coerceIn(0f, 1f)
                val idx = (py * DEFAULT_TILE_SIZE + px) * 4
                val existingA = tile[idx + 3].toInt() and 0xFF
                if (existingA == 0) continue
                blendPixel(tile, idx, r, g, b, mix * 0.6f)
            }
        }
    }

    private fun blendPixel(
        tile: ByteArray,
        idx: Int,
        r: Int,
        g: Int,
        b: Int,
        srcAlpha: Float,
        preserveAlpha: Boolean = false,
    ) {
        val dstA = (tile[idx + 3].toInt() and 0xFF) / 255f
        val dstR = (tile[idx].toInt() and 0xFF) / 255f
        val dstG = (tile[idx + 1].toInt() and 0xFF) / 255f
        val dstB = (tile[idx + 2].toInt() and 0xFF) / 255f

        val outA = if (preserveAlpha) dstA else srcAlpha + dstA * (1f - srcAlpha)
        if (outA <= 0f) return

        val outR = (r / 255f * srcAlpha + dstR * dstA * (1f - srcAlpha)) / outA
        val outG = (g / 255f * srcAlpha + dstG * dstA * (1f - srcAlpha)) / outA
        val outB = (b / 255f * srcAlpha + dstB * dstA * (1f - srcAlpha)) / outA

        tile[idx] = (outR * 255).toInt().coerceIn(0, 255).toByte()
        tile[idx + 1] = (outG * 255).toInt().coerceIn(0, 255).toByte()
        tile[idx + 2] = (outB * 255).toInt().coerceIn(0, 255).toByte()
        if (!preserveAlpha) {
            tile[idx + 3] = (outA * 255).toInt().coerceIn(0, 255).toByte()
        }
    }
}

data class UndoSnapshot(
    val layerId: String,
    val tiles: Map<Pair<Int, Int>, ByteArray>,
)
