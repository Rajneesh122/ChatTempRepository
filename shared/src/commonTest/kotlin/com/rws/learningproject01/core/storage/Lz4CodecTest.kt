package com.rws.learningproject01.core.storage

import com.rws.learningproject01.platform.Lz4Codec
import com.rws.learningproject01.platform.clearTile
import com.rws.learningproject01.platform.decodeRgba
import com.rws.learningproject01.platform.encodeRgba
import com.rws.learningproject01.platform.rgbaTileBuffer
import com.rws.learningproject01.platform.tileRawSize
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Lz4CodecTest {

    @Test
    fun roundTrip_preservesTileBytes() {
        val raw = ByteArray(Lz4Codec.tileRawSize()) { index ->
            (index % 256).toByte()
        }
        val encoded = encodeRgba(raw)
        val decoded = decodeRgba(encoded)
        assertContentEquals(raw, decoded)
    }

    @Test
    fun tileRawSize_matchesRgba8888() {
        assertEquals(256 * 256 * 4, Lz4Codec.tileRawSize())
    }
}
