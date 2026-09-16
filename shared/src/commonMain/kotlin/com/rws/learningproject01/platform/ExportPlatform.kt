package com.rws.learningproject01.platform

import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.platform.ImageBitmap
import okio.Path

expect object ExportPlatform {
    fun exportBitmap(bitmap: ImageBitmap, format: ExportFormat, output: Path, jpegQuality: Int = 92)
    fun exportAnimationFrames(
        frames: List<ImageBitmap>,
        format: ExportFormat,
        output: Path,
        fps: Int,
        jpegQuality: Int = 92,
        force4K: Boolean = false,
    )
    fun thumbnailExtension(): String
}
