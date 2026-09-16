package com.rws.learningproject01.viewmodel

import com.rws.learningproject01.core.model.AnimationSummary
import com.rws.learningproject01.core.model.BlendMode
import com.rws.learningproject01.core.model.EditorState
import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.core.model.ViewportState
import com.rws.learningproject01.core.storage.CelFrameStore
import com.rws.learningproject01.core.storage.ExportService
import com.rws.learningproject01.core.storage.OpenProject
import com.rws.learningproject01.core.storage.ProjectRepository
import com.rws.learningproject01.engine.ArtEngine
import com.rws.learningproject01.engine.BrushSettings
import com.rws.learningproject01.engine.CanvasCompositor
import com.rws.learningproject01.engine.ColorHarmony
import com.rws.learningproject01.engine.ColorUtils
import com.rws.learningproject01.engine.DrawingTool
import com.rws.learningproject01.engine.FilterParams
import com.rws.learningproject01.engine.HarmonyType
import com.rws.learningproject01.engine.LayerFilterType
import com.rws.learningproject01.engine.SymmetryMode
import com.rws.learningproject01.engine.TimelapseReplayer
import com.rws.learningproject01.platform.ImageBitmap
import com.rws.learningproject01.platform.PlatformContext
import com.rws.learningproject01.platform.ThumbnailGenerator
import com.rws.learningproject01.platform.ioDispatcher
import com.rws.learningproject01.platform.platformCacheDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath

private val DEFAULT_SAVED_PALETTE = listOf(
    0xFF000000.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFF44336.toInt(),
    0xFFFF9800.toInt(),
    0xFFFFEB3B.toInt(),
    0xFF4CAF50.toInt(),
    0xFF2196F3.toInt(),
    0xFF9C27B0.toInt(),
)

data class LayerUiModel(
    val id: String,
    val name: String,
    val visible: Boolean,
    val locked: Boolean,
    val opacity: Float,
    val blendMode: String,
    val clipToBelow: Boolean,
    val alphaLock: Boolean,
    val layerType: String = "raster",
)

data class EditorUiState(
    val isLoading: Boolean = true,
    val isEngineReady: Boolean = false,
    val title: String = "Untitled",
    val projectId: String = "",
    val canvasWidth: Int = 2048,
    val canvasHeight: Int = 2048,
    val activeTool: DrawingTool = DrawingTool.BRUSH,
    val brushSize: Float = 24f,
    val brushOpacity: Float = 1f,
    val brushHardness: Float = 0.85f,
    val brushSpacing: Float = 0.15f,
    val streamLine: Float = 0.3f,
    val smudgeStrength: Float = 0.5f,
    val brushColor: Int = 0xFF000000.toInt(),
    val symmetryMode: SymmetryMode = SymmetryMode.OFF,
    val quickShapeEnabled: Boolean = true,
    val activeLayerIndex: Int = 0,
    val layers: List<LayerUiModel> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showBrushStudio: Boolean = false,
    val showLayerPanel: Boolean = false,
    val showColorPanel: Boolean = false,
    val showFiltersPanel: Boolean = false,
    val colorHue: Float = 0f,
    val colorSaturation: Float = 0f,
    val colorValue: Float = 0f,
    val harmonyType: HarmonyType = HarmonyType.COMPLEMENTARY,
    val harmonyColors: List<Int> = emptyList(),
    val savedPalette: List<Int> = DEFAULT_SAVED_PALETTE,
    val colorDropTolerance: Int = 32,
    val selectedFilter: LayerFilterType = LayerFilterType.HSB,
    val filterParams: FilterParams = FilterParams(),
    val error: String? = null,
    val saveMessage: String? = null,
    val showExportPanel: Boolean = false,
    val showAnimationPanel: Boolean = false,
    val showTextEditor: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.PNG,
    val exportTransparentBackground: Boolean = true,
    val exportAnimation: Boolean = false,
    val exportTimelapse4K: Boolean = false,
    val isExporting: Boolean = false,
    val animationEnabled: Boolean = false,
    val animationFrameCount: Int = 1,
    val currentAnimationFrame: Int = 0,
    val animationPlaying: Boolean = false,
    val animationFps: Int = 12,
    val onionSkinBefore: Float = 1f,
    val onionSkinAfter: Float = 1f,
    val onionSkinOpacity: Float = 0.3f,
    val textLayerContent: String = "Text",
    val textLayerFontSize: Float = 48f,
    val editingTextLayer: Boolean = false,
    val pendingTextX: Float = 100f,
    val pendingTextY: Float = 100f,
)

class EditorViewModel(
    private val repository: ProjectRepository,
    private val platformContext: PlatformContext,
    private val scope: CoroutineScope,
    private val projectId: String,
) {

    private val _uiState = MutableStateFlow(EditorUiState(projectId = projectId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var engine: ArtEngine? = null
    private var openProject: OpenProject? = null
    private var autoSaveJob: Job? = null
    private var playbackJob: Job? = null

    fun engineOrNull(): ArtEngine? = engine

    init {
        loadProject()
    }

    private inline fun withEngine(block: (ArtEngine) -> Unit) {
        engine?.let(block)
    }

    private fun loadProject() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, isEngineReady = false, error = null) }
            runCatching {
                val project = repository.openProject(projectId)
                    ?: error("Project not found. Go back and tap + to create a new canvas.")
                openProject = project

                val width = project.manifest.canvas.width
                val height = project.manifest.canvas.height

                val loadedEngine = withContext(Dispatchers.Default) {
                    val eng = ArtEngine(width, height)
                    project.manifest.layerOrder.forEach { layerId ->
                        val meta = project.tileStore.readLayerMeta(layerId) ?: return@forEach
                        val tiles = repository.loadAllTiles(project, layerId)
                        eng.loadLayer(meta, tiles)
                    }
                    if (eng.layers.isEmpty()) {
                        eng.addLayer("Layer 1")
                    }
                    eng
                }

                val editorState = repository.readEditorState(project)
                val activeIndex = project.manifest.layerOrder
                    .indexOf(editorState.activeLayerId)
                    .coerceAtLeast(0)
                loadedEngine.setActiveLayer(activeIndex.coerceAtMost(loadedEngine.layers.lastIndex))

                engine = loadedEngine

                val timeline = repository.readAnimationTimeline(project)
                loadedEngine.animation.timeline = timeline
                loadedEngine.animation.enabled = project.manifest.animation.enabled
                loadedEngine.animation.fps = project.manifest.animation.fps
                loadedEngine.animation.ensureDefaultFrame(loadedEngine.layerOrder())
                loadedEngine.animation.setOnionSkin(timeline.onionSkin)

                val savedPalette = repository.readProjectPalette(project)
                val timelapse = repository.readTimelapse(project)
                timelapse?.let { loadedEngine.timelapse.loadLog(it) }

                syncUiFromEngine(project.manifest.title)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEngineReady = true,
                        canvasWidth = width,
                        canvasHeight = height,
                        savedPalette = if (savedPalette.isNotEmpty()) savedPalette else it.savedPalette,
                        animationEnabled = loadedEngine.animation.enabled,
                        animationFrameCount = loadedEngine.animation.frameCount,
                        currentAnimationFrame = loadedEngine.animation.currentFrameIndex,
                        animationFps = loadedEngine.animation.fps,
                        onionSkinBefore = timeline.onionSkin.before.toFloat(),
                        onionSkinAfter = timeline.onionSkin.after.toFloat(),
                        onionSkinOpacity = timeline.onionSkin.opacity,
                    )
                }
            }.onFailure { e ->
                engine = null
                openProject = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEngineReady = false,
                        error = e.message ?: "Failed to open project",
                    )
                }
            }
        }
    }

    fun onStrokeFinished() {
        scheduleAutoSave()
        scope.launch(Dispatchers.Default) {
            syncUiFromEngine()
        }
    }

    fun undo() {
        val eng = engine ?: return
        scope.launch(Dispatchers.Default) {
            val changed = eng.withStateLock { eng.undo() }
            if (changed) {
                syncUiFromEngine()
                scheduleAutoSave()
            }
        }
    }

    fun redo() {
        val eng = engine ?: return
        scope.launch(Dispatchers.Default) {
            val changed = eng.withStateLock { eng.redo() }
            if (changed) {
                syncUiFromEngine()
                scheduleAutoSave()
            }
        }
    }

    fun setActiveTool(tool: DrawingTool) {
        val eng = engine ?: return
        eng.activeTool = tool
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun setBrushSize(size: Float) {
        _uiState.update { it.copy(brushSize = size) }
        applyBrushToEngine()
    }

    fun setBrushOpacity(opacity: Float) {
        _uiState.update { it.copy(brushOpacity = opacity) }
        applyBrushToEngine()
    }

    fun setBrushHardness(hardness: Float) {
        _uiState.update { it.copy(brushHardness = hardness) }
        applyBrushToEngine()
    }

    fun setBrushSpacing(spacing: Float) {
        _uiState.update { it.copy(brushSpacing = spacing) }
        applyBrushToEngine()
    }

    fun setStreamLine(value: Float) {
        _uiState.update { it.copy(streamLine = value) }
        applyBrushToEngine()
    }

    fun setSmudgeStrength(value: Float) {
        _uiState.update { it.copy(smudgeStrength = value) }
        applyBrushToEngine()
    }

    fun setBrushColor(color: Int) {
        selectColor(color)
        _uiState.update { it.copy(activeTool = DrawingTool.BRUSH) }
        withEngine { it.activeTool = DrawingTool.BRUSH }
    }

    fun setSymmetryMode(mode: SymmetryMode) {
        val eng = engine ?: return
        eng.symmetryMode = mode
        _uiState.update { it.copy(symmetryMode = mode) }
    }

    fun toggleQuickShape() {
        val eng = engine ?: return
        eng.quickShapeEnabled = !eng.quickShapeEnabled
        _uiState.update { it.copy(quickShapeEnabled = eng.quickShapeEnabled) }
    }

    fun toggleBrushStudio() {
        _uiState.update {
            it.copy(showBrushStudio = !it.showBrushStudio, showLayerPanel = false, showColorPanel = false, showFiltersPanel = false, showExportPanel = false, showAnimationPanel = false, showTextEditor = false)
        }
    }

    fun toggleLayerPanel() {
        _uiState.update {
            it.copy(showLayerPanel = !it.showLayerPanel, showBrushStudio = false, showColorPanel = false, showFiltersPanel = false, showExportPanel = false, showAnimationPanel = false, showTextEditor = false)
        }
    }

    fun toggleColorPanel() {
        _uiState.update {
            it.copy(showColorPanel = !it.showColorPanel, showBrushStudio = false, showLayerPanel = false, showFiltersPanel = false, showExportPanel = false, showAnimationPanel = false, showTextEditor = false)
        }
    }

    fun toggleFiltersPanel() {
        _uiState.update {
            it.copy(showFiltersPanel = !it.showFiltersPanel, showBrushStudio = false, showLayerPanel = false, showColorPanel = false, showExportPanel = false, showAnimationPanel = false, showTextEditor = false)
        }
    }

    fun setColorFromHsv(hue: Float? = null, saturation: Float? = null, value: Float? = null) {
        val state = _uiState.value
        val h = hue ?: state.colorHue
        val s = saturation ?: state.colorSaturation
        val v = value ?: state.colorValue
        val color = ColorUtils.hsvToArgb(h, s, v)
        _uiState.update {
            it.copy(
                colorHue = h,
                colorSaturation = s,
                colorValue = v,
                brushColor = color,
                harmonyColors = ColorHarmony.colors(color, it.harmonyType),
            )
        }
        applyBrushToEngine()
    }

    fun setHarmonyType(type: HarmonyType) {
        _uiState.update {
            it.copy(
                harmonyType = type,
                harmonyColors = ColorHarmony.colors(it.brushColor, type),
            )
        }
    }

    fun selectColor(color: Int) {
        val hsv = ColorUtils.argbToHsv(color)
        _uiState.update {
            it.copy(
                brushColor = color,
                colorHue = hsv[0],
                colorSaturation = hsv[1],
                colorValue = hsv[2],
                harmonyColors = ColorHarmony.colors(color, it.harmonyType),
            )
        }
        applyBrushToEngine()
    }

    fun onColorPicked(color: Int) {
        selectColor(color)
        setActiveTool(DrawingTool.BRUSH)
    }

    fun addColorToPalette() {
        val color = _uiState.value.brushColor
        _uiState.update {
            val updated = (it.savedPalette + color).distinct().takeLast(16)
            it.copy(savedPalette = updated)
        }
    }

    fun setColorDropTolerance(value: Float) {
        val tolerance = value.toInt().coerceIn(0, 128)
        withEngine { it.colorDropTolerance = tolerance }
        _uiState.update { it.copy(colorDropTolerance = tolerance) }
    }

    fun setSelectedFilter(filter: LayerFilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setFilterParams(params: FilterParams) {
        _uiState.update { it.copy(filterParams = params) }
    }

    fun applySelectedFilter() {
        val eng = engine ?: return
        val state = _uiState.value
        scope.launch(Dispatchers.Default) {
            val applied = eng.withStateLock {
                eng.applyFilterToActiveLayer(state.selectedFilter, state.filterParams)
            }
            if (applied) {
                syncUiFromEngine()
                scheduleAutoSave()
                _uiState.update { it.copy(saveMessage = "Filter applied") }
            }
        }
    }

    fun addLayer() {
        val eng = engine ?: return
        eng.addLayer("Layer ${eng.layers.size + 1}")
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun selectLayer(index: Int) {
        val eng = engine ?: return
        eng.setActiveLayer(index)
        syncUiFromEngine()
    }

    fun deleteLayer(index: Int) {
        val eng = engine ?: return
        if (eng.deleteLayer(index)) {
            syncUiFromEngine()
            scheduleAutoSave()
        }
    }

    fun moveLayerUp(index: Int) {
        val eng = engine ?: return
        if (index > 0 && eng.moveLayer(index, index - 1)) {
            syncUiFromEngine()
            scheduleAutoSave()
        }
    }

    fun moveLayerDown(index: Int) {
        val eng = engine ?: return
        if (index < eng.layers.lastIndex && eng.moveLayer(index, index + 1)) {
            syncUiFromEngine()
            scheduleAutoSave()
        }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        val eng = engine ?: return
        eng.setLayerOpacity(index, opacity)
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun setLayerBlendMode(index: Int, blendMode: BlendMode) {
        val eng = engine ?: return
        eng.setLayerBlendMode(index, blendMode)
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun toggleLayerVisibility(index: Int) {
        val eng = engine ?: return
        eng.toggleLayerVisibility(index)
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun toggleLayerLock(index: Int) {
        val eng = engine ?: return
        eng.toggleLayerLock(index)
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun toggleLayerClipMask(index: Int) {
        val eng = engine ?: return
        eng.toggleLayerClipMask(index)
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun toggleLayerAlphaLock(index: Int) {
        val eng = engine ?: return
        val layer = eng.layers.getOrNull(index) ?: return
        val flags = layer.meta.flags
        layer.meta = layer.meta.copy(flags = flags.copy(alphaLock = !flags.alphaLock))
        syncUiFromEngine()
        scheduleAutoSave()
    }

    fun saveNow() {
        scope.launch(ioDispatcher) {
            runCatching { persistProject() }
                .onSuccess { _uiState.update { it.copy(saveMessage = "Saved") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Save failed") } }
        }
    }

    fun startExport(onComplete: (String?) -> Unit) {
        if (_uiState.value.isExporting) return
        scope.launch(ioDispatcher) {
            _uiState.update { it.copy(isExporting = true, error = null) }
            val path = runCatching { exportArtwork() }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Export failed") }
                }
                .getOrNull()
            _uiState.update { it.copy(isExporting = false) }
            if (path != null) {
                _uiState.update { it.copy(saveMessage = "Export ready") }
            }
            onComplete(path)
        }
    }

    private suspend fun exportArtwork(): String? = withContext(Dispatchers.Default) {
        val project = openProject ?: return@withContext null
        val eng = engine ?: return@withContext null
        val state = _uiState.value
        val format = state.exportFormat
        val exportDir = platformCacheDir(platformContext).toPath().resolve("exports")
        val baseName = project.manifest.title.replace(" ", "_")
        val output = exportDir.resolve("$baseName.${format.extension}")

        withContext(ioDispatcher) { persistProject() }
        val width = project.manifest.canvas.width
        val height = project.manifest.canvas.height
        val layerOrder = eng.withStateLock { eng.layerOrder() }

        if (state.exportTimelapse4K) {
            val log = eng.withStateLock { eng.timelapse.toLog() }
            val frameCount = TimelapseReplayer.replayFrameCount(log, state.animationFps).coerceIn(1, 30)
            val frames = ArrayList<ImageBitmap>(frameCount)
            try {
                repeat(frameCount) {
                    frames.add(
                        eng.withStateLock {
                            CanvasCompositor.compositeToBitmap(
                                eng.layers,
                                layerOrder,
                                width,
                                height,
                                includeBackground = !state.exportTransparentBackground,
                            )
                        },
                    )
                }
                withContext(ioDispatcher) {
                    ExportService.exportAnimationFrames(
                        frames,
                        ExportFormat.HEVC,
                        output,
                        state.animationFps,
                        force4K = true,
                    )
                }
            } finally {
                frames.forEach { it.recycle() }
            }
        } else if (state.exportAnimation && eng.withStateLock { eng.animation.enabled }) {
            val frameCount = eng.withStateLock { eng.animation.frameCount.coerceAtMost(60) }
            val frames = ArrayList<ImageBitmap>(frameCount)
            try {
                repeat(frameCount) { frameIndex ->
                    val overrides = eng.withStateLock { eng.animation.frameOverrides(frameIndex) }
                    frames.add(
                        eng.withStateLock {
                            CanvasCompositor.compositeToBitmap(
                                eng.layers,
                                layerOrder,
                                width,
                                height,
                                includeBackground = !state.exportTransparentBackground,
                                frameOverrides = overrides,
                            )
                        },
                    )
                }
                withContext(ioDispatcher) {
                    ExportService.exportAnimationFrames(frames, format, output, state.animationFps)
                }
            } finally {
                frames.forEach { it.recycle() }
            }
        } else {
            val bitmap = eng.withStateLock {
                CanvasCompositor.compositeToBitmap(
                    eng.layers,
                    layerOrder,
                    width,
                    height,
                    includeBackground = !state.exportTransparentBackground,
                    frameOverrides = if (eng.animation.enabled) {
                        eng.animation.frameOverrides(eng.animation.currentFrameIndex)
                    } else {
                        emptyMap()
                    },
                )
            }
            try {
                withContext(ioDispatcher) {
                    ExportService.exportBitmap(bitmap, format, output)
                }
            } finally {
                bitmap.recycle()
            }
        }

        output.toString()
    }

    suspend fun exportPngAndGetPath(): String? {
        _uiState.update { it.copy(exportFormat = ExportFormat.PNG) }
        return exportArtwork()
    }

    fun onPause() {
        scope.launch(ioDispatcher) {
            runCatching { persistProject() }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch(ioDispatcher) {
            delay(2000)
            runCatching { persistProject() }
        }
    }

    private suspend fun persistProject() {
        val project = openProject ?: return
        val eng = engine ?: return

        val dirtyTiles = withContext(Dispatchers.Default) {
            eng.withStateLock { eng.collectDirtyTiles() }
        }
        dirtyTiles.forEach { (layerId, coords) ->
            coords.forEach { (col, row) ->
                val tile = withContext(Dispatchers.Default) {
                    eng.withStateLock {
                        val layer = eng.layers.find { it.id == layerId } ?: return@withStateLock null
                        layer.tiles[col to row]
                    }
                } ?: return@forEach
                repository.saveTile(project, layerId, col, row, tile)
            }
        }

        val layerOrder = withContext(Dispatchers.Default) {
            eng.withStateLock { eng.layers.map { it.id } }
        }
        project.manifest = project.manifest.copy(layerOrder = layerOrder)

        val state = _uiState.value
        val editorState = EditorState(
            activeLayerId = withContext(Dispatchers.Default) {
                eng.withStateLock { eng.getActiveLayer()?.id }
            },
            activeBrushId = state.activeTool.name,
            tool = state.activeTool.name.lowercase(),
            viewport = ViewportState(
                panX = withContext(Dispatchers.Default) { eng.withStateLock { eng.viewportPanX } },
                panY = withContext(Dispatchers.Default) { eng.withStateLock { eng.viewportPanY } },
                zoom = withContext(Dispatchers.Default) { eng.withStateLock { eng.viewportZoom } },
            ),
        )

        repository.saveProject(
            openProject = project,
            layers = withContext(Dispatchers.Default) { eng.withStateLock { eng.layerMetas() } },
            dirtyTiles = dirtyTiles,
            editorState = editorState,
        )

        val anim = eng.animation
        repository.saveAnimationTimeline(
            project,
            anim.timeline,
            AnimationSummary(
                enabled = anim.enabled,
                fps = anim.fps,
                frameCount = anim.frameCount,
            ),
        )
        repository.saveProjectPalette(project, _uiState.value.savedPalette)
        repository.saveTimelapse(project, withContext(Dispatchers.Default) { eng.withStateLock { eng.timelapse.toLog() } })

        val thumbFile = project.directory.resolve("thumb.webp")
        val thumb = withContext(Dispatchers.Default) {
            eng.withStateLock {
                CanvasCompositor.compositeToBitmap(
                    eng.layers,
                    layerOrder,
                    project.manifest.canvas.width.coerceAtMost(512),
                    project.manifest.canvas.height.coerceAtMost(512),
                    includeBackground = true,
                )
            }
        }
        withContext(ioDispatcher) {
            ThumbnailGenerator.generatePlaceholder(
                project.manifest.canvas.width,
                project.manifest.canvas.height,
                thumbFile,
            )
            thumb.recycle()
        }
    }

    private fun syncUiFromEngine(title: String? = null) {
        val eng = engine ?: return
        val brushColor = _uiState.value.brushColor
        val hsv = ColorUtils.argbToHsv(brushColor)
        val snapshot = eng.withStateLock {
            Triple(
                eng.activeLayerIndex,
                eng.canUndo() to eng.canRedo(),
                eng.layers.map { layer ->
                    LayerUiModel(
                        id = layer.id,
                        name = layer.meta.name,
                        visible = layer.meta.visible,
                        locked = layer.meta.locked,
                        opacity = layer.meta.opacity,
                        blendMode = layer.meta.blendMode,
                        clipToBelow = layer.meta.clipToBelow,
                        alphaLock = layer.meta.flags.alphaLock,
                        layerType = layer.meta.type,
                    )
                },
            )
        }
        _uiState.update {
            it.copy(
                title = title ?: it.title,
                activeLayerIndex = snapshot.first,
                activeTool = eng.activeTool,
                symmetryMode = eng.symmetryMode,
                quickShapeEnabled = eng.quickShapeEnabled,
                canUndo = snapshot.second.first,
                canRedo = snapshot.second.second,
                colorHue = hsv[0],
                colorSaturation = hsv[1],
                colorValue = hsv[2],
                harmonyColors = ColorHarmony.colors(brushColor, it.harmonyType),
                colorDropTolerance = eng.colorDropTolerance,
                layers = snapshot.third,
            )
        }
        applyBrushToEngine()
    }

    private fun applyBrushToEngine() {
        val eng = engine ?: return
        val state = _uiState.value
        eng.updateBrush(
            BrushSettings(
                size = state.brushSize,
                opacity = state.brushOpacity,
                color = state.brushColor,
                hardness = state.brushHardness,
                spacing = state.brushSpacing,
                streamLine = state.streamLine,
                smudgeStrength = state.smudgeStrength,
            ),
        )
    }

    fun toggleExportPanel() {
        _uiState.update {
            it.copy(
                showExportPanel = !it.showExportPanel,
                showBrushStudio = false,
                showLayerPanel = false,
                showColorPanel = false,
                showFiltersPanel = false,
                showAnimationPanel = false,
                showTextEditor = false,
            )
        }
    }

    fun toggleAnimationPanel() {
        _uiState.update {
            it.copy(
                showAnimationPanel = !it.showAnimationPanel,
                showBrushStudio = false,
                showLayerPanel = false,
                showColorPanel = false,
                showFiltersPanel = false,
                showExportPanel = false,
                showTextEditor = false,
            )
        }
    }

    fun toggleTextEditor() {
        val eng = engine ?: return
        val active = eng.getActiveLayer()
        val editing = active?.meta?.type == "text"
        _uiState.update {
            it.copy(
                showTextEditor = !it.showTextEditor,
                editingTextLayer = editing,
                textLayerContent = active?.meta?.textContent?.text ?: it.textLayerContent,
                textLayerFontSize = active?.meta?.textContent?.fontSize ?: it.textLayerFontSize,
                showBrushStudio = false,
                showLayerPanel = false,
                showColorPanel = false,
                showFiltersPanel = false,
                showExportPanel = false,
                showAnimationPanel = false,
            )
        }
    }

    fun setExportFormat(format: ExportFormat) {
        _uiState.update { it.copy(exportFormat = format) }
    }

    fun setExportTransparentBackground(enabled: Boolean) {
        _uiState.update { it.copy(exportTransparentBackground = enabled) }
    }

    fun setExportAnimation(enabled: Boolean) {
        _uiState.update { it.copy(exportAnimation = enabled) }
    }

    fun setExportTimelapse4K(enabled: Boolean) {
        _uiState.update { it.copy(exportTimelapse4K = enabled) }
    }

    fun setAnimationEnabled(enabled: Boolean) {
        val eng = engine ?: return
        eng.animation.enabled = enabled
        _uiState.update { it.copy(animationEnabled = enabled) }
        scheduleAutoSave()
    }

    fun selectAnimationFrame(index: Int) {
        val eng = engine ?: return
        scope.launch {
            saveCurrentCelTiles()
            eng.animation.selectFrame(index)
            loadCelTilesForCurrentFrame()
            _uiState.update {
                it.copy(currentAnimationFrame = eng.animation.currentFrameIndex)
            }
        }
    }

    fun addAnimationFrame() {
        val eng = engine ?: return
        eng.animation.addFrame(eng.layerOrder())
        _uiState.update { it.copy(animationFrameCount = eng.animation.frameCount) }
        scheduleAutoSave()
    }

    fun duplicateAnimationFrame() {
        val eng = engine ?: return
        scope.launch {
            saveCurrentCelTiles()
            eng.animation.duplicateFrame(eng.layerOrder())
            loadCelTilesForCurrentFrame()
            _uiState.update {
                it.copy(
                    animationFrameCount = eng.animation.frameCount,
                    currentAnimationFrame = eng.animation.currentFrameIndex,
                )
            }
            scheduleAutoSave()
        }
    }

    fun deleteAnimationFrame() {
        val eng = engine ?: return
        val project = openProject ?: return
        scope.launch {
            val index = eng.animation.currentFrameIndex
            CelFrameStore(project.directory).deleteFrame(index)
            eng.animation.deleteFrame(index)
            loadCelTilesForCurrentFrame()
            _uiState.update {
                it.copy(
                    animationFrameCount = eng.animation.frameCount,
                    currentAnimationFrame = eng.animation.currentFrameIndex,
                )
            }
            scheduleAutoSave()
        }
    }

    fun toggleAnimationPlayback() {
        val eng = engine ?: return
        if (_uiState.value.animationPlaying) {
            playbackJob?.cancel()
            eng.animation.stopPlayback()
            _uiState.update { it.copy(animationPlaying = false) }
        } else {
            eng.animation.startPlayback()
            _uiState.update { it.copy(animationPlaying = true) }
            playbackJob = scope.launch {
                while (eng.animation.isPlaying) {
                    delay((1000L / eng.animation.fps.coerceAtLeast(1)))
                    if (eng.animation.advancePlayback()) {
                        loadCelTilesForCurrentFrame()
                        _uiState.update {
                            it.copy(currentAnimationFrame = eng.animation.currentFrameIndex)
                        }
                    }
                }
                _uiState.update { it.copy(animationPlaying = false) }
            }
        }
    }

    fun saveCelFrame() {
        scope.launch {
            saveCurrentCelTiles()
            _uiState.update { it.copy(saveMessage = "Cel frame saved") }
            scheduleAutoSave()
        }
    }

    fun setOnionSkinBefore(value: Float) {
        val eng = engine ?: return
        val settings = eng.animation.timeline.onionSkin.copy(before = value.toInt().coerceIn(0, 4))
        eng.animation.setOnionSkin(settings)
        _uiState.update { it.copy(onionSkinBefore = value) }
        scheduleAutoSave()
    }

    fun setOnionSkinAfter(value: Float) {
        val eng = engine ?: return
        val settings = eng.animation.timeline.onionSkin.copy(after = value.toInt().coerceIn(0, 4))
        eng.animation.setOnionSkin(settings)
        _uiState.update { it.copy(onionSkinAfter = value) }
        scheduleAutoSave()
    }

    fun setOnionSkinOpacity(value: Float) {
        val eng = engine ?: return
        val settings = eng.animation.timeline.onionSkin.copy(opacity = value.coerceIn(0.1f, 0.8f))
        eng.animation.setOnionSkin(settings)
        _uiState.update { it.copy(onionSkinOpacity = value) }
        scheduleAutoSave()
    }

    fun onTextPlaced(x: Float, y: Float) {
        _uiState.update {
            it.copy(
                pendingTextX = x,
                pendingTextY = y,
                showTextEditor = true,
                editingTextLayer = false,
            )
        }
    }

    fun setTextLayerContent(text: String) {
        _uiState.update { it.copy(textLayerContent = text) }
    }

    fun setTextLayerFontSize(size: Float) {
        _uiState.update { it.copy(textLayerFontSize = size) }
    }

    fun addTextLayerAtPending() {
        val eng = engine ?: return
        val state = _uiState.value
        scope.launch(Dispatchers.Default) {
            eng.withStateLock {
                eng.addTextLayer(
                    text = state.textLayerContent,
                    x = state.pendingTextX,
                    y = state.pendingTextY,
                    fontSize = state.textLayerFontSize,
                    color = state.brushColor,
                )
            }
            syncUiFromEngine()
            scheduleAutoSave()
            _uiState.update { it.copy(showTextEditor = false, saveMessage = "Text layer added") }
        }
    }

    fun applyTextLayerEdits() {
        val eng = engine ?: return
        val state = _uiState.value
        scope.launch(Dispatchers.Default) {
            eng.withStateLock {
                eng.updateActiveTextLayer(
                    text = state.textLayerContent,
                    fontSize = state.textLayerFontSize,
                    color = state.brushColor,
                )
            }
            syncUiFromEngine()
            scheduleAutoSave()
            _uiState.update { it.copy(showTextEditor = false, saveMessage = "Text updated") }
        }
    }

    fun importPalette(fileName: String, bytes: ByteArray) {
        scope.launch {
            runCatching {
                val colors = repository.importPalette(fileName, bytes)
                if (colors.isEmpty()) error("No colors found in palette file")
                _uiState.update {
                    it.copy(
                        savedPalette = (it.savedPalette + colors).distinct().takeLast(32),
                        saveMessage = "Imported ${colors.size} colors",
                    )
                }
                openProject?.let { repository.saveProjectPalette(it, _uiState.value.savedPalette) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Palette import failed") }
            }
        }
    }

    private suspend fun saveCurrentCelTiles() {
        val project = openProject ?: return
        val eng = engine ?: return
        val frameIndex = eng.animation.currentFrameIndex
        withContext(ioDispatcher) {
            eng.layers.forEach { layer ->
                val tiles = layer.tiles.mapValues { (_, v) -> v.copyOf() }
                repository.saveCelFrameTiles(project, frameIndex, layer.id, tiles)
            }
        }
    }

    private suspend fun loadCelTilesForCurrentFrame() {
        val project = openProject ?: return
        val eng = engine ?: return
        val frameIndex = eng.animation.currentFrameIndex
        val loaded = withContext(ioDispatcher) {
            eng.layers.associate { layer ->
                layer.id to repository.loadCelFrameTiles(project, frameIndex, layer.id)
            }
        }
        withContext(Dispatchers.Default) {
            loaded.forEach { (layerId, tiles) ->
                if (tiles.isEmpty()) return@forEach
                val layer = eng.layers.find { it.id == layerId } ?: return@forEach
                layer.restoreTiles(tiles)
                layer.markDirty()
            }
        }
    }
}
