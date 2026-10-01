package com.setons.trackrep.review

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import android.content.Intent
import androidx.core.content.FileProvider
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.video.SaveVideoChoiceDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.setons.trackrep.video.MediaAlbumHelper
import kotlinx.coroutines.delay
import java.io.File

enum class PlaybackDisplayMode {
    REAL_VIDEO,
    MOTION_STICKS_ONLY
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun SessionPlaybackScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var session by remember(sessionId) {
        mutableStateOf(
            SessionReviewRepository.getSessionById(sessionId) ?: SessionReviewRepository.getAllSessions().firstOrNull()
        )
    }

    var isExtractingPoses by remember { mutableStateOf(false) }
    var extractionProgress by remember { mutableFloatStateOf(0f) }
    var showSaveChoiceDialog by remember { mutableStateOf(false) }

    var videoFile by remember(session?.videoPath) {
        mutableStateOf(session?.videoPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 })
    }

    LaunchedEffect(sessionId) {
        withContext(Dispatchers.IO) {
            val db = TrackRepDatabase.getDatabase(context)
            val entity = db.sessionDao().getSessionById(sessionId)
            val memorySession = SessionReviewRepository.getSessionById(sessionId)

            val targetId = entity?.id ?: memorySession?.id ?: sessionId
            val exName = entity?.exerciseName ?: memorySession?.exerciseName ?: "Workout"
            val durSec = entity?.durationSeconds ?: memorySession?.durationSeconds ?: 10
            val vPath = entity?.videoPath ?: memorySession?.videoPath
            val reps = entity?.totalValidReps ?: memorySession?.repCount ?: 0
            val date = entity?.dateString ?: memorySession?.dateString ?: ""

            // 1. Try disk telemetry first
            val diskTelemetry = SessionTelemetryHelper.loadTelemetry(context, targetId)
            val realPoses: List<TimestampedPose>
            val repTimestamps: List<Long>

            if (diskTelemetry != null && diskTelemetry.first.isNotEmpty()) {
                realPoses = diskTelemetry.first
                repTimestamps = diskTelemetry.second
            } else if (memorySession != null && memorySession.recordedPoses.isNotEmpty() && memorySession.id != "sample_session_1") {
                realPoses = memorySession.recordedPoses
                repTimestamps = memorySession.completedRepTimestamps
                // Cache to disk
                SessionTelemetryHelper.saveTelemetry(context, targetId, realPoses, repTimestamps)
            } else if (vPath != null && File(vPath).exists() && File(vPath).length() > 0) {
                // Extract directly from user's video file!
                withContext(Dispatchers.Main) {
                    isExtractingPoses = true
                    extractionProgress = 0f
                }
                realPoses = SessionTelemetryHelper.extractPosesFromVideo(
                    context = context,
                    videoFile = File(vPath),
                    sessionId = targetId,
                    onProgress = { p -> extractionProgress = p }
                )
                repTimestamps = emptyList()
                withContext(Dispatchers.Main) {
                    isExtractingPoses = false
                }
            } else {
                realPoses = SessionReviewRepository.generatePosesForExercise(exName, durSec)
                repTimestamps = emptyList()
            }

            val loadedSession = RecordedWorkoutSession(
                id = targetId,
                exerciseName = exName,
                videoPath = vPath,
                durationSeconds = durSec,
                repCount = reps,
                dateString = date,
                detectedFlaws = memorySession?.detectedFlaws ?: emptyList(),
                recordedPoses = realPoses,
                completedRepTimestamps = repTimestamps
            )
            SessionReviewRepository.addSession(loadedSession)
            withContext(Dispatchers.Main) {
                session = loadedSession
                if (vPath != null && File(vPath).exists() && File(vPath).length() > 0) {
                    videoFile = File(vPath)
                }
            }
        }
    }

    if (session == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Session not found", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val currentSession = session!!

    var displayMode by remember {
        mutableStateOf(
            if (currentSession.videoPath != null && File(currentSession.videoPath).exists()) PlaybackDisplayMode.REAL_VIDEO
            else PlaybackDisplayMode.MOTION_STICKS_ONLY
        )
    }

    var selectedFlaw by remember { mutableStateOf(currentSession.detectedFlaws.firstOrNull()) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableFloatStateOf(0f) }
    val totalDurationMs = (currentSession.durationSeconds * 1000).toFloat().coerceAtLeast(1000f)

    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    // ExoPlayer for real video file if available

    LaunchedEffect(currentSession.videoPath) {
        val path = currentSession.videoPath
        if (path != null) {
            val f = File(path)
            if (!f.exists() || f.length() == 0L) {
                for (i in 0 until 10) {
                    delay(300)
                    if (f.exists() && f.length() > 0) {
                        videoFile = f
                        displayMode = PlaybackDisplayMode.REAL_VIDEO
                        break
                    }
                }
            } else {
                videoFile = f
            }
        }
    }

    fun saveWorkoutMedia(forceMotionSticks: Boolean = false) {
        val targetFile = videoFile ?: currentSession.videoPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
        val shouldExportMotionSticks = forceMotionSticks || (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY && targetFile == null) || targetFile == null
        if (shouldExportMotionSticks) {
            coroutineScope.launch {
                isExporting = true
                exportProgress = 0f
                val poses = if (currentSession.recordedPoses.isNotEmpty()) {
                    currentSession.recordedPoses
                } else {
                    SessionReviewRepository.generatePosesForExercise(currentSession.exerciseName, currentSession.durationSeconds)
                }
                MediaAlbumHelper.exportAndSaveMotionSticksToAlbum(
                    context = context,
                    exerciseName = currentSession.exerciseName,
                    poses = poses,
                    durationSeconds = currentSession.durationSeconds,
                    completedRepTimestamps = currentSession.completedRepTimestamps,
                    onProgress = { p -> exportProgress = p }
                )
                isExporting = false
            }
        } else {
            targetFile?.let { f ->
                MediaAlbumHelper.saveVideoToPhoneAlbum(
                    context = context,
                    sourceFile = f,
                    exerciseName = currentSession.exerciseName,
                    isMotionSticksOnly = false
                )
            }
        }
    }

    fun shareWorkoutVideo() {
        val targetFile = videoFile ?: currentSession.videoPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
        if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Save or Export Workout Video"))
            } catch (e: Exception) {
                MediaAlbumHelper.saveVideoToPhoneAlbum(context, targetFile, currentSession.exerciseName)
            }
        } else {
            saveWorkoutMedia(forceMotionSticks = true)
        }
    }

    val exoPlayer = remember(videoFile) {
        if (videoFile != null) {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
                setMediaItem(mediaItem)
                prepare()
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
            }
        } else {
            null
        }
    }

    // Scrubber sync loop
    LaunchedEffect(isPlaying, exoPlayer, displayMode) {
        while (true) {
            if (exoPlayer != null && displayMode == PlaybackDisplayMode.REAL_VIDEO) {
                currentPositionMs = exoPlayer.currentPosition.toFloat()
            } else if (isPlaying) {
                if (currentPositionMs >= totalDurationMs) {
                    currentPositionMs = 0f
                    isPlaying = false
                } else {
                    currentPositionMs += 100f
                }
            }
            delay(100)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    val scrollState = rememberScrollState()

    if (isExporting) {
        Dialog(onDismissRequest = {}) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Exporting Motion Sticks (Privacy Mode)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rendering motion skeleton tracking onto clean canvas. Zero face, body, or room background recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "${(exportProgress * 100).toInt()}% Encoded",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentSession.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${currentSession.repCount} Reps • ${currentSession.durationSeconds}s • ${currentSession.dateString}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = {
                // Save to Phone Album Button
                IconButton(
                    onClick = { showSaveChoiceDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Phone Album",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // View Mode Selector: Real Video vs Motion Sticks Only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { displayMode = PlaybackDisplayMode.REAL_VIDEO },
                    shape = RoundedCornerShape(10.dp),
                    color = if (displayMode == PlaybackDisplayMode.REAL_VIDEO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (displayMode == PlaybackDisplayMode.REAL_VIDEO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (displayMode == PlaybackDisplayMode.REAL_VIDEO) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Real Video",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (displayMode == PlaybackDisplayMode.REAL_VIDEO) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    onClick = { displayMode = PlaybackDisplayMode.MOTION_STICKS_ONLY },
                    shape = RoundedCornerShape(10.dp),
                    color = if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Motion Sticks Only",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Video / Motion Sticks Player Display
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (displayMode) {
                        PlaybackDisplayMode.REAL_VIDEO -> {
                            if (exoPlayer != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF14120E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No Raw Video File Attached",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Switch to 'Motion Sticks Only' above to view the full motion telemetry animation.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        PlaybackDisplayMode.MOTION_STICKS_ONLY -> {
                            if (isExtractingPoses) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Analyzing Motion Sticks from Video...",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Extracting your exact body skeleton telemetry frame-by-frame.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        LinearProgressIndicator(
                                            progress = { extractionProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        )
                                        Text(
                                            text = "${(extractionProgress * 100).toInt()}% Extracted",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                MotionSticksCanvas(
                                    poses = currentSession.recordedPoses,
                                    currentPositionMs = currentPositionMs.toLong(),
                                    completedRepTimestamps = currentSession.completedRepTimestamps,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Single Play / Pause Button Overlay in Corner
                    IconButton(
                        onClick = {
                            if (exoPlayer != null && displayMode == PlaybackDisplayMode.REAL_VIDEO) {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            } else {
                                isPlaying = !isPlaying
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Timestamp overlay
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        val currentSec = (currentPositionMs / 1000).toInt()
                        val totalSec = currentSession.durationSeconds
                        Text(
                            text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Save & Download Action Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasRealVideo = videoFile != null || (currentSession.videoPath != null && File(currentSession.videoPath).exists() && File(currentSession.videoPath).length() > 0)

                Button(
                    onClick = { showSaveChoiceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Video to Gallery",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasRealVideo) {
                    OutlinedButton(
                        onClick = { shareWorkoutVideo() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share Video to Device",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (showSaveChoiceDialog) {
                val hasRealVideo = videoFile != null || (currentSession.videoPath != null && File(currentSession.videoPath).exists() && File(currentSession.videoPath).length() > 0)
                SaveVideoChoiceDialog(
                    exerciseName = currentSession.exerciseName,
                    hasRawVideo = hasRealVideo,
                    onSaveRaw = {
                        val target = videoFile ?: currentSession.videoPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                        if (target != null) {
                            MediaAlbumHelper.saveVideoToPhoneAlbum(
                                context = context,
                                sourceFile = target,
                                exerciseName = currentSession.exerciseName,
                                isMotionSticksOnly = false
                            )
                        }
                    },
                    onSaveMotionSticks = {
                        coroutineScope.launch {
                            isExporting = true
                            exportProgress = 0f
                            val poses = if (currentSession.recordedPoses.isNotEmpty()) {
                                currentSession.recordedPoses
                            } else {
                                SessionReviewRepository.generatePosesForExercise(currentSession.exerciseName, currentSession.durationSeconds)
                            }
                            MediaAlbumHelper.exportAndSaveMotionSticksToAlbum(
                                context = context,
                                exerciseName = currentSession.exerciseName,
                                poses = poses,
                                durationSeconds = currentSession.durationSeconds,
                                completedRepTimestamps = currentSession.completedRepTimestamps,
                                onProgress = { p -> exportProgress = p }
                            )
                            isExporting = false
                        }
                    },
                    onSaveBoth = {
                        val target = videoFile ?: currentSession.videoPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
                        if (target != null) {
                            MediaAlbumHelper.saveVideoToPhoneAlbum(
                                context = context,
                                sourceFile = target,
                                exerciseName = currentSession.exerciseName,
                                isMotionSticksOnly = false
                            )
                        }
                        coroutineScope.launch {
                            isExporting = true
                            exportProgress = 0f
                            val poses = if (currentSession.recordedPoses.isNotEmpty()) {
                                currentSession.recordedPoses
                            } else {
                                SessionReviewRepository.generatePosesForExercise(currentSession.exerciseName, currentSession.durationSeconds)
                            }
                            MediaAlbumHelper.exportAndSaveMotionSticksToAlbum(
                                context = context,
                                exerciseName = currentSession.exerciseName,
                                poses = poses,
                                durationSeconds = currentSession.durationSeconds,
                                completedRepTimestamps = currentSession.completedRepTimestamps,
                                onProgress = { p -> exportProgress = p }
                            )
                            isExporting = false
                        }
                    },
                    onDismiss = { showSaveChoiceDialog = false }
                )
            }

            // Interactive Scrubber & Timeline with Form Flaw Markers
            Column {
                Slider(
                    value = currentPositionMs.coerceIn(0f, totalDurationMs),
                    onValueChange = { newVal ->
                        currentPositionMs = newVal
                        exoPlayer?.seekTo(newVal.toLong())
                    },
                    valueRange = 0f..totalDurationMs,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Timeline Flaw Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentSession.detectedFlaws.forEach { flaw ->
                        val isSelected = selectedFlaw == flaw
                        val flawSec = flaw.timestampMs / 1000
                        Surface(
                            onClick = {
                                selectedFlaw = flaw
                                currentPositionMs = flaw.timestampMs.toFloat()
                                exoPlayer?.seekTo(flaw.timestampMs)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFF5252).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFF5252) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", flawSec / 60, flawSec % 60),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = flaw.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Form Coaching & Correction Card
            if (selectedFlaw != null) {
                val flaw = selectedFlaw!!
                OutlinedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252)
                            )
                            Text(
                                text = "Form Flaw: ${flaw.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = flaw.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        // Coaching Correction Tip Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Coaching Adjustment:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = flaw.correctionTip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Generous bottom spacer so content is fully scrollable above system navigation bar
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}
