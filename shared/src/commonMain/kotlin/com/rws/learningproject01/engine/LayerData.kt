package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import com.rws.learningproject01.core.model.LayerMeta
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LayerData private constructor(
    val id: String,
    var meta: LayerMeta,
    val tiles: MutableMap<Pair<Int, Int>, ByteArray> = mutableMapOf(),
    private val dirtyTiles: MutableSet<Pair<Int, Int>> = mutableSetOf(),
    var textureId: Int = 0,
    var dirtyGpu: Boolean = true,
) {
    fun getOrCreateTile(col: Int, row: Int): ByteArray =
        tiles.getOrPut(col to row) { ByteArray(DEFAULT_TILE_SIZE * DEFAULT_TILE_SIZE * 4) }

    fun copyTiles(): Map<Pair<Int, Int>, ByteArray> =
        tiles.mapValues { (_, value) -> value.copyOf() }

    fun restoreTiles(snapshot: Map<Pair<Int, Int>, ByteArray>) {
        tiles.clear()
        tiles.putAll(snapshot.mapValues { (_, value) -> value.copyOf() })
        dirtyGpu = true
        dirtyTiles.clear()
        snapshot.keys.forEach { dirtyTiles.add(it) }
    }

    fun markTileDirty(col: Int, row: Int) {
        dirtyTiles.add(col to row)
        dirtyGpu = true
    }

    fun markDirty() {
        dirtyGpu = true
    }

    fun clearGpuDirty() {
        dirtyGpu = false
    }

    fun gpuUploadCoords(): List<Pair<Int, Int>> =
        if (dirtyTiles.isNotEmpty()) dirtyTiles.toList() else tiles.keys.toList()

    fun consumeDirtyTiles(): List<Pair<Int, Int>> {
        val result = dirtyTiles.toList()
        dirtyTiles.clear()
        return result
    }

    fun flattenToRgba(width: Int, height: Int): ByteArray {
        val buffer = ByteArray(width * height * 4)
        tiles.forEach { (coord, tile) ->
            val (col, row) = coord
            val originX = col * DEFAULT_TILE_SIZE
            val originY = row * DEFAULT_TILE_SIZE
            for (py in 0 until DEFAULT_TILE_SIZE) {
                val destY = originY + py
                if (destY >= height) continue
                for (px in 0 until DEFAULT_TILE_SIZE) {
                    val destX = originX + px
                    if (destX >= width) continue
                    val srcIdx = (py * DEFAULT_TILE_SIZE + px) * 4
                    val dstIdx = (destY * width + destX) * 4
                    buffer[dstIdx] = tile[srcIdx]
                    buffer[dstIdx + 1] = tile[srcIdx + 1]
                    buffer[dstIdx + 2] = tile[srcIdx + 2]
                    buffer[dstIdx + 3] = tile[srcIdx + 3]
                }
            }
        }
        return buffer
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun create(name: String, width: Int, height: Int, index: Int): LayerData {
            val id = Uuid.random().toString()
            return LayerData(
                id = id,
                meta = LayerMeta(
                    id = id,
                    name = name,
                    bounds = com.rws.learningproject01.core.model.LayerBounds(
                        maxX = width,
                        maxY = height,
                    ),
                ),
            )
        }

        fun fromMeta(meta: LayerMeta, width: Int, height: Int): LayerData =
            LayerData(id = meta.id, meta = meta)
    }
}
