package com.rws.learningproject01.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import com.rws.learningproject01.core.model.BlendMode
import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

class GlCanvasRenderer(
    private val engine: ArtEngine,
    private val onReady: () -> Unit = {},
) : GLSurfaceView.Renderer {

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private var program = 0
    private var posHandle = 0
    private var texHandle = 0
    private var mvpHandle = 0
    private var opacityHandle = 0
    private var quadBuffer: FloatBuffer = floatBuffer(
        floatArrayOf(
            0f, 0f, 0f, 0f,
            1f, 0f, 1f, 0f,
            0f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
        ),
    )
    private val requestRender = AtomicBoolean(true)
    var canvasWidth = 1
    var canvasHeight = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f, 0.12f, 0.12f, 1f)
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        posHandle = GLES30.glGetAttribLocation(program, "aPosition")
        texHandle = GLES30.glGetAttribLocation(program, "aTexCoord")
        mvpHandle = GLES30.glGetUniformLocation(program, "uMvp")
        opacityHandle = GLES30.glGetUniformLocation(program, "uOpacity")
        val texLoc = GLES30.glGetUniformLocation(program, "uTexture")
        GLES30.glUseProgram(program)
        GLES30.glUniform1i(texLoc, 0)
        onReady()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        Matrix.setIdentityM(viewMatrix, 0)
        applyViewTransform(width, height)
    }

    private data class LayerDrawPlan(
        val layer: LayerData,
        val opacity: Float,
    )

    private data class GhostDrawPlan(
        val layerPlans: List<LayerDrawPlan>,
    )

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)

        val (ghostPlans, layerPlans) = engine.withStateLock { captureDrawPlans() }

        ghostPlans.forEach { drawGhostPlan(it) }
        layerPlans.forEach { plan ->
            ensureLayerTexture(plan.layer)
            uploadDirtyTiles(plan.layer)
            drawLayer(plan.layer, plan.opacity)
        }
        requestRender.set(false)
    }

    private fun captureDrawPlans(): Pair<List<GhostDrawPlan>, List<LayerDrawPlan>> {
        val ghosts = if (engine.animation.enabled) {
            engine.animation.onionFrameIndices().map { (frameIndex, opacity) ->
                val overrides = engine.animation.frameOverrides(frameIndex)
                GhostDrawPlan(
                    engine.layers.mapNotNull { layer ->
                        val override = overrides[layer.id]
                        val visible = override?.first ?: layer.meta.visible
                        val layerOpacity = (override?.second ?: layer.meta.opacity) * opacity
                        if (!visible || layerOpacity <= 0f) return@mapNotNull null
                        LayerDrawPlan(layer, layerOpacity)
                    },
                )
            }
        } else {
            emptyList()
        }

        val frameOverrides = if (engine.animation.enabled) {
            engine.animation.frameOverrides(engine.animation.currentFrameIndex)
        } else {
            emptyMap()
        }
        val mainLayers = engine.layers.mapNotNull { layer ->
            val override = frameOverrides[layer.id]
            val visible = override?.first ?: layer.meta.visible
            val opacity = override?.second ?: layer.meta.opacity
            if (!visible || opacity <= 0f) return@mapNotNull null
            LayerDrawPlan(layer, opacity)
        }
        return ghosts to mainLayers
    }

    private fun drawGhostPlan(plan: GhostDrawPlan) {
        plan.layerPlans.forEach { layerPlan ->
            ensureLayerTexture(layerPlan.layer)
            uploadDirtyTiles(layerPlan.layer)
            drawLayer(layerPlan.layer, layerPlan.opacity)
        }
    }

    fun invalidate() {
        requestRender.set(true)
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

    private fun applyViewTransform(viewWidth: Int, viewHeight: Int) {
        Matrix.setIdentityM(viewMatrix, 0)
        val canvasW = canvasWidth.toFloat()
        val canvasH = canvasHeight.toFloat()
        val fitScale = min(viewWidth / canvasW, viewHeight / canvasH) * 0.9f * engine.viewportZoom
        val offsetX = (viewWidth - canvasW * fitScale) / 2f + engine.viewportPanX
        val offsetY = (viewHeight - canvasH * fitScale) / 2f + engine.viewportPanY
        Matrix.translateM(viewMatrix, 0, offsetX, offsetY, 0f)
        Matrix.scaleM(viewMatrix, 0, fitScale, fitScale, 1f)
    }

    private fun ensureLayerTexture(layer: LayerData) {
        if (layer.textureId != 0) return
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        layer.textureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val empty = ByteBuffer.allocateDirect(canvasWidth * canvasHeight * 4)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            canvasWidth, canvasHeight, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, empty,
        )
        layer.dirtyGpu = true
    }

    private fun uploadDirtyTiles(layer: LayerData) {
        if (!layer.dirtyGpu) return
        val uploads = engine.withStateLock {
            layer.gpuUploadCoords().mapNotNull { coord ->
                layer.tiles[coord]?.let { coord to it }
            }
        }
        if (uploads.isEmpty()) {
            engine.withStateLock { layer.clearGpuDirty() }
            return
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.textureId)
        uploads.forEach { (coord, tile) ->
            val (col, row) = coord
            val buffer = ByteBuffer.wrap(tile).order(ByteOrder.nativeOrder())
            GLES30.glTexSubImage2D(
                GLES30.GL_TEXTURE_2D, 0,
                col * DEFAULT_TILE_SIZE, row * DEFAULT_TILE_SIZE,
                DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer,
            )
        }
        engine.withStateLock { layer.clearGpuDirty() }
    }

    private fun drawLayer(layer: LayerData, opacityOverride: Float? = null) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.textureId)
        GLES30.glEnable(GLES30.GL_BLEND)
        applyBlendMode(BlendMode.fromId(layer.meta.blendMode))
        val opacity = opacityOverride ?: layer.meta.opacity
        GLES30.glUniform1f(opacityHandle, opacity.coerceIn(0f, 1f))

        val mvp = FloatArray(16)
        Matrix.setIdentityM(mvp, 0)
        Matrix.scaleM(mvp, 0, canvasWidth.toFloat(), canvasHeight.toFloat(), 1f)
        Matrix.multiplyMM(mvp, 0, viewMatrix, 0, mvp, 0)
        Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, mvp, 0)

        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
        GLES30.glEnableVertexAttribArray(posHandle)
        GLES30.glEnableVertexAttribArray(texHandle)
        quadBuffer.position(0)
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 16, quadBuffer)
        quadBuffer.position(2)
        GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, 16, quadBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(posHandle)
        GLES30.glDisableVertexAttribArray(texHandle)
    }

    private fun applyBlendMode(mode: BlendMode) {
        when (mode) {
            BlendMode.NORMAL -> GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            BlendMode.MULTIPLY -> GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            BlendMode.SCREEN -> GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_COLOR)
            BlendMode.OVERLAY -> GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_SRC_ALPHA)
            BlendMode.DARKEN -> GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            BlendMode.LIGHTEN -> GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            BlendMode.COLOR_DODGE -> GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_ONE)
            BlendMode.COLOR_BURN -> GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_SRC_ALPHA)
            BlendMode.SOFT_LIGHT, BlendMode.HARD_LIGHT -> GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            BlendMode.DIFFERENCE -> GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            BlendMode.EXCLUSION -> GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        }
    }

    private fun buildProgram(vertex: String, fragment: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertex)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragment)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }

    private fun floatBuffer(array: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(array)
                position(0)
            }

    companion object {
        private const val VERTEX_SHADER = """
            #version 300 es
            in vec2 aPosition;
            in vec2 aTexCoord;
            uniform mat4 uMvp;
            out vec2 vTexCoord;
            void main() {
                gl_Position = uMvp * vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            uniform sampler2D uTexture;
            uniform float uOpacity;
            out vec4 fragColor;
            void main() {
                vec4 c = texture(uTexture, vTexCoord);
                fragColor = vec4(c.rgb, c.a * uOpacity);
            }
        """
    }
}

class DrawingSurface(
    context: android.content.Context,
    val engine: ArtEngine,
) : GLSurfaceView(context) {

    var renderer: GlCanvasRenderer
    var onStrokeFinished: (() -> Unit)? = null
    var onColorPicked: ((Int) -> Unit)? = null
    var onTextPlaced: ((Float, Float) -> Unit)? = null
    var inputEnabled: Boolean = true

    private val strokeExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    init {
        setEGLContextClientVersion(3)
        renderer = GlCanvasRenderer(engine) { }
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onDetachedFromWindow() {
        strokeExecutor.shutdownNow()
        super.onDetachedFromWindow()
    }

    private val strokeMovePending = AtomicBoolean(false)

    private fun runStrokeWork(notifyFinished: Boolean = false, block: () -> Unit) {
        strokeExecutor.execute {
            engine.withStateLock(block)
            post {
                requestCanvasRender()
                if (notifyFinished) onStrokeFinished?.invoke()
            }
        }
    }

    private fun runStrokeMoveWork(block: () -> Unit) {
        if (!strokeMovePending.compareAndSet(false, true)) return
        strokeExecutor.execute {
            try {
                engine.withStateLock(block)
            } finally {
                strokeMovePending.set(false)
            }
            post { requestCanvasRender() }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled) return false
        val viewW = width.coerceAtLeast(1)
        val viewH = height.coerceAtLeast(1)
        val tool = engine.activeTool
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (tool == DrawingTool.TEXT) return true
                if (tool == DrawingTool.COLOR_DROP || tool == DrawingTool.COLOR_PICKER) {
                    return true
                }
                val (x, y) = renderer.screenToCanvas(event.x, event.y, viewW, viewH)
                val point = StrokePoint(x, y, event.pressure.coerceIn(0.1f, 1f))
                runStrokeWork {
                    engine.beginStroke()
                    engine.addStrokePoint(point)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (tool == DrawingTool.TEXT) return true
                if (tool == DrawingTool.COLOR_DROP || tool == DrawingTool.COLOR_PICKER) {
                    return true
                }
                val points = ArrayList<StrokePoint>(event.historySize + 1)
                val historySize = event.historySize
                if (historySize > 0) {
                    val maxHistory = minOf(historySize, 4)
                    val start = historySize - maxHistory
                    for (i in start until historySize) {
                        val (x, y) = renderer.screenToCanvas(
                            event.getHistoricalX(i),
                            event.getHistoricalY(i),
                            viewW,
                            viewH,
                        )
                        points.add(
                            StrokePoint(x, y, event.getHistoricalPressure(i).coerceIn(0.1f, 1f)),
                        )
                    }
                }
                val (x, y) = renderer.screenToCanvas(event.x, event.y, viewW, viewH)
                points.add(StrokePoint(x, y, event.pressure.coerceIn(0.1f, 1f)))
                runStrokeMoveWork {
                    points.forEach { engine.addStrokePoint(it) }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val (x, y) = renderer.screenToCanvas(event.x, event.y, viewW, viewH)
                when (tool) {
                    DrawingTool.TEXT -> onTextPlaced?.invoke(x, y)
                    DrawingTool.COLOR_DROP -> {
                        runStrokeWork(notifyFinished = true) {
                            engine.performColorDrop(x, y)
                        }
                    }
                    DrawingTool.COLOR_PICKER -> {
                        strokeExecutor.execute {
                            val color = engine.withStateLock { engine.pickColorAt(x, y) }
                            post {
                                color?.let { onColorPicked?.invoke(it) }
                                requestCanvasRender()
                            }
                        }
                    }
                    else -> {
                        val point = StrokePoint(x, y, event.pressure.coerceIn(0.1f, 1f))
                        runStrokeWork(notifyFinished = true) {
                            engine.addStrokePoint(point)
                            engine.endStroke()
                        }
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun requestCanvasRender() {
        renderer.invalidate()
        requestRender()
    }
}

fun generateThumbnail(
    layers: List<LayerData>,
    width: Int,
    height: Int,
    layerOrder: List<String>,
): Bitmap {
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFF1E1E1E.toInt())
    val scale = min(512f / width, 512f / height)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    layerOrder.forEach { layerId ->
        val layer = layers.find { it.id == layerId } ?: return@forEach
        if (!layer.meta.visible) return@forEach
        val flat = layer.flattenToRgba(width, height)
        val layerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        layerBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(flat))
        paint.alpha = (layer.meta.opacity * 255).toInt().coerceIn(0, 255)
        val dest = android.graphics.RectF(0f, 0f, width * scale, height * scale)
        canvas.drawBitmap(layerBitmap, null, dest, paint)
        layerBitmap.recycle()
    }
    return bitmap
}
