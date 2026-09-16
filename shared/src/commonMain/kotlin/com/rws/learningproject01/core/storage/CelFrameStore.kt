package com.rws.learningproject01.core.storage

import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import com.rws.learningproject01.platform.encodeRgba
import com.rws.learningproject01.platform.decodeRgba
import okio.FileSystem
import okio.Path

class CelFrameStore(private val projectDir: Path) {

    private val fs = FileSystem.SYSTEM

    private fun frameDir(frameIndex: Int, layerId: String): Path =
        projectDir.resolve("animation/cels/$frameIndex/$layerId/tiles")

    fun saveFrameTiles(frameIndex: Int, layerId: String, tiles: Map<Pair<Int, Int>, ByteArray>) {
        val dir = frameDir(frameIndex, layerId)
        fs.createDirectories(dir)
        tiles.forEach { (coord, data) ->
            val (col, row) = coord
            AtomicFileWriter.writeBytes(
                dir.resolve("${col}_$row.lz4"),
                encodeRgba(data, DEFAULT_TILE_SIZE),
            )
        }
    }

    fun loadFrameTiles(frameIndex: Int, layerId: String): Map<Pair<Int, Int>, ByteArray> {
        val dir = frameDir(frameIndex, layerId)
        if (!fs.exists(dir)) return emptyMap()
        return fs.list(dir)
            .filter { it.name.endsWith(".lz4") }
            .mapNotNull { file ->
                val baseName = file.name.removeSuffix(".lz4")
                val parts = baseName.split("_")
                if (parts.size != 2) return@mapNotNull null
                val col = parts[0].toIntOrNull() ?: return@mapNotNull null
                val row = parts[1].toIntOrNull() ?: return@mapNotNull null
                (col to row) to decodeRgba(fs.read(file) { readByteArray() })
            }
            .toMap()
    }

    fun deleteFrame(frameIndex: Int) {
        val dir = projectDir.resolve("animation/cels/$frameIndex")
        if (fs.exists(dir)) fs.deleteRecursively(dir)
    }

    fun hasFrameTiles(frameIndex: Int, layerId: String): Boolean {
        val dir = frameDir(frameIndex, layerId)
        return fs.exists(dir) && fs.list(dir).isNotEmpty()
    }
}
