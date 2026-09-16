package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.TimelapseEvent
import com.rws.learningproject01.core.model.TimelapseLog
import com.rws.learningproject01.core.model.TimelapsePoint
import kotlinx.datetime.Clock

class TimelapseRecorder(
    private val canvasWidth: Int,
    private val canvasHeight: Int,
) {
    private val events = mutableListOf<TimelapseEvent>()
    private var strokeStartMs: Long = 0L
    private val currentPoints = mutableListOf<TimelapsePoint>()

    fun onStrokeBegin(layerId: String, color: Int, brushSize: Float) {
        strokeStartMs = Clock.System.now().toEpochMilliseconds()
        currentPoints.clear()
        events.add(
            TimelapseEvent(
                timestampMs = strokeStartMs,
                type = "stroke_begin",
                layerId = layerId,
                color = color,
                brushSize = brushSize,
            ),
        )
    }

    fun onStrokePoint(x: Float, y: Float, pressure: Float) {
        currentPoints.add(TimelapsePoint(x, y, pressure))
    }

    fun onStrokeEnd(layerId: String) {
        events.add(
            TimelapseEvent(
                timestampMs = Clock.System.now().toEpochMilliseconds(),
                type = "stroke_end",
                layerId = layerId,
                points = currentPoints.toList(),
            ),
        )
        currentPoints.clear()
    }

    fun toLog(): TimelapseLog = TimelapseLog(canvasWidth, canvasHeight, events.toList())

    fun loadLog(log: TimelapseLog) {
        events.clear()
        events.addAll(log.events)
    }

    fun clear() {
        events.clear()
        currentPoints.clear()
    }
}

object TimelapseReplayer {

    fun replayFrameCount(log: TimelapseLog, fps: Int): Int {
        if (log.events.isEmpty()) return 1
        val durationMs = log.events.last().timestampMs - log.events.first().timestampMs
        return ((durationMs / 1000f) * fps).toInt().coerceAtLeast(1)
    }

    fun eventsUpToFrame(log: TimelapseLog, frameIndex: Int, fps: Int): List<TimelapseEvent> {
        if (log.events.isEmpty()) return emptyList()
        val startMs = log.events.first().timestampMs
        val frameEndMs = startMs + ((frameIndex + 1) * 1000L / fps)
        return log.events.filter { it.timestampMs <= frameEndMs }
    }
}
