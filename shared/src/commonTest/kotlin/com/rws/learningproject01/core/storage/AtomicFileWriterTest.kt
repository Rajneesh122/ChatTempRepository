package com.rws.learningproject01.core.storage

import com.rws.learningproject01.platform.Lz4Codec
import com.rws.learningproject01.platform.decodeRgba
import com.rws.learningproject01.platform.encodeRgba
import com.rws.learningproject01.platform.rgbaTileBuffer
import com.rws.learningproject01.platform.tileRawSize
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import okio.FileSystem
import okio.Path.Companion.toPath

class AtomicFileWriterTest {

    @Test
    fun writeText_rejectsEmptyContent() {
        val file = createTempPath()
        try {
            AtomicFileWriter.writeText(file, "")
            error("Expected exception")
        } catch (_: IllegalArgumentException) {
            // expected
        } finally {
            if (FileSystem.SYSTEM.exists(file)) FileSystem.SYSTEM.delete(file)
        }
    }

    @Test
    fun writeText_writesValidJson() {
        val file = createTempPath()
        try {
            AtomicFileWriter.writeText(file, """{"projects":[]}""")
            val text = FileSystem.SYSTEM.read(file) { readUtf8() }
            assertEquals(false, text.isEmpty())
        } finally {
            if (FileSystem.SYSTEM.exists(file)) FileSystem.SYSTEM.delete(file)
        }
    }

    private fun createTempPath(): okio.Path {
        val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("gallery_index_${kotlin.random.Random.nextInt()}.json")
        if (FileSystem.SYSTEM.exists(path)) FileSystem.SYSTEM.delete(path)
        return path
    }
}
