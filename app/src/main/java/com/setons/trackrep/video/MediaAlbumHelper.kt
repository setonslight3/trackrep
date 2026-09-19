package com.setons.trackrep.video

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaAlbumHelper {

    /**
     * Saves a recorded MP4 video to the phone's public Movies/TrackRep album in MediaStore,
     * making it immediately accessible in Gallery, Google Photos, or file managers.
     */
    fun saveVideoToPhoneAlbum(
        context: Context,
        sourceFile: File,
        exerciseName: String,
        isMotionSticksOnly: Boolean = false
    ): Boolean {
        if (!sourceFile.exists()) {
            Toast.makeText(context, "Recording file not found on disk", Toast.LENGTH_SHORT).show()
            return false
        }

        val suffix = if (isMotionSticksOnly) "MotionSticks" else "Set"
        val cleanName = exerciseName.replace(" ", "_")
        val fileName = "TrackRep_${cleanName}_${suffix}_${System.currentTimeMillis()}.mp4"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/TrackRep")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri: Uri? = resolver.insert(collection, contentValues)

                if (itemUri != null) {
                    resolver.openOutputStream(itemUri).use { outStream ->
                        FileInputStream(sourceFile).use { inStream ->
                            inStream.copyTo(outStream!!)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)

                    Toast.makeText(context, "Saved to Gallery in Movies/TrackRep!", Toast.LENGTH_LONG).show()
                    true
                } else {
                    false
                }
            } else {
                // Fallback for older Android versions
                val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "TrackRep")
                if (!moviesDir.exists()) moviesDir.mkdirs()

                val destFile = File(moviesDir, fileName)
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )

                Toast.makeText(context, "Saved to Movies/TrackRep!", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Encodes recorded motion sticks telemetry into an MP4 and saves it directly to Movies/TrackRep.
     * Guaranteed 100% privacy: no user face, body or room recorded.
     */
    suspend fun exportAndSaveMotionSticksToAlbum(
        context: Context,
        exerciseName: String,
        poses: List<com.setons.trackrep.review.TimestampedPose>,
        durationSeconds: Int,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        if (poses.isEmpty()) {
            Toast.makeText(context, "No pose telemetry to export", Toast.LENGTH_SHORT).show()
            return false
        }

        val cacheFile = File(context.cacheDir, "motion_sticks_temp_${System.currentTimeMillis()}.mp4")
        val success = MotionSticksVideoExporter.exportMotionSticksVideo(
            outputFile = cacheFile,
            poses = poses,
            durationSeconds = durationSeconds,
            onProgress = onProgress
        )

        return if (success && cacheFile.exists()) {
            val saved = saveVideoToPhoneAlbum(
                context = context,
                sourceFile = cacheFile,
                exerciseName = exerciseName,
                isMotionSticksOnly = true
            )
            cacheFile.delete()
            saved
        } else {
            Toast.makeText(context, "Failed to encode motion sticks video", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
