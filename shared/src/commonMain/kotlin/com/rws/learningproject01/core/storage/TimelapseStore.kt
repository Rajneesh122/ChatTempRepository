package com.rws.learningproject01.core.storage

import com.rws.learningproject01.core.model.TimelapseLog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class TimelapseStore(private val projectDir: Path) {

    private val logFile = projectDir.resolve("timelapse/log.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val fs = FileSystem.SYSTEM

    fun read(): TimelapseLog? {
        if (!fs.exists(logFile) || fs.metadata(logFile).size == 0L) return null
        val text = fs.read(logFile) { readUtf8() }.trim()
        if (text.isEmpty()) return null
        return runCatching { json.decodeFromString<TimelapseLog>(text) }.getOrNull()
    }

    fun save(log: TimelapseLog) {
        logFile.parent?.let { fs.createDirectories(it) }
        AtomicFileWriter.writeText(logFile, json.encodeToString(log))
    }

    fun clear() {
        if (fs.exists(logFile)) fs.delete(logFile)
    }
}
