package com.rws.learningproject01.core.model

import kotlinx.serialization.Serializable

enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    TIFF("tiff", "image/tiff"),
    GIF("gif", "image/gif"),
    PDF("pdf", "application/pdf"),
    MP4("mp4", "video/mp4"),
    HEVC("mp4", "video/mp4"),
}

@Serializable
data class ExportOptions(
    val format: String = ExportFormat.PNG.name,
    val width: Int = 0,
    val height: Int = 0,
    val jpegQuality: Int = 92,
    val transparentBackground: Boolean = true,
    val exportAnimation: Boolean = false,
    val animationFps: Int = 12,
    val timelapse4K: Boolean = false,
)
