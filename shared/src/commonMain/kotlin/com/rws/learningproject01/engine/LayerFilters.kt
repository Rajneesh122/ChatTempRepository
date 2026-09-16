package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.DEFAULT_TILE_SIZE
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

enum class LayerFilterType(val label: String) {
    HSB("Hue / Sat / Bright"),
    BLUR("Blur"),
    SHARPEN("Sharpen"),
    INVERT("Invert"),
    GRAYSCALE("Grayscale"),
    NOISE("Noise"),
    VIGNETTE("Vignette"),
}

data class FilterParams(
    val hueShift: Float = 0f,
    val saturation: Float = 1f,
    val brightness: Float = 0f,
    val blurRadius: Int = 2,
    val noiseAmount: Float = 0.15f,
    val vignetteStrength: Float = 0.5f,
)

object LayerFilters {
    fun apply(
        pixels: ByteArray,
        width: Int,
        height: Int,
        filter: LayerFilterType,
        params: FilterParams,
    ): ByteArray {
        return when (filter) {
            LayerFilterType.HSB -> applyHsb(pixels, params)
            LayerFilterType.BLUR -> boxBlur(pixels, width, height, params.blurRadius)
            LayerFilterType.SHARPEN -> sharpen(pixels, width, height)
            LayerFilterType.INVERT -> invert(pixels)
            LayerFilterType.GRAYSCALE -> grayscale(pixels)
            LayerFilterType.NOISE -> addNoise(pixels, params.noiseAmount)
            LayerFilterType.VIGNETTE -> vignette(pixels, width, height, params.vignetteStrength)
        }
    }

    private fun applyHsb(pixels: ByteArray, params: FilterParams): ByteArray {
        val out = pixels.copyOf()
        var i = 0
        while (i < out.size) {
            val a = out[i + 3].toInt() and 0xFF
            if (a == 0) {
                i += 4
                continue
            }
            val color = (a shl 24) or
                ((out[i].toInt() and 0xFF) shl 16) or
                ((out[i + 1].toInt() and 0xFF) shl 8) or
                (out[i + 2].toInt() and 0xFF)
            val hsv = ColorUtils.argbToHsv(color)
            val h = (hsv[0] + params.hueShift * 360f) % 360f
            val s = (hsv[1] * params.saturation).coerceIn(0f, 1f)
            val v = (hsv[2] + params.brightness).coerceIn(0f, 1f)
            val adjusted = ColorUtils.hsvToArgb(h, s, v, a)
            out[i] = ((adjusted shr 16) and 0xFF).toByte()
            out[i + 1] = ((adjusted shr 8) and 0xFF).toByte()
            out[i + 2] = (adjusted and 0xFF).toByte()
            i += 4
        }
        return out
    }

    private fun boxBlur(pixels: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        if (radius <= 0) return pixels.copyOf()
        val out = pixels.copyOf()
        val r = radius.coerceIn(1, 8)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var sumA = 0
                var count = 0
                for (dy in -r..r) {
                    for (dx in -r..r) {
                        val sx = x + dx
                        val sy = y + dy
                        if (sx !in 0 until width || sy !in 0 until height) continue
                        val idx = (sy * width + sx) * 4
                        sumR += pixels[idx].toInt() and 0xFF
                        sumG += pixels[idx + 1].toInt() and 0xFF
                        sumB += pixels[idx + 2].toInt() and 0xFF
                        sumA += pixels[idx + 3].toInt() and 0xFF
                        count++
                    }
                }
                if (count == 0) continue
                val idx = (y * width + x) * 4
                out[idx] = (sumR / count).toByte()
                out[idx + 1] = (sumG / count).toByte()
                out[idx + 2] = (sumB / count).toByte()
                out[idx + 3] = (sumA / count).toByte()
            }
        }
        return out
    }

    private fun sharpen(pixels: ByteArray, width: Int, height: Int): ByteArray {
        val kernel = intArrayOf(0, -1, 0, -1, 5, -1, 0, -1, 0)
        return convolve(pixels, width, height, kernel)
    }

    private fun convolve(pixels: ByteArray, width: Int, height: Int, kernel: IntArray): ByteArray {
        val out = pixels.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0
                var ki = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val idx = ((y + dy) * width + (x + dx)) * 4
                        val k = kernel[ki++]
                        r += (pixels[idx].toInt() and 0xFF) * k
                        g += (pixels[idx + 1].toInt() and 0xFF) * k
                        b += (pixels[idx + 2].toInt() and 0xFF) * k
                    }
                }
                val idx = (y * width + x) * 4
                out[idx] = r.coerceIn(0, 255).toByte()
                out[idx + 1] = g.coerceIn(0, 255).toByte()
                out[idx + 2] = b.coerceIn(0, 255).toByte()
            }
        }
        return out
    }

    private fun invert(pixels: ByteArray): ByteArray {
        val out = pixels.copyOf()
        var i = 0
        while (i < out.size) {
            val a = out[i + 3].toInt() and 0xFF
            if (a > 0) {
                out[i] = (255 - (out[i].toInt() and 0xFF)).toByte()
                out[i + 1] = (255 - (out[i + 1].toInt() and 0xFF)).toByte()
                out[i + 2] = (255 - (out[i + 2].toInt() and 0xFF)).toByte()
            }
            i += 4
        }
        return out
    }

    private fun grayscale(pixels: ByteArray): ByteArray {
        val out = pixels.copyOf()
        var i = 0
        while (i < out.size) {
            val a = out[i + 3].toInt() and 0xFF
            if (a > 0) {
                val r = out[i].toInt() and 0xFF
                val g = out[i + 1].toInt() and 0xFF
                val b = out[i + 2].toInt() and 0xFF
                val gray = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt()
                out[i] = gray.toByte()
                out[i + 1] = gray.toByte()
                out[i + 2] = gray.toByte()
            }
            i += 4
        }
        return out
    }

    private fun addNoise(pixels: ByteArray, amount: Float): ByteArray {
        val out = pixels.copyOf()
        val amp = (amount * 64).roundToInt()
        var i = 0
        while (i < out.size) {
            val a = out[i + 3].toInt() and 0xFF
            if (a > 0) {
                out[i] = ((out[i].toInt() and 0xFF) + Random.nextInt(-amp, amp + 1)).coerceIn(0, 255).toByte()
                out[i + 1] = ((out[i + 1].toInt() and 0xFF) + Random.nextInt(-amp, amp + 1)).coerceIn(0, 255).toByte()
                out[i + 2] = ((out[i + 2].toInt() and 0xFF) + Random.nextInt(-amp, amp + 1)).coerceIn(0, 255).toByte()
            }
            i += 4
        }
        return out
    }

    private fun vignette(pixels: ByteArray, width: Int, height: Int, strength: Float): ByteArray {
        val out = pixels.copyOf()
        val cx = width / 2f
        val cy = height / 2f
        val maxDist = kotlin.math.sqrt(cx * cx + cy * cy)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                val dist = kotlin.math.sqrt(dx * dx + dy * dy) / maxDist
                val factor = (1f - dist * strength).coerceIn(0f, 1f)
                val idx = (y * width + x) * 4
                out[idx] = ((out[idx].toInt() and 0xFF) * factor).roundToInt().toByte()
                out[idx + 1] = ((out[idx + 1].toInt() and 0xFF) * factor).roundToInt().toByte()
                out[idx + 2] = ((out[idx + 2].toInt() and 0xFF) * factor).roundToInt().toByte()
            }
        }
        return out
    }
}

object ColorDropFill {
    fun floodFill(
        pixels: ByteArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        fillColor: Int,
        tolerance: Int = 32,
    ): Boolean {
        if (startX !in 0 until width || startY !in 0 until height) return false
        val startIdx = (startY * width + startX) * 4
        val targetR = pixels[startIdx].toInt() and 0xFF
        val targetG = pixels[startIdx + 1].toInt() and 0xFF
        val targetB = pixels[startIdx + 2].toInt() and 0xFF
        val targetA = pixels[startIdx + 3].toInt() and 0xFF

        val fillR = (fillColor shr 16) and 0xFF
        val fillG = (fillColor shr 8) and 0xFF
        val fillB = fillColor and 0xFF
        val fillA = (fillColor shr 24) and 0xFF

        if (colorsMatch(targetR, targetG, targetB, targetA, fillR, fillG, fillB, fillA, tolerance)) {
            return false
        }

        val visited = BooleanArray(width * height)
        val stack = ArrayDeque<Int>()
        stack.add(startY * width + startX)

        while (stack.isNotEmpty()) {
            val pos = stack.removeLast()
            if (pos < 0 || pos >= visited.size || visited[pos]) continue
            val x = pos % width
            val y = pos / width
            val idx = pos * 4
            val r = pixels[idx].toInt() and 0xFF
            val g = pixels[idx + 1].toInt() and 0xFF
            val b = pixels[idx + 2].toInt() and 0xFF
            val a = pixels[idx + 3].toInt() and 0xFF
            if (!colorsMatch(r, g, b, a, targetR, targetG, targetB, targetA, tolerance)) continue

            visited[pos] = true
            pixels[idx] = fillR.toByte()
            pixels[idx + 1] = fillG.toByte()
            pixels[idx + 2] = fillB.toByte()
            pixels[idx + 3] = fillA.toByte()

            if (x > 0) stack.add(pos - 1)
            if (x < width - 1) stack.add(pos + 1)
            if (y > 0) stack.add(pos - width)
            if (y < height - 1) stack.add(pos + width)
        }
        return true
    }

    private fun colorsMatch(
        r1: Int, g1: Int, b1: Int, a1: Int,
        r2: Int, g2: Int, b2: Int, a2: Int,
        tolerance: Int,
    ): Boolean {
        if (a1 < 10 && a2 < 10) return true
        return abs(r1 - r2) <= tolerance &&
            abs(g1 - g2) <= tolerance &&
            abs(b1 - b2) <= tolerance &&
            abs(a1 - a2) <= tolerance
    }

    fun writePixelsToLayer(layer: LayerData, pixels: ByteArray, width: Int, height: Int) {
        layer.tiles.clear()
        val maxCol = (width + DEFAULT_TILE_SIZE - 1) / DEFAULT_TILE_SIZE
        val maxRow = (height + DEFAULT_TILE_SIZE - 1) / DEFAULT_TILE_SIZE
        for (col in 0 until maxCol) {
            for (row in 0 until maxRow) {
                val tile = ByteArray(DEFAULT_TILE_SIZE * DEFAULT_TILE_SIZE * 4)
                var hasContent = false
                val originX = col * DEFAULT_TILE_SIZE
                val originY = row * DEFAULT_TILE_SIZE
                for (py in 0 until DEFAULT_TILE_SIZE) {
                    val destY = originY + py
                    if (destY >= height) continue
                    for (px in 0 until DEFAULT_TILE_SIZE) {
                        val destX = originX + px
                        if (destX >= width) continue
                        val srcIdx = (destY * width + destX) * 4
                        val dstIdx = (py * DEFAULT_TILE_SIZE + px) * 4
                        tile[dstIdx] = pixels[srcIdx]
                        tile[dstIdx + 1] = pixels[srcIdx + 1]
                        tile[dstIdx + 2] = pixels[srcIdx + 2]
                        tile[dstIdx + 3] = pixels[srcIdx + 3]
                        if (tile[dstIdx + 3] != 0.toByte()) hasContent = true
                    }
                }
                if (hasContent) {
                    layer.tiles[col to row] = tile
                    layer.markTileDirty(col, row)
                }
            }
        }
        layer.markDirty()
    }
}
