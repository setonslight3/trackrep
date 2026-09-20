package com.setons.trackrep.pose

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseDetectorProcessor(
    private val onPoseDetected: (TrackedPose) -> Unit
) : ImageAnalysis.Analyzer {

    // Optimized for Infinix Smart 9: STREAM_MODE uses lightweight fast neural pipeline
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector: PoseDetector = PoseDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Dimensions accounting for frame rotation
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val frameWidth = if (isRotated) imageProxy.height else imageProxy.width
        val frameHeight = if (isRotated) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                val landmarksMap = mutableMapOf<Int, TrackedLandmark>()
                for (landmark in pose.allPoseLandmarks) {
                    val normX = landmark.position.x / frameWidth.toFloat()
                    val normY = landmark.position.y / frameHeight.toFloat()
                    landmarksMap[landmark.landmarkType] = TrackedLandmark(
                        type = landmark.landmarkType,
                        x = normX.coerceIn(0f, 1f),
                        y = normY.coerceIn(0f, 1f),
                        inFrameLikelihood = landmark.inFrameLikelihood
                    )
                }

                val hasKeyPoints = landmarksMap.containsKey(PoseLandmark.LEFT_SHOULDER) &&
                        landmarksMap.containsKey(PoseLandmark.RIGHT_SHOULDER) &&
                        landmarksMap.containsKey(PoseLandmark.LEFT_HIP) &&
                        landmarksMap.containsKey(PoseLandmark.RIGHT_HIP)

                // Calculate joint angles
                val leftElbowAngle = PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.LEFT_SHOULDER],
                    landmarksMap[PoseLandmark.LEFT_ELBOW],
                    landmarksMap[PoseLandmark.LEFT_WRIST]
                )

                val rightElbowAngle = PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.RIGHT_SHOULDER],
                    landmarksMap[PoseLandmark.RIGHT_ELBOW],
                    landmarksMap[PoseLandmark.RIGHT_WRIST]
                )

                val hipAngle = PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.LEFT_SHOULDER],
                    landmarksMap[PoseLandmark.LEFT_HIP],
                    landmarksMap[PoseLandmark.LEFT_ANKLE]
                ) ?: PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.RIGHT_SHOULDER],
                    landmarksMap[PoseLandmark.RIGHT_HIP],
                    landmarksMap[PoseLandmark.RIGHT_ANKLE]
                )

                val leftKneeAngle = PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.LEFT_HIP],
                    landmarksMap[PoseLandmark.LEFT_KNEE],
                    landmarksMap[PoseLandmark.LEFT_ANKLE]
                )

                val rightKneeAngle = PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.RIGHT_HIP],
                    landmarksMap[PoseLandmark.RIGHT_KNEE],
                    landmarksMap[PoseLandmark.RIGHT_ANKLE]
                )

                val torsoLeanAngle = PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.LEFT_SHOULDER],
                    landmarksMap[PoseLandmark.LEFT_HIP],
                    landmarksMap[PoseLandmark.LEFT_KNEE]
                ) ?: PoseAngleCalculator.calculateAngle(
                    landmarksMap[PoseLandmark.RIGHT_SHOULDER],
                    landmarksMap[PoseLandmark.RIGHT_HIP],
                    landmarksMap[PoseLandmark.RIGHT_KNEE]
                )

                // Evaluate real-time form indicators
                val issues = mutableListOf<String>()
                if (hipAngle != null && hipAngle < 155.0) {
                    issues.add("Keep core tight - hips sagging")
                }
                if (leftElbowAngle != null && rightElbowAngle != null) {
                    if (leftElbowAngle < 55.0 || rightElbowAngle < 55.0) {
                        issues.add("Elbows over-bent")
                    }
                }

                onPoseDetected(
                    TrackedPose(
                        landmarks = landmarksMap,
                        imageWidth = frameWidth,
                        imageHeight = frameHeight,
                        isTrackingValid = hasKeyPoints,
                        leftElbowAngle = leftElbowAngle,
                        rightElbowAngle = rightElbowAngle,
                        hipAlignmentAngle = hipAngle,
                        leftKneeAngle = leftKneeAngle,
                        rightKneeAngle = rightKneeAngle,
                        torsoLeanAngle = torsoLeanAngle,
                        formIssues = issues
                    )
                )
            }
            .addOnFailureListener {
                // Return empty pose on failure
                onPoseDetected(
                    TrackedPose(
                        landmarks = emptyMap(),
                        imageWidth = frameWidth,
                        imageHeight = frameHeight,
                        isTrackingValid = false
                    )
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun close() {
        detector.close()
    }
}
