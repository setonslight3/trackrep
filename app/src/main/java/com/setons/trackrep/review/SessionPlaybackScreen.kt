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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
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
    val session = remember(sessionId) {
        SessionReviewRepository.getSessionById(sessionId) ?: SessionReviewRepository.getAllSessions().firstOrNull()
    }

    if (session == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Session not found", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    var displayMode by remember {
        mutableStateOf(
            if (session.videoPath != null && File(session.videoPath).exists()) PlaybackDisplayMode.REAL_VIDEO
            else PlaybackDisplayMode.MOTION_STICKS_ONLY
        )
    }

    var selectedFlaw by remember { mutableStateOf(session.detectedFlaws.firstOrNull()) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableFloatStateOf(0f) }
    val totalDurationMs = (session.durationSeconds * 1000).toFloat().coerceAtLeast(1000f)

    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    // ExoPlayer for real video file if available
    val videoFile = remember(session.videoPath) {
        session.videoPath?.let { File(it) }?.takeIf { it.exists() }
    }

    fun saveWorkoutMedia(forceMotionSticks: Boolean = false) {
        val shouldExportMotionSticks = forceMotionSticks || displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY || videoFile == null
        if (shouldExportMotionSticks) {
            coroutineScope.launch {
                isExporting = true
                exportProgress = 0f
                val poses = if (session.recordedPoses.isNotEmpty()) {
                    session.recordedPoses
                } else {
                    SessionReviewRepository.generatePosesForExercise(session.exerciseName, session.durationSeconds)
                }
                MediaAlbumHelper.exportAndSaveMotionSticksToAlbum(
                    context = context,
                    exerciseName = session.exerciseName,
                    poses = poses,
                    durationSeconds = session.durationSeconds,
                    onProgress = { p -> exportProgress = p }
                )
                isExporting = false
            }
        } else {
            videoFile.let { f ->
                MediaAlbumHelper.saveVideoToPhoneAlbum(
                    context = context,
                    sourceFile = f,
                    exerciseName = session.exerciseName,
                    isMotionSticksOnly = false
                )
            }
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
                        tint = DarkPrimaryGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Exporting Motion Sticks (Privacy Mode)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rendering luxury gold skeleton tracking onto black canvas. Zero face, body, or room background recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkPrimaryGold,
                        trackColor = DarkPrimaryGold.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "${(exportProgress * 100).toInt()}% Encoded",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimaryGold
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = session.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${session.repCount} Reps • ${session.durationSeconds}s • ${session.dateString}",
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
                    onClick = { saveWorkoutMedia() }
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
                    color = if (displayMode == PlaybackDisplayMode.REAL_VIDEO) DarkPrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (displayMode == PlaybackDisplayMode.REAL_VIDEO) DarkPrimaryGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                            tint = if (displayMode == PlaybackDisplayMode.REAL_VIDEO) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Real Video",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (displayMode == PlaybackDisplayMode.REAL_VIDEO) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    onClick = { displayMode = PlaybackDisplayMode.MOTION_STICKS_ONLY },
                    shape = RoundedCornerShape(10.dp),
                    color = if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) DarkPrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) DarkPrimaryGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
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
                            tint = if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Motion Sticks Only",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY) Color.Black else MaterialTheme.colorScheme.onSurface
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
                                            tint = DarkPrimaryGold,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No Raw Video File Attached",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkPrimaryGold
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
                            MotionSticksCanvas(
                                poses = session.recordedPoses,
                                currentPositionMs = currentPositionMs.toLong(),
                                modifier = Modifier.fillMaxSize()
                            )
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
                            tint = DarkPrimaryGold,
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
                        val totalSec = session.durationSeconds
                        Text(
                            text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Save to Phone Album Action Controls (Privacy Mode vs Full Video)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayMode == PlaybackDisplayMode.MOTION_STICKS_ONLY || videoFile == null) {
                    Button(
                        onClick = { saveWorkoutMedia(forceMotionSticks = true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Motion Sticks to Phone Album (Privacy Mode)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (videoFile != null) {
                        OutlinedButton(
                            onClick = { saveWorkoutMedia(forceMotionSticks = false) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Real Video to Phone Album",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { saveWorkoutMedia(forceMotionSticks = false) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Real Video to Phone Album",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { saveWorkoutMedia(forceMotionSticks = true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Motion Sticks Only (No Face / Background)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
                        thumbColor = DarkPrimaryGold,
                        activeTrackColor = DarkPrimaryGold,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Timeline Flaw Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    session.detectedFlaws.forEach { flaw ->
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
                            color = DarkPrimaryGold.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.35f)),
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
                                    tint = DarkPrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Coaching Adjustment:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkPrimaryGold
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
        }
    }
}
