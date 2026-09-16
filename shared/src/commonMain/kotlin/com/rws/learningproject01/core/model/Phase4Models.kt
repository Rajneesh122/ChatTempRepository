package com.rws.learningproject01.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TextLayerContent(
    val text: String = "Text",
    val fontId: String = "default",
    val fontSize: Float = 48f,
    val color: String = "#FF000000",
    val alignment: String = "left",
)

@Serializable
data class ProjectPalette(
    val colors: List<String> = emptyList(),
)

@Serializable
data class CelFrameRef(
    val layerId: String,
    val hasTiles: Boolean = false,
)

@Serializable
data class TimelapseEvent(
    val timestampMs: Long,
    val type: String,
    val layerId: String? = null,
    val points: List<TimelapsePoint> = emptyList(),
    val color: Int? = null,
    val brushSize: Float? = null,
)

@Serializable
data class TimelapsePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
)

@Serializable
data class TimelapseLog(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val events: List<TimelapseEvent> = emptyList(),
)
