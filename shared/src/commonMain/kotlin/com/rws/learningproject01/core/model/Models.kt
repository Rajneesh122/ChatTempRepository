package com.rws.learningproject01.core.model

import kotlinx.serialization.Serializable

const val LPDOC_FORMAT_VERSION = 1
const val DEFAULT_TILE_SIZE = 256
const val DEFAULT_CANVAS_SIZE = 2048
const val DEFAULT_DPI = 300
const val MAX_UNDO_LEVELS = 250

@Serializable
data class ProjectManifest(
    val formatVersion: Int = LPDOC_FORMAT_VERSION,
    val appVersion: String = "1.0.0",
    val projectId: String,
    val title: String = "Untitled",
    val createdAt: String,
    val modifiedAt: String,
    val canvas: CanvasSpec,
    val layerOrder: List<String> = emptyList(),
    val layerGroups: List<LayerGroup> = emptyList(),
    val animation: AnimationSummary = AnimationSummary(),
    val checksum: String? = null,
)

@Serializable
data class CanvasSpec(
    val width: Int = DEFAULT_CANVAS_SIZE,
    val height: Int = DEFAULT_CANVAS_SIZE,
    val dpi: Int = DEFAULT_DPI,
    val colorSpace: String = "sRGB",
    val background: CanvasBackground = CanvasBackground(),
)

@Serializable
data class CanvasBackground(
    val type: String = "solid",
    val color: String = "#FFFFFFFF",
)

@Serializable
data class LayerGroup(
    val id: String,
    val name: String,
    val childLayerIds: List<String>,
    val collapsed: Boolean = false,
)

@Serializable
data class AnimationSummary(
    val enabled: Boolean = false,
    val fps: Int = 12,
    val frameCount: Int = 1,
)

@Serializable
data class LayerMeta(
    val id: String,
    val name: String,
    val type: String = "raster",
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1f,
    val blendMode: String = BlendMode.NORMAL.id,
    val clipToBelow: Boolean = false,
    val transform: LayerTransform = LayerTransform(),
    val tileSize: Int = DEFAULT_TILE_SIZE,
    val bounds: LayerBounds = LayerBounds(),
    val flags: LayerFlags = LayerFlags(),
    val textContent: TextLayerContent? = null,
)

@Serializable
data class LayerTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
)

@Serializable
data class LayerBounds(
    val minX: Int = 0,
    val minY: Int = 0,
    val maxX: Int = DEFAULT_CANVAS_SIZE,
    val maxY: Int = DEFAULT_CANVAS_SIZE,
)

@Serializable
data class LayerFlags(
    val alphaLock: Boolean = false,
)

@Serializable
data class EditorState(
    val activeLayerId: String? = null,
    val viewport: ViewportState = ViewportState(),
    val activeBrushId: String = "brush-round-default",
    val tool: String = "brush",
    val selection: String? = null,
)

@Serializable
data class ViewportState(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1f,
)

@Serializable
data class AnimationTimeline(
    val frames: List<AnimationFrame> = emptyList(),
    val onionSkin: OnionSkinSettings = OnionSkinSettings(),
)

@Serializable
data class AnimationFrame(
    val index: Int,
    val durationMs: Long = 83,
    val layerStates: Map<String, FrameLayerState> = emptyMap(),
    val celRefs: List<CelFrameRef> = emptyList(),
)

@Serializable
data class FrameLayerState(
    val visible: Boolean = true,
    val opacity: Float = 1f,
)

@Serializable
data class OnionSkinSettings(
    val before: Int = 1,
    val after: Int = 1,
    val opacity: Float = 0.3f,
)

@Serializable
data class GalleryIndex(
    val projects: List<GalleryEntry> = emptyList(),
)

@Serializable
data class GalleryEntry(
    val projectId: String,
    val title: String,
    val modifiedAt: String,
    val thumbPath: String,
)

@Serializable
data class SaveJournal(
    val dirtyTiles: List<String> = emptyList(),
    val lastCheckpoint: String? = null,
)

enum class BlendMode(val id: String) {
    NORMAL("normal"),
    MULTIPLY("multiply"),
    SCREEN("screen"),
    OVERLAY("overlay"),
    DARKEN("darken"),
    LIGHTEN("lighten"),
    COLOR_DODGE("colorDodge"),
    COLOR_BURN("colorBurn"),
    SOFT_LIGHT("softLight"),
    HARD_LIGHT("hardLight"),
    DIFFERENCE("difference"),
    EXCLUSION("exclusion"),
    ;

    companion object {
        fun fromId(id: String): BlendMode =
            entries.find { it.id == id } ?: NORMAL
    }
}
