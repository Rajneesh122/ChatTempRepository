package com.rws.learningproject01.core.storage

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.File
import java.nio.ByteBuffer

object VideoExportService {

    private const val TIMELAPSE_4K_WIDTH = 3840
    private const val TIMELAPSE_4K_HEIGHT = 2160

    fun encodeFrames(
        frames: List<Bitmap>,
        output: File,
        fps: Int,
        useHevc: Boolean = false,
        force4K: Boolean = false,
    ) {
        require(frames.isNotEmpty()) { "No frames to encode" }
        output.parentFile?.mkdirs()

        val first = frames.first()
        val width = if (force4K) TIMELAPSE_4K_WIDTH else first.width
        val height = if (force4K) TIMELAPSE_4K_HEIGHT else first.height
        val mime = if (useHevc && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaFormat.MIMETYPE_VIDEO_HEVC
        } else {
            MediaFormat.MIMETYPE_VIDEO_AVC
        }

        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, if (force4K) 16_000_000 else 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val frameDurationUs = 1_000_000L / fps

        frames.forEachIndexed { index, source ->
            val bitmap = if (source.width != width || source.height != height) {
                Bitmap.createScaledBitmap(source, width, height, true)
            } else {
                source
            }
            val yuv = bitmapToNv21(bitmap)
            if (bitmap !== source) bitmap.recycle()

            var inputIndex = codec.dequeueInputBuffer(10_000)
            while (inputIndex < 0) inputIndex = codec.dequeueInputBuffer(10_000)
            val inputBuffer = codec.getInputBuffer(inputIndex) ?: error("No input buffer")
            inputBuffer.clear()
            inputBuffer.put(yuv)
            val pts = index * frameDurationUs
            codec.queueInputBuffer(inputIndex, 0, yuv.size, pts, 0)

            muxerStarted = drainEncoder(codec, muxer, bufferInfo, trackIndex, muxerStarted).let {
                trackIndex = it.first
                it.second
            }
        }

        var inputIndex = codec.dequeueInputBuffer(10_000)
        if (inputIndex >= 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, frames.size * frameDurationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }

        drainEncoder(codec, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = true)
        if (muxerStarted) muxer.stop()
        muxer.release()
        codec.stop()
        codec.release()
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        trackIndex: Int,
        muxerStarted: Boolean,
        endOfStream: Boolean = false,
    ): Pair<Int, Boolean> {
        var index = trackIndex
        var started = muxerStarted
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return index to started
                    break
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!started) {
                        index = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        started = true
                    }
                }
                outputIndex >= 0 -> {
                    val data = codec.getOutputBuffer(outputIndex) ?: break
                    if (bufferInfo.size > 0 && started) {
                        muxer.writeSampleData(index, data, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return index to started
                }
            }
        }
        return index to started
    }

    private fun bitmapToNv21(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val yuv = ByteArray(width * height * 3 / 2)
        var yIndex = 0
        var uvIndex = width * height
        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = argb[j * width + i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                }
            }
        }
        return yuv
    }
}
