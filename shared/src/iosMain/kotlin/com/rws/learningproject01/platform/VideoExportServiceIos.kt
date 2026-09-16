package com.rws.learningproject01.platform

import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.platform.ImageBitmap
import okio.FileSystem
import okio.Path

object VideoExportServiceIos {
    fun encodeFrames(
        frames: List<ImageBitmap>,
        output: Path,
        fps: Int,
        format: ExportFormat,
        force4K: Boolean,
    ) {
        // AVAssetWriter integration placeholder: writes numbered PNG sequence for now.
        val dir = output.parent!!.resolve(output.name.substringBeforeLast('.') + "_video_frames")
        FileSystem.SYSTEM.createDirectories(dir)
        frames.forEachIndexed { index, frame ->
            val frameFile = dir.resolve("frame_${index.toString().padStart(4, '0')}.png")
            FileSystem.SYSTEM.write(frameFile) { write(frame.compressPng()) }
        }
        FileSystem.SYSTEM.write(output) { write(frames.firstOrNull()?.compressPng() ?: ByteArray(0)) }
    }
}
