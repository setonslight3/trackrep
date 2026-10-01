package com.setons.trackrep.review

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.setons.trackrep.pose.PoseAngleCalculator
import com.setons.trackrep.pose.TrackedLandmark
import com.setons.trackrep.pose.TrackedPose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

object SessionTelemetryHelper {

    private const val TAG = "SessionTelemetryHelper"

    private fun getTelemetryDir(context: Context): File {
        val dir = File(context.filesDir, "telemetry")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTelemetryFile(context: Context, sessionId: String): File {
        return File(getTelemetryDir(context), "${sessionId}.json")
    }

    /**
     * Persists recorded pose telemetry and completed rep timestamps to local disk storage as JSON.
     */
    fun saveTelemetry(
        context: Context,
        sessionId: String,
        poses: List<TimestampedPose>,
        completedRepTimestamps: List<Long> = emptyList()
    ): Boolean {
        if (poses.isEmpty()) return false

        return try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("sessionId", sessionId)

            val repsArray = JSONArray()
            for (ts in completedRepTimestamps) {
                repsArray.put(ts)
            }
            root.put("completedRepTimestamps", repsArray)

            val framesArray = JSONArray()
            for (item in poses) {
                val frameObj = JSONObject()
                frameObj.put("t", item.timestampMs)
                frameObj.put("w", item.pose.imageWidth)
                frameObj.put("h", item.pose.imageHeight)
                frameObj.put("v", item.pose.isTrackingValid)

                val lmArray = JSONArray()
                for ((type, lm) in item.pose.landmarks) {
                    val lmObj = JSONArray()
                    lmObj.put(type)
                    lmObj.put(lm.x.toDouble())
                    lmObj.put(lm.y.toDouble())
                    lmObj.put(lm.inFrameLikelihood.toDouble())
                    lmArray.put(lmObj)
                }
                frameObj.put("lm", lmArray)
                framesArray.put(frameObj)
            }
            root.put("frames", framesArray)

            val targetFile = getTelemetryFile(context, sessionId)
            val tempFile = File(targetFile.parentFile, "${sessionId}.tmp")
            tempFile.writeText(root.toString(), Charsets.UTF_8)
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
            Log.d(TAG, "Saved ${poses.size} telemetry frames for session $sessionId to ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving telemetry for session $sessionId", e)
            false
        }
    }

    /**
     * Loads persisted pose telemetry from local storage if available.
     */
    fun loadTelemetry(
        context: Context,
        sessionId: String
    ): Pair<List<TimestampedPose>, List<Long>>? {
        val file = getTelemetryFile(context, sessionId)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val content = file.readText(Charsets.UTF_8)
            val root = JSONObject(content)

            val repsList = mutableListOf<Long>()
            val repsArray = root.optJSONArray("completedRepTimestamps")
            if (repsArray != null) {
                for (i in 0 until repsArray.length()) {
                    repsList.add(repsArray.getLong(i))
                }
            }

            val framesArray = root.optJSONArray("frames") ?: return null
            val posesList = ArrayList<TimestampedPose>(framesArray.length())

            for (i in 0 until framesArray.length()) {
                val frameObj = framesArray.getJSONObject(i)
                val timestampMs = frameObj.getLong("t")
                val width = frameObj.optInt("w", 480)
                val height = frameObj.optInt("h", 640)
                val isValid = frameObj.optBoolean("v", true)

                val lmMap = mutableMapOf<Int, TrackedLandmark>()
                val lmArray = frameObj.optJSONArray("lm")
                if (lmArray != null) {
                    for (j in 0 until lmArray.length()) {
                        val singleLm = lmArray.getJSONArray(j)
                        val type = singleLm.getInt(0)
                        val x = singleLm.getDouble(1).toFloat()
                        val y = singleLm.getDouble(2).toFloat()
                        val likelihood = singleLm.getDouble(3).toFloat()
                        lmMap[type] = TrackedLandmark(type, x, y, likelihood)
                    }
                }

                val trackedPose = buildTrackedPoseFromLandmarks(lmMap, width, height, isValid)
                posesList.add(TimestampedPose(timestampMs, trackedPose))
            }

            Log.d(TAG, "Loaded ${posesList.size} poses from telemetry for session $sessionId")
            posesList to repsList
        } catch (e: Exception) {
            Log.e(TAG, "Error loading telemetry for session $sessionId", e)
            null
        }
    }

    /**
     * Extracts actual real body skeleton poses from an existing video file on disk.
     * Uses Android's MediaMetadataRetriever and Google ML Kit Pose Detection.
     * Caches the extracted poses immediately to disk so subsequent reviews are instant.
     */
    suspend fun extractPosesFromVideo(
        context: Context,
        videoFile: File,
        sessionId: String,
        onProgress: (Float) -> Unit = {}
    ): List<TimestampedPose> = withContext(Dispatchers.IO) {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            return@withContext emptyList()
        }

        val retriever = MediaMetadataRetriever()
        var detector: PoseDetector? = null

        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val totalDurationMs = durationStr?.toLongOrNull()?.coerceAtLeast(1000L) ?: 10000L

            val options = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
            detector = PoseDetection.getClient(options)

            val extracted = mutableListOf<TimestampedPose>()
            val sampleIntervalMs = 120L // ~8.3 fps extraction for fast processing and smooth motion
            var currentMs = 0L

            while (currentMs <= totalDurationMs) {
                val frameTimeUs = currentMs * 1000L
                val bitmap: Bitmap? = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            frameTimeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                            360,
                            640
                        )
                    } else {
                        retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    }
                } catch (e: Exception) {
                    null
                }

                if (bitmap != null) {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val pose = detector.processSuspending(inputImage)
                    if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {
                        val tracked = mapPoseToTracked(pose, bitmap.width, bitmap.height)
                        extracted.add(TimestampedPose(currentMs, tracked))
                    }
                }

                val progress = (currentMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                onProgress(progress)
                currentMs += sampleIntervalMs
            }

            if (extracted.isNotEmpty()) {
                saveTelemetry(context, sessionId, extracted, emptyList())
            }

            extracted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract poses from video: ${videoFile.name}", e)
            emptyList()
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
            try {
                detector?.close()
            } catch (ignored: Exception) {}
        }
    }

    private suspend fun PoseDetector.processSuspending(image: InputImage): Pose? =
        suspendCancellableCoroutine { continuation ->
            process(image)
                .addOnSuccessListener { pose ->
                    if (continuation.isActive) continuation.resume(pose)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }

    fun mapPoseToTracked(pose: Pose, imageWidth: Int, imageHeight: Int): TrackedPose {
        val landmarksMap = mutableMapOf<Int, TrackedLandmark>()
        for (landmark in pose.allPoseLandmarks) {
            val normX = landmark.position.x / imageWidth.toFloat()
            val normY = landmark.position.y / imageHeight.toFloat()
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

        return buildTrackedPoseFromLandmarks(landmarksMap, imageWidth, imageHeight, hasKeyPoints)
    }

    private fun buildTrackedPoseFromLandmarks(
        landmarksMap: Map<Int, TrackedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
        isTrackingValid: Boolean
    ): TrackedPose {
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
            landmarksMap[PoseLandmark.NOSE],
            landmarksMap[PoseLandmark.LEFT_SHOULDER],
            landmarksMap[PoseLandmark.LEFT_HIP]
        )

        return TrackedPose(
            landmarks = landmarksMap,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            isTrackingValid = isTrackingValid,
            leftElbowAngle = leftElbowAngle,
            rightElbowAngle = rightElbowAngle,
            hipAlignmentAngle = hipAngle,
            leftKneeAngle = leftKneeAngle,
            rightKneeAngle = rightKneeAngle,
            torsoLeanAngle = torsoLeanAngle
        )
    }
}
