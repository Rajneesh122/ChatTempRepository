package com.rws.learningproject01.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ColorUtils {
    fun argbToHsv(color: Int): FloatArray {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        val h = when {
            delta == 0f -> 0f
            maxC == r -> 60f * (((g - b) / delta) % 6f)
            maxC == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        val hue = if (h < 0f) h + 360f else h
        val sat = if (maxC == 0f) 0f else delta / maxC
        return floatArrayOf(hue, sat, maxC)
    }

    fun hsvToArgb(h: Float, s: Float, v: Float, alpha: Int = 255): Int {
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).roundToInt().coerceIn(0, 255)
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha * 255).roundToInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (a shl 24)
    }
}

enum class HarmonyType(val label: String) {
    COMPLEMENTARY("Complementary"),
    ANALOGOUS("Analogous"),
    TRIADIC("Triadic"),
    SPLIT_COMPLEMENTARY("Split"),
    TETRADIC("Tetradic"),
}

object ColorHarmony {
    fun colors(baseColor: Int, type: HarmonyType, count: Int = 5): List<Int> {
        val hsv = ColorUtils.argbToHsv(baseColor)
        val h = hsv[0]
        val s = hsv[1]
        val v = hsv[2]
        return when (type) {
            HarmonyType.COMPLEMENTARY -> listOf(
                baseColor,
                ColorUtils.hsvToArgb((h + 180f) % 360f, s, v),
            )
            HarmonyType.ANALOGOUS -> listOf(
                ColorUtils.hsvToArgb((h - 30f + 360f) % 360f, s, v),
                baseColor,
                ColorUtils.hsvToArgb((h + 30f) % 360f, s, v),
                ColorUtils.hsvToArgb((h + 60f) % 360f, s * 0.9f, v),
            )
            HarmonyType.TRIADIC -> listOf(
                baseColor,
                ColorUtils.hsvToArgb((h + 120f) % 360f, s, v),
                ColorUtils.hsvToArgb((h + 240f) % 360f, s, v),
            )
            HarmonyType.SPLIT_COMPLEMENTARY -> listOf(
                baseColor,
                ColorUtils.hsvToArgb((h + 150f) % 360f, s, v),
                ColorUtils.hsvToArgb((h + 210f) % 360f, s, v),
            )
            HarmonyType.TETRADIC -> listOf(
                baseColor,
                ColorUtils.hsvToArgb((h + 90f) % 360f, s, v),
                ColorUtils.hsvToArgb((h + 180f) % 360f, s, v),
                ColorUtils.hsvToArgb((h + 270f) % 360f, s, v),
            )
        }.take(count)
    }
}
