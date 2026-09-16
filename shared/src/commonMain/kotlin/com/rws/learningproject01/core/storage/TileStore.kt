package com.rws.learningproject01.core.storage

import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import com.rws.learningproject01.core.model.LayerMeta
import com.rws.learningproject01.platform.decodeRgba
import com.rws.learningproject01.platform.encodeRgba
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class TileStore(private val projectDir: Path) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val fs = FileSystem.SYSTEM

    fun layerDir(layerId: String): Path =
        projectDir.resolve("layers").resolve(layerId)

    fun tilesDir(layerId: String): Path =
        layerDir(layerId).resolve("tiles")

    fun writeLayerMeta(layer: LayerMeta) {
        val file = layerDir(layer.id).resolve("layer.json")
        AtomicFileWriter.writeText(file, json.encodeToString(layer))
    }

    fun readLayerMeta(layerId: String): LayerMeta? {
        val file = layerDir(layerId).resolve("layer.json")
        if (!fs.exists(file) || fs.metadata(file).size == 0L) return null
        val text = fs.read(file) { readUtf8() }.trim()
        if (text.isEmpty()) return null
        return runCatching { json.decodeFromString<LayerMeta>(text) }.getOrNull()
    }

    fun writeTile(layerId: String, col: Int, row: Int, rgba: ByteArray, tileSize: Int = DEFAULT_TILE_SIZE) {
        val encoded = encodeRgba(rgba, tileSize)
        val file = tilesDir(layerId).resolve("${col}_$row.lz4")
        AtomicFileWriter.writeBytes(file, encoded)
    }

    fun readTile(layerId: String, col: Int, row: Int, tileSize: Int = DEFAULT_TILE_SIZE): ByteArray? {
        val file = tilesDir(layerId).resolve("${col}_$row.lz4")
        if (!fs.exists(file)) return null
        val bytes = fs.read(file) { readByteArray() }
        return decodeRgba(bytes)
    }

    fun listTiles(layerId: String): List<Pair<Int, Int>> {
        val dir = tilesDir(layerId)
        if (!fs.exists(dir)) return emptyList()
        return fs.list(dir)
            .filter { it.name.endsWith(".lz4") }
            .mapNotNull { file ->
                val baseName = file.name.removeSuffix(".lz4")
                val parts = baseName.split("_")
                if (parts.size != 2) return@mapNotNull null
                val col = parts[0].toIntOrNull() ?: return@mapNotNull null
                val row = parts[1].toIntOrNull() ?: return@mapNotNull null
                col to row
            }
    }

    fun deleteTile(layerId: String, col: Int, row: Int) {
        val file = tilesDir(layerId).resolve("${col}_$row.lz4")
        if (fs.exists(file)) fs.delete(file)
    }

    fun deleteLayer(layerId: String) {
        val dir = layerDir(layerId)
        if (fs.exists(dir)) fs.deleteRecursively(dir)
    }
}
