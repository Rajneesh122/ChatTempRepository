package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.AnimationFrame
import com.rws.learningproject01.core.model.AnimationTimeline
import com.rws.learningproject01.core.model.FrameLayerState
import com.rws.learningproject01.core.model.OnionSkinSettings

class AnimationController(
    var timeline: AnimationTimeline = AnimationTimeline(),
    var currentFrameIndex: Int = 0,
    var enabled: Boolean = false,
    var fps: Int = 12,
) {
    var isPlaying: Boolean = false
        private set

    val frameCount: Int
        get() = timeline.frames.size.coerceAtLeast(1)

    fun ensureDefaultFrame(layerIds: List<String>) {
        if (timeline.frames.isNotEmpty()) return
        timeline = timeline.copy(
            frames = listOf(
                AnimationFrame(
                    index = 0,
                    layerStates = layerIds.associateWith { FrameLayerState() },
                ),
            ),
        )
    }

    fun currentFrame(): AnimationFrame? =
        timeline.frames.getOrNull(currentFrameIndex)

    fun frameOverrides(frameIndex: Int): Map<String, Pair<Boolean, Float>> {
        val frame = timeline.frames.getOrNull(frameIndex) ?: return emptyMap()
        return frame.layerStates.mapValues { (_, state) -> state.visible to state.opacity }
    }

    fun onionFrameIndices(): List<Pair<Int, Float>> {
        if (!enabled) return emptyList()
        val settings = timeline.onionSkin
        val result = mutableListOf<Pair<Int, Float>>()
        for (offset in 1..settings.before) {
            val idx = currentFrameIndex - offset
            if (idx >= 0) result.add(idx to settings.opacity)
        }
        for (offset in 1..settings.after) {
            val idx = currentFrameIndex + offset
            if (idx < timeline.frames.size) result.add(idx to settings.opacity)
        }
        return result
    }

    fun selectFrame(index: Int) {
        currentFrameIndex = index.coerceIn(0, (timeline.frames.size - 1).coerceAtLeast(0))
    }

    fun addFrame(layerIds: List<String>) {
        val newIndex = timeline.frames.size
        val states = layerIds.associateWith { FrameLayerState() }
        timeline = timeline.copy(
            frames = timeline.frames + AnimationFrame(index = newIndex, layerStates = states),
        )
        currentFrameIndex = newIndex
    }

    fun duplicateFrame(layerIds: List<String>) {
        val source = currentFrame() ?: return
        val newIndex = timeline.frames.size
        val states = if (source.layerStates.isNotEmpty()) {
            source.layerStates.mapValues { (_, s) -> s.copy() }
        } else {
            layerIds.associateWith { FrameLayerState() }
        }
        timeline = timeline.copy(
            frames = timeline.frames + AnimationFrame(
                index = newIndex,
                durationMs = source.durationMs,
                layerStates = states,
                celRefs = source.celRefs,
            ),
        )
        currentFrameIndex = newIndex
    }

    fun deleteFrame(index: Int) {
        if (timeline.frames.size <= 1) return
        val updated = timeline.frames.filterIndexed { i, _ -> i != index }
            .mapIndexed { i, frame -> frame.copy(index = i) }
        timeline = timeline.copy(frames = updated)
        currentFrameIndex = currentFrameIndex.coerceIn(0, updated.lastIndex)
    }

    fun setLayerFrameState(layerId: String, visible: Boolean, opacity: Float) {
        val frame = currentFrame() ?: return
        val updatedStates = frame.layerStates.toMutableMap()
        updatedStates[layerId] = FrameLayerState(visible, opacity)
        updateCurrentFrame(frame.copy(layerStates = updatedStates))
    }

    fun setOnionSkin(settings: OnionSkinSettings) {
        timeline = timeline.copy(onionSkin = settings)
    }

    fun startPlayback() {
        isPlaying = true
    }

    fun stopPlayback() {
        isPlaying = false
    }

    fun advancePlayback(): Boolean {
        if (!isPlaying || timeline.frames.isEmpty()) return false
        currentFrameIndex = if (currentFrameIndex >= timeline.frames.lastIndex) 0 else currentFrameIndex + 1
        return true
    }

    private fun updateCurrentFrame(frame: AnimationFrame) {
        val frames = timeline.frames.toMutableList()
        if (currentFrameIndex in frames.indices) {
            frames[currentFrameIndex] = frame
            timeline = timeline.copy(frames = frames)
        }
    }
}
