package com.rws.learningproject01.engine

enum class DrawingTool {
    BRUSH,
    ERASER,
    SMUDGE,
    COLOR_DROP,
    COLOR_PICKER,
    TEXT,
}

enum class SymmetryMode(val label: String) {
    OFF("Off"),
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal"),
    QUADRANT("Quadrant"),
}

data class BrushSettings(
    val size: Float = 24f,
    val opacity: Float = 1f,
    val color: Int = 0xFF000000.toInt(),
    val hardness: Float = 0.85f,
    val spacing: Float = 0.15f,
    val streamLine: Float = 0.3f,
    val smudgeStrength: Float = 0.5f,
)

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
)
