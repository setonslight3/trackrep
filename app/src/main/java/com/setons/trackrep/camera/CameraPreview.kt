package com.setons.trackrep.camera

import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.setons.trackrep.pose.PoseDetectorProcessor
import com.setons.trackrep.pose.TrackedPose
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    lens: CameraLens,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onPoseDetected: (TrackedPose) -> Unit = {},
    onVideoCaptureReady: (VideoCapture<Recorder>?) -> Unit = {},
    onCameraReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scaleType = if (isFullscreen) PreviewView.ScaleType.FIT_CENTER else PreviewView.ScaleType.FILL_CENTER
    val targetAspectRatio = if (isFullscreen) AspectRatio.RATIO_16_9 else null

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.scaleType = scaleType
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(scaleType) {
        previewView.scaleType = scaleType
        onDispose { }
    }

    DisposableEffect(lens, targetAspectRatio) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val poseProcessor = PoseDetectorProcessor { pose ->
            onPoseDetected(pose)
        }

        val qualitySelector = QualitySelector.from(Quality.SD)
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewBuilder = Preview.Builder()
            if (targetAspectRatio != null) {
                previewBuilder.setTargetAspectRatio(targetAspectRatio)
            }
            val preview = previewBuilder.build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysisBuilder = ImageAnalysis.Builder()
            if (targetAspectRatio != null) {
                analysisBuilder.setTargetAspectRatio(targetAspectRatio)
            }
            val imageAnalysis = analysisBuilder
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor, poseProcessor)

            try {
                cameraProvider.unbindAll()
                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        lens.selector,
                        preview,
                        imageAnalysis,
                        videoCapture
                    )
                    onVideoCaptureReady(videoCapture)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        lens.selector,
                        preview,
                        imageAnalysis
                    )
                    onVideoCaptureReady(null)
                }
                onCameraReady()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, executor)

        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                poseProcessor.close()
                analysisExecutor.shutdown()
                onVideoCaptureReady(null)
            } catch (e: Exception) {
                // Ignore during disposal
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}
