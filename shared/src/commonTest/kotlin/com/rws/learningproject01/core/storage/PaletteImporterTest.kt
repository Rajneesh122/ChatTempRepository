package com.rws.learningproject01.core.storage

import kotlin.test.Test
import kotlin.test.assertTrue

class PaletteImporterTest {

    @Test
    fun parseAco_rgbColors() {
        val bytes = byteArrayOf(
            0x00, 0x01, // version
            0x00, 0x01, // count
            0x00, 0x00, // RGB
            0xFF.toByte(), 0xFF.toByte(), // r
            0x00, 0x00, // g
            0x00, 0x00, // b
        )
        val colors = PaletteImporter.parseAco(bytes)
        assertTrue(colors.isNotEmpty())
        assertTrue((colors.first() and 0x00FF0000.toInt()) != 0)
    }

    @Test
    fun parseAse_empty_returnsEmpty() {
        val colors = PaletteImporter.parseAse(ByteArray(4))
        assertTrue(colors.isEmpty())
    }
}
