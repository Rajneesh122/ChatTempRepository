package com.rws.learningproject01.core.storage

import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.platform.ExportPlatform
import com.rws.learningproject01.platform.ImageBitmap
import okio.FileSystem
import okio.Path

object ExportService {

    fun exportBitmap(bitmap: ImageBitmap, format: ExportFormat, output: Path, jpegQuality: Int = 92) {
        output.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        ExportPlatform.exportBitmap(bitmap, format, output, jpegQuality)
    }

    fun exportAnimationFrames(
        frames: List<ImageBitmap>,
        format: ExportFormat,
        output: Path,
        fps: Int,
        jpegQuality: Int = 92,
        force4K: Boolean = false,
    ) {
        output.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        ExportPlatform.exportAnimationFrames(frames, format, output, fps, jpegQuality, force4K)
    }

    fun resolveFormat(name: String): ExportFormat =
        runCatching { ExportFormat.valueOf(name) }.getOrDefault(ExportFormat.PNG)

    fun scaledSize(sourceW: Int, sourceH: Int, targetW: Int, targetH: Int): Pair<Int, Int> {
        if (targetW <= 0 || targetH <= 0) return sourceW to sourceH
        return targetW to targetH
    }
}
