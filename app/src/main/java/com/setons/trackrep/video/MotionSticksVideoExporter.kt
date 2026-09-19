package com.setons.trackrep.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.review.TimestampedPose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

object MotionSticksVideoExporter {

    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
    private const val FRAME_RATE = 15
    private const val BIT_RATE = 1_500_000 // 1.5 Mbps
    private const val I_FRAME_INTERVAL = 1
    private const val TIMEOUT_US = 10_000L

    /**
     * Renders recorded skeleton telemetry onto a pitch-black canvas and encodes it into
     * an MP4 video file using Android's hardware MediaCodec & MediaMuxer.
     * 100% on-device, zero network transmission, full privacy protection.
     */
    suspend fun exportMotionSticksVideo(
        outputFile: File,
        poses: List<TimestampedPose>,
        durationSeconds: Int,
        completedRepTimestamps: List<Long> = emptyList(),
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val width = 480  // 16:9 SD, guaranteed multiple of 16
        val height = 848 // 16:9 SD, guaranteed multiple of 16
        val totalDurationSec = durationSeconds.coerceIn(3, 120)
        val totalFrames = totalDurationSec * FRAME_RATE

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            // Initialize MediaCodec encoder
            val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            codec = encoder

            val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            // Initialize MediaMuxer
            val mm = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mm

            // Canvas & drawing primitives
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val argbPixels = IntArray(width * height)
            val yuvBuffer = ByteArray(width * height * 3 / 2)

            val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFD4AF37.toInt() // Luxury Gold
                strokeWidth = 6f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            val goldGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x55D4AF37.toInt()
                strokeWidth = 14f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

            // Green flash paints for completed reps
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF00E676.toInt() // Radiant Neon Green
                strokeWidth = 8f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            val greenGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x8869F0AE.toInt()
                strokeWidth = 18f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            val greenJointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xAA00E676.toInt()
                style = Paint.Style.FILL
            }
            val flashVignettePaint = Paint().apply {
                color = 0x2500E676.toInt()
                style = Paint.Style.FILL
            }

            val jointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x88D4AF37.toInt()
                style = Paint.Style.FILL
            }
            val jointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFD4AF37.toInt()
                textSize = 18f
                style = Paint.Style.FILL
                isFakeBoldText = true
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xCCFFFFFF.toInt()
                textSize = 14f
                style = Paint.Style.FILL
            }

            val bufferInfo = MediaCodec.BufferInfo()

            for (frame in 0 until totalFrames) {
                val timestampMs = (frame * 1000L) / FRAME_RATE
                val presentationTimeUs = frame * 1_000_000L / FRAME_RATE

                // Find closest pose
                val currentPose = poses.minByOrNull { abs(it.timestampMs - timestampMs) }?.pose

                // Render frame onto pitch black canvas
                canvas.drawColor(android.graphics.Color.BLACK)

                // Check if this frame is near a completed rep
                val isRepFlash = completedRepTimestamps.any { abs(it - timestampMs) <= 450L }
                if (isRepFlash) {
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashVignettePaint)
                }

                val activeLimbPaint = if (isRepFlash) greenPaint else goldPaint
                val activeLimbGlowPaint = if (isRepFlash) greenGlowPaint else goldGlowPaint
                val activeJointOuter = if (isRepFlash) greenJointOuterPaint else jointOuterPaint

                if (currentPose != null && currentPose.isTrackingValid) {
                    fun drawLimb(startType: Int, endType: Int) {
                        val s = currentPose.landmarks[startType]
                        val e = currentPose.landmarks[endType]
                        if (s != null && e != null && s.inFrameLikelihood >= 0.35f && e.inFrameLikelihood >= 0.35f) {
                            val sx = s.x * width
                            val sy = s.y * height
                            val ex = e.x * width
                            val ey = e.y * height
                            canvas.drawLine(sx, sy, ex, ey, activeLimbGlowPaint)
                            canvas.drawLine(sx, sy, ex, ey, activeLimbPaint)
                        }
                    }

                    // Draw body skeleton
                    drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
                    drawLimb(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
                    drawLimb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
                    drawLimb(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

                    drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
                    drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
                    drawLimb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
                    drawLimb(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)

                    drawLimb(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
                    drawLimb(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
                    drawLimb(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
                    drawLimb(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

                    // Draw joint nodes
                    val joints = listOf(
                        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                        PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                        PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
                        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                        PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
                    )
                    val nodeRadius = if (isRepFlash) 13f else 10f
                    for (jt in joints) {
                        val lm = currentPose.landmarks[jt]
                        if (lm != null && lm.inFrameLikelihood >= 0.35f) {
                            val cx = lm.x * width
                            val cy = lm.y * height
                            canvas.drawCircle(cx, cy, nodeRadius, activeJointOuter)
                            canvas.drawCircle(cx, cy, 4f, jointInnerPaint)
                        }
                    }
                }

                // Watermark / Brand Header
                canvas.drawText("TRACKREP • MOTION PRIVACY REPLAY", 20f, 44f, titlePaint)
                val curSec = (timestampMs / 1000).toInt()
                val totalSec = totalDurationSec
                canvas.drawText(
                    String.format("%02d:%02d / %02d:%02d  •  100%% Private Telemetry", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60),
                    20f,
                    70f,
                    subPaint
                )

                // Extract ARGB and convert to NV12
                bitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                encodeYUV420SP(yuvBuffer, argbPixels, width, height)

                // Queue input buffer
                var queued = false
                var waitCount = 0
                while (!queued && waitCount < 10) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(yuvBuffer)
                        encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, presentationTimeUs, 0)
                        queued = true
                    } else {
                        waitCount++
                        // Drain output buffer while waiting
                        drainOutput(encoder, mm, bufferInfo, false) { idx, started ->
                            trackIndex = idx
                            muxerStarted = started
                        }
                    }
                }

                drainOutput(encoder, mm, bufferInfo, false) { idx, started ->
                    trackIndex = idx
                    muxerStarted = started
                }

                onProgress((frame + 1).toFloat() / totalFrames.toFloat())
            }

            // End of stream
            var eosQueued = false
            while (!eosQueued) {
                val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    encoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        totalFrames * 1_000_000L / FRAME_RATE,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    eosQueued = true
                } else {
                    drainOutput(encoder, mm, bufferInfo, false) { idx, started ->
                        trackIndex = idx
                        muxerStarted = started
                    }
                }
            }

            // Drain remaining frames until EOS
            drainOutput(encoder, mm, bufferInfo, true) { idx, started ->
                trackIndex = idx
                muxerStarted = started
            }

            bitmap.recycle()
            outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                // Ignore during cleanup
            }
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (e: Exception) {
                // Ignore during cleanup
            }
        }
    }

    private fun drainOutput(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        onMuxerReady: (Int, Boolean) -> Unit
    ) {
        var trackIdx = -1
        var muxerStarted = false

        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                trackIdx = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
                onMuxerReady(trackIdx, muxerStarted)
            } else if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && outputBuffer != null) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIdx.coerceAtLeast(0), outputBuffer, bufferInfo)
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index++]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                // RGB to YUV formula
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv420sp[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    // NV12 format (interleaved U, then V)
                    yuv420sp[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv420sp[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }
}
