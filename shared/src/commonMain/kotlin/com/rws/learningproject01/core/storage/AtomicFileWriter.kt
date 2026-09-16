package com.rws.learningproject01.core.storage

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private val fs = FileSystem.SYSTEM

object AtomicFileWriter {
    fun writeBytes(target: Path, data: ByteArray) {
        require(data.isNotEmpty()) { "Refusing to write empty file: ${target.name}" }
        target.parent?.let { fs.createDirectories(it) }
        val temp = target.parent!!.resolve("${target.name}.tmp")
        fs.write(temp) { write(data) }
        runCatching { fs.atomicMove(temp, target) }.onFailure {
            fs.write(target) { write(data) }
            runCatching { fs.delete(temp) }
        }
    }

    fun writeText(target: Path, text: String) {
        val bytes = text.encodeToByteArray()
        require(bytes.isNotEmpty()) { "Refusing to write empty file: ${target.name}" }
        writeBytes(target, bytes)
    }
}

fun String.toPathFromString(): Path = this.toPath()
