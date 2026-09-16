package com.rws.learningproject01.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorHarmonyTest {

    @Test
    fun complementary_returnsTwoColors() {
        val base = 0xFFFF0000.toInt()
        val colors = ColorHarmony.colors(base, HarmonyType.COMPLEMENTARY)
        assertEquals(2, colors.size)
        assertEquals(base, colors[0])
    }

    @Test
    fun hsvRoundTrip_preservesHue() {
        val original = ColorUtils.hsvToArgb(120f, 0.8f, 0.9f)
        val hsv = ColorUtils.argbToHsv(original)
        assertEquals(120f, hsv[0], 1f)
    }
}
