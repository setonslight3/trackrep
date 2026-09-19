package com.setons.trackrep.video

import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.io.File

object VideoRecorderManager {
    private var activeRecording: Recording? = null

    fun startRecording(
        context: Context,
        videoCapture: VideoCapture<Recorder>?,
        outputFile: File,
        onFinalized: (File?) -> Unit
    ): Boolean {
        if (videoCapture == null) {
            return false
        }

        return try {
            val fileOptions = FileOutputOptions.Builder(outputFile).build()
            val executor = ContextCompat.getMainExecutor(context)

            activeRecording = videoCapture.output
                .prepareRecording(context, fileOptions)
                .start(executor) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            if (!event.hasError()) {
                                onFinalized(outputFile)
                            } else {
                                onFinalized(null)
                            }
                            activeRecording = null
                        }
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun isRecordingActive(): Boolean = activeRecording != null
}
