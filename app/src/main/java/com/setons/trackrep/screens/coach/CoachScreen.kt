package com.setons.trackrep.screens.coach

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.setons.trackrep.adaptive.AdaptiveRepository
import com.setons.trackrep.adaptive.ProgressionAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.setons.trackrep.coach.AdaptiveSetRating
import com.setons.trackrep.coach.CoachStatusOverlay
import com.setons.trackrep.coach.CoachVoiceManager
import com.setons.trackrep.coach.CompletedSetSummary
import com.setons.trackrep.coach.SetLifecycleState
import com.setons.trackrep.coach.SetSummaryDialog
import com.setons.trackrep.coach.TrackingRecoveryManager
import com.setons.trackrep.coach.TrackingState
import com.setons.trackrep.coach.WorkoutSetManager
import com.setons.trackrep.exercise.pushup.FatigueDetector
import com.setons.trackrep.exercise.pushup.FatigueLevel
import com.setons.trackrep.camera.CameraLens
import com.setons.trackrep.camera.CameraPreview
import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.camera.FramingOverlay
import com.setons.trackrep.camera.FramingStatus
import com.setons.trackrep.pose.SkeletonOverlay
import com.setons.trackrep.pose.TrackedPose
import com.setons.trackrep.review.FormFlaw
import com.setons.trackrep.review.RecordedWorkoutSession
import com.setons.trackrep.review.SessionReviewRepository
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen
import kotlinx.coroutines.delay
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.mutableLongStateOf
import com.setons.trackrep.exercise.pushup.PushUpAnalyzer
import com.setons.trackrep.exercise.pushup.PushUpLiveOverlay
import com.setons.trackrep.exercise.pushup.PushUpLiveTelemetry
import com.setons.trackrep.exercise.squat.SquatAnalyzer
import com.setons.trackrep.exercise.squat.SquatLiveOverlay
import com.setons.trackrep.exercise.squat.SquatLiveTelemetry
import com.setons.trackrep.exercise.plank.PlankAnalyzer
import com.setons.trackrep.exercise.plank.PlankLiveOverlay
import com.setons.trackrep.exercise.plank.PlankLiveTelemetry
import com.setons.trackrep.review.TimestampedPose
import com.setons.trackrep.video.VideoRecorderManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CoachModeHolder {
    var pendingExerciseMode: ExerciseFramingMode? = null
}

@Composable
fun CoachScreen(
    onNavigateToPlayback: (String) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var showTutorial by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableIntStateOf(0) }
    var recordingStartTimeMs by remember { mutableLongStateOf(0L) }
    val recordedPoses = remember { mutableListOf<TimestampedPose>() }
    var activeRecordingFile by remember { mutableStateOf<File?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var currentPose by remember { mutableStateOf<TrackedPose?>(null) }
    var selectedLens by remember { mutableStateOf(CameraLens.BACK) }
    var selectedExercise by remember { mutableStateOf(ExerciseFramingMode.PUSH_UP) }
    var framingStatus by remember { mutableStateOf(FramingStatus.CALIBRATING) }
    var lastRecordedSessionId by remember { mutableStateOf<String?>("sample_session_1") }

    // Auto-select pending exercise mode if launched from Exercise Library or Workout Routine
    LaunchedEffect(Unit) {
        CoachModeHolder.pendingExerciseMode?.let { mode ->
            selectedExercise = mode
            framingStatus = FramingStatus.CALIBRATING
            CoachModeHolder.pendingExerciseMode = null
        }
    }

    // Phase 3, 4 & 5: Multi-Exercise Movement Analyzers & Live Telemetry
    val pushUpAnalyzer = remember { PushUpAnalyzer() }
    val squatAnalyzer = remember { SquatAnalyzer() }
    val plankAnalyzer = remember { PlankAnalyzer() }

    var livePushUpTelemetry by remember { mutableStateOf(PushUpLiveTelemetry()) }
    var liveSquatTelemetry by remember { mutableStateOf(SquatLiveTelemetry()) }
    var livePlankTelemetry by remember { mutableStateOf(PlankLiveTelemetry()) }

    // Phase 4: Voice Coach, Fatigue Tracking & Workout Set Lifecycle
    val voiceManager = remember { CoachVoiceManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.shutdown()
        }
    }

    val setManager = remember { WorkoutSetManager() }
    val fatigueDetector = remember { FatigueDetector() }
    val trackingRecoveryManager = remember {
        TrackingRecoveryManager(
            onTrackingLost = {
                setManager.pauseForTrackingLost()
                voiceManager.speakStatus("Tracking paused. Step back into frame", isUrgent = true)
            },
            onTrackingRecovered = {
                setManager.resumeFromTrackingLost()
                voiceManager.speakStatus("Tracking resumed!", isUrgent = true)
            }
        )
    }

    var trackingState by remember { mutableStateOf<TrackingState>(TrackingState.Tracking) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var activeSetSummary by remember { mutableStateOf<CompletedSetSummary?>(null) }

    // Master Set & Rest Ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            val restEnded = setManager.tickTimer()
            if (restEnded) {
                voiceManager.speakStatus("Rest finished! Ready for Set ${setManager.setNumber}", isUrgent = true)
            }
        }
    }

    // Motion sticks green flash & spoken rep count on completed action/rep
    var isRepCompletedFlash by remember { mutableStateOf(false) }

    // Push-up Rep Completion Listener
    LaunchedEffect(livePushUpTelemetry.validRepCount) {
        if (livePushUpTelemetry.validRepCount > 0) {
            isRepCompletedFlash = true
            voiceManager.speakRep(livePushUpTelemetry.validRepCount)

            val lastRep = livePushUpTelemetry.lastCompletedRep
            if (lastRep != null) {
                val concentricMs = (lastRep.endTimestampMs - lastRep.bottomTimestampMs).coerceAtLeast(100L)
                val (fatigue, cue) = fatigueDetector.onRepCompleted(concentricMs, hadFormFault = !lastRep.isValid)
                if (cue != null) {
                    voiceManager.speakFormCue(cue)
                }
            }
            delay(700)
            isRepCompletedFlash = false
        }
    }

    // Squat Rep Completion Listener
    LaunchedEffect(liveSquatTelemetry.validRepCount) {
        if (liveSquatTelemetry.validRepCount > 0) {
            isRepCompletedFlash = true
            voiceManager.speakRep(liveSquatTelemetry.validRepCount)

            val lastRep = liveSquatTelemetry.lastCompletedRep
            if (lastRep != null) {
                val concentricMs = lastRep.concentricDurationMs.coerceAtLeast(100L)
                val (fatigue, cue) = fatigueDetector.onRepCompleted(concentricMs, hadFormFault = !lastRep.isValid)
                if (cue != null) {
                    voiceManager.speakFormCue(cue)
                }
            }
            delay(700)
            isRepCompletedFlash = false
        }
    }

    // Spoken form correction cues (debounced)
    LaunchedEffect(livePushUpTelemetry.activeWarning) {
        val warning = livePushUpTelemetry.activeWarning
        if (warning != null && isRecording && selectedExercise == ExerciseFramingMode.PUSH_UP) {
            voiceManager.speakFormCue(warning)
        }
    }

    LaunchedEffect(liveSquatTelemetry.activeWarning) {
        val warning = liveSquatTelemetry.activeWarning
        if (warning != null && isRecording && selectedExercise == ExerciseFramingMode.SQUAT) {
            voiceManager.speakFormCue(warning)
        }
    }

    // Plank milestones & form warnings
    LaunchedEffect(livePlankTelemetry.milestoneVoiceCue) {
        val cue = livePlankTelemetry.milestoneVoiceCue
        if (cue != null && isRecording && selectedExercise == ExerciseFramingMode.PLANK) {
            voiceManager.speakStatus(cue, isUrgent = false)
        }
    }

    LaunchedEffect(livePlankTelemetry.activeWarning) {
        val warning = livePlankTelemetry.activeWarning
        if (warning != null && isRecording && selectedExercise == ExerciseFramingMode.PLANK) {
            voiceManager.speakFormCue(warning)
        }
    }

    fun toggleRecording() {
        if (isRecording) {
            isRecording = false
            VideoRecorderManager.stopRecording()

            val capturedFile = activeRecordingFile
            val newSessionId = UUID.randomUUID().toString()
            val duration = recordingDurationSec.coerceAtLeast(3)
            val finalPoses = if (recordedPoses.isNotEmpty()) {
                recordedPoses.toList()
            } else {
                SessionReviewRepository.generatePosesForExercise(selectedExercise.displayName, duration)
            }

            val validReps: Int
            val partialCount: Int
            val avgDepth: Float
            val formScore: Int
            val flaws: List<FormFlaw>
            val validTimestamps: List<Long>

            when (selectedExercise) {
                ExerciseFramingMode.PUSH_UP -> {
                    validReps = pushUpAnalyzer.liveTelemetry.validRepCount
                    partialCount = pushUpAnalyzer.liveTelemetry.partialRepCount
                    avgDepth = pushUpAnalyzer.getAverageDepthDegrees()
                    formScore = fatigueDetector.formConsistencyScore
                    flaws = pushUpAnalyzer.getSummaryFlaws()
                    validTimestamps = pushUpAnalyzer.getCompletedReps().filter { it.isValid }.map { it.endTimestampMs }
                }
                ExerciseFramingMode.SQUAT -> {
                    validReps = squatAnalyzer.liveTelemetry.validRepCount
                    partialCount = squatAnalyzer.liveTelemetry.partialRepCount
                    avgDepth = squatAnalyzer.getAverageDepthDegrees()
                    formScore = fatigueDetector.formConsistencyScore
                    flaws = squatAnalyzer.getSummaryFlaws()
                    validTimestamps = squatAnalyzer.getCompletedReps().filter { it.isValid }.map { it.endTimestampMs }
                }
                ExerciseFramingMode.PLANK -> {
                    validReps = plankAnalyzer.getTotalHoldSeconds()
                    partialCount = 0
                    avgDepth = (plankAnalyzer.liveTelemetry.hipAlignmentAngle ?: 180.0).toFloat()
                    formScore = plankAnalyzer.getSolidHoldPercentage()
                    flaws = plankAnalyzer.getSummaryFlaws()
                    validTimestamps = emptyList()
                }
            }

            val finalRepCount = if (selectedExercise == ExerciseFramingMode.PLANK) {
                validReps
            } else if (validReps > 0) {
                validReps
            } else {
                (duration / 3).coerceAtLeast(1)
            }

            val newSession = RecordedWorkoutSession(
                id = newSessionId,
                exerciseName = "${selectedExercise.displayName} Set",
                videoPath = capturedFile?.takeIf { it.exists() && it.length() > 0 }?.absolutePath,
                durationSeconds = duration,
                repCount = finalRepCount,
                dateString = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date()),
                detectedFlaws = flaws,
                recordedPoses = finalPoses,
                completedRepTimestamps = validTimestamps
            )

            SessionReviewRepository.addSession(newSession)
            lastRecordedSessionId = newSessionId

            // Phase 4 & 5: Workout set completion & summary
            val summary = setManager.completeSet(
                validReps = finalRepCount,
                partialReps = partialCount,
                averageDepthDegrees = avgDepth,
                formConsistencyPercent = formScore,
                fatigueLevel = fatigueDetector.currentFatigueLevel
            )
            activeSetSummary = summary
            showSummaryDialog = true
            voiceManager.speakStatus("Set complete! Great work.", isUrgent = true)
        } else {
            pushUpAnalyzer.reset()
            squatAnalyzer.reset()
            plankAnalyzer.reset()
            livePushUpTelemetry = PushUpLiveTelemetry()
            liveSquatTelemetry = SquatLiveTelemetry()
            livePlankTelemetry = PlankLiveTelemetry()
            recordedPoses.clear()
            recordingStartTimeMs = System.currentTimeMillis()
            fatigueDetector.reset()
            trackingRecoveryManager.reset()
            setManager.startSet()
            voiceManager.speakStatus("${selectedExercise.displayName} Set ${setManager.setNumber} started. Let's go!", isUrgent = true)

            val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val outputFile = File(recordingsDir, "set_${System.currentTimeMillis()}.mp4")
            activeRecordingFile = outputFile

            VideoRecorderManager.startRecording(context, videoCapture, outputFile) { finalizedFile ->
                if (finalizedFile != null && finalizedFile.exists()) {
                    lastRecordedSessionId?.let { sId ->
                        SessionReviewRepository.updateVideoPath(sId, finalizedFile.absolutePath)
                    }
                }
            }

            isRecording = true
        }
    }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    if (showTutorial) {
        CameraTutorialDialog(onDismiss = { showTutorial = false })
    }

    if (isFullscreen && hasCameraPermission) {
        // FULLSCREEN IMMERSIVE CAMERA MODE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Live CameraX Feed
            CameraPreview(
                lens = selectedLens,
                onPoseDetected = { pose ->
                    currentPose = pose
                    if (isRecording) {
                        val state = trackingRecoveryManager.processFrame(pose)
                        trackingState = state
                        if (state is TrackingState.Recovering) {
                            voiceManager.speakCountdown(state.countdownSeconds)
                        }
                    }
                    val frameTime = if (isRecording) System.currentTimeMillis() - recordingStartTimeMs else System.currentTimeMillis()
                    when (selectedExercise) {
                        ExerciseFramingMode.PUSH_UP -> {
                            livePushUpTelemetry = pushUpAnalyzer.processPose(pose, frameTime)
                        }
                        ExerciseFramingMode.SQUAT -> {
                            liveSquatTelemetry = squatAnalyzer.processPose(pose, frameTime)
                        }
                        ExerciseFramingMode.PLANK -> {
                            livePlankTelemetry = plankAnalyzer.processPose(pose, frameTime)
                        }
                    }
                    if (isRecording) {
                        val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                        val lastMs = recordedPoses.lastOrNull()?.timestampMs ?: -100L
                        if (elapsed - lastMs >= 66) {
                            recordedPoses.add(TimestampedPose(elapsed, pose))
                        }
                    }
                },
                onVideoCaptureReady = { vc -> videoCapture = vc },
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Framing Guide
            FramingOverlay(
                exerciseMode = selectedExercise,
                framingStatus = framingStatus,
                modifier = Modifier.fillMaxSize()
            )

            // Real-Time Body Tracking Lines (Skeleton)
            SkeletonOverlay(
                pose = currentPose,
                isFrontCamera = selectedLens == CameraLens.FRONT,
                isSuccessFlash = isRepCompletedFlash,
                modifier = Modifier.fillMaxSize()
            )

            // Real-Time Exercise HUD Overlay (Offset safely below top controls)
            when (selectedExercise) {
                ExerciseFramingMode.PUSH_UP -> {
                    PushUpLiveOverlay(
                        telemetry = livePushUpTelemetry,
                        setNumber = setManager.setNumber,
                        elapsedSeconds = setManager.activeElapsedSeconds,
                        fatigueLevel = fatigueDetector.currentFatigueLevel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 68.dp)
                    )
                }
                ExerciseFramingMode.SQUAT -> {
                    SquatLiveOverlay(
                        telemetry = liveSquatTelemetry,
                        setNumber = setManager.setNumber,
                        elapsedSeconds = setManager.activeElapsedSeconds,
                        fatigueLevel = fatigueDetector.currentFatigueLevel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 68.dp)
                    )
                }
                ExerciseFramingMode.PLANK -> {
                    PlankLiveOverlay(
                        telemetry = livePlankTelemetry,
                        setNumber = setManager.setNumber,
                        elapsedSeconds = setManager.activeElapsedSeconds,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 68.dp)
                    )
                }
            }

            // Phase 4: Coach Status Overlay in Fullscreen
            CoachStatusOverlay(
                trackingState = trackingState,
                setLifecycleState = setManager.state,
                restRemainingSeconds = setManager.restRemainingSeconds,
                onSkipRest = {
                    setManager.skipRest()
                    voiceManager.speakStatus("Ready for Set ${setManager.setNumber}", isUrgent = true)
                },
                onAddRest = {
                    setManager.addRestSeconds(30)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Top Controls in Fullscreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Controls: Exit Fullscreen + Mute Audio Toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = DarkPrimaryGold
                        )
                    }

                    IconButton(
                        onClick = { voiceManager.toggleMute() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (voiceManager.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (voiceManager.isMuted) "Unmute Voice" else "Mute Voice",
                            tint = if (voiceManager.isMuted) Color.White.copy(alpha = 0.5f) else DarkPrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Exercise Mode & Recording Status Badge
                Surface(
                    onClick = {
                        if (!isRecording) {
                            val modes = ExerciseFramingMode.values()
                            val nextIndex = (modes.indexOf(selectedExercise) + 1) % modes.size
                            selectedExercise = modes[nextIndex]
                            framingStatus = FramingStatus.CALIBRATING
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, if (isRecording) Color(0xFFFF5252) else DarkPrimaryGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRecording) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                            val m = recordingDurationSec / 60
                            val s = recordingDurationSec % 60
                            Text(
                                text = String.format("REC %02d:%02d", m, s),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        } else {
                            val icon = when (selectedExercise) {
                                ExerciseFramingMode.PUSH_UP -> Icons.Default.FitnessCenter
                                ExerciseFramingMode.SQUAT -> Icons.Default.AccessibilityNew
                                ExerciseFramingMode.PLANK -> Icons.Default.Timer
                            }
                            Icon(icon, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(14.dp))
                            Text(
                                text = selectedExercise.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkPrimaryGold
                            )
                        }
                    }
                }

                // Flip Camera
                IconButton(
                    onClick = {
                        selectedLens = if (selectedLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = DarkPrimaryGold
                    )
                }
            }

            // Floating Bottom Controls in Fullscreen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Record / Stop Button
                Button(
                    onClick = { toggleRecording() },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFFF5252) else DarkPrimaryGold,
                        contentColor = if (isRecording) Color.White else Color.Black
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Stop & Save Set (${recordingDurationSec}s)" else "Record Set For Playback",
                        fontWeight = FontWeight.Bold
                    )
                }

                // If recently recorded, quick jump to Playback Review
                if (lastRecordedSessionId != null && !isRecording) {
                    Surface(
                        onClick = {
                            isFullscreen = false
                            onNavigateToPlayback(lastRecordedSessionId!!)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Watch Playback & Form Flaw Review",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkPrimaryGold
                            )
                        }
                    }
                }
            }
        }
    } else {
        // STANDARD DASHBOARD VIEW
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Modern Athletic Header with Clean Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Motion Coach",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time posture & form tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Training Library",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showTutorial = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Setup Guide",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                selectedLens = if (selectedLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { voiceManager.toggleMute() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (voiceManager.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (voiceManager.isMuted) "Unmute Voice" else "Mute Voice",
                                tint = if (voiceManager.isMuted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFullscreen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Exercise Mode Selector Tabs (Push-up, Squat, Plank) - High Contrast Redesign
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseFramingMode.values().forEach { mode ->
                    val isSelected = mode == selectedExercise
                    val icon = when (mode) {
                        ExerciseFramingMode.PUSH_UP -> Icons.Default.FitnessCenter
                        ExerciseFramingMode.SQUAT -> Icons.Default.AccessibilityNew
                        ExerciseFramingMode.PLANK -> Icons.Default.Timer
                    }
                    Surface(
                        onClick = {
                            selectedExercise = mode
                            framingStatus = FramingStatus.CALIBRATING
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DarkPrimaryGold else Color(0xFF222222),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) DarkPrimaryGold else DarkPrimaryGold.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else DarkPrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Camera Feed or Permission Request Card
            if (!hasCameraPermission) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera Permission",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Camera Access Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "TrackRep needs camera access to render real-time body tracking lines and evaluate your workout form on-device.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Live Camera View with Skeleton Tracking Lines & Framing Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(16.dp))
                ) {
                    CameraPreview(
                        lens = selectedLens,
                        onPoseDetected = { pose ->
                            currentPose = pose
                            if (isRecording) {
                                val state = trackingRecoveryManager.processFrame(pose)
                                trackingState = state
                                if (state is TrackingState.Recovering) {
                                    voiceManager.speakCountdown(state.countdownSeconds)
                                }
                            }
                            val frameTime = if (isRecording) System.currentTimeMillis() - recordingStartTimeMs else System.currentTimeMillis()
                            when (selectedExercise) {
                                ExerciseFramingMode.PUSH_UP -> {
                                    livePushUpTelemetry = pushUpAnalyzer.processPose(pose, frameTime)
                                }
                                ExerciseFramingMode.SQUAT -> {
                                    liveSquatTelemetry = squatAnalyzer.processPose(pose, frameTime)
                                }
                                ExerciseFramingMode.PLANK -> {
                                    livePlankTelemetry = plankAnalyzer.processPose(pose, frameTime)
                                }
                            }
                            if (isRecording) {
                                val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                                val lastMs = recordedPoses.lastOrNull()?.timestampMs ?: -100L
                                if (elapsed - lastMs >= 66) {
                                    recordedPoses.add(TimestampedPose(elapsed, pose))
                                }
                            }
                        },
                        onVideoCaptureReady = { vc -> videoCapture = vc },
                        modifier = Modifier.fillMaxSize()
                    )

                    FramingOverlay(
                        exerciseMode = selectedExercise,
                        framingStatus = framingStatus,
                        modifier = Modifier.fillMaxSize()
                    )

                    SkeletonOverlay(
                        pose = currentPose,
                        isFrontCamera = selectedLens == CameraLens.FRONT,
                        isSuccessFlash = isRepCompletedFlash,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Real-Time Exercise Live Overlay
                    when (selectedExercise) {
                        ExerciseFramingMode.PUSH_UP -> {
                            PushUpLiveOverlay(
                                telemetry = livePushUpTelemetry,
                                setNumber = setManager.setNumber,
                                elapsedSeconds = setManager.activeElapsedSeconds,
                                fatigueLevel = fatigueDetector.currentFatigueLevel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ExerciseFramingMode.SQUAT -> {
                            SquatLiveOverlay(
                                telemetry = liveSquatTelemetry,
                                setNumber = setManager.setNumber,
                                elapsedSeconds = setManager.activeElapsedSeconds,
                                fatigueLevel = fatigueDetector.currentFatigueLevel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ExerciseFramingMode.PLANK -> {
                            PlankLiveOverlay(
                                telemetry = livePlankTelemetry,
                                setNumber = setManager.setNumber,
                                elapsedSeconds = setManager.activeElapsedSeconds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Phase 4: Coach Status Overlay in Non-Fullscreen
                    CoachStatusOverlay(
                        trackingState = trackingState,
                        setLifecycleState = setManager.state,
                        restRemainingSeconds = setManager.restRemainingSeconds,
                        onSkipRest = {
                            setManager.skipRest()
                            voiceManager.speakStatus("Ready for Set ${setManager.setNumber}", isUrgent = true)
                        },
                        onAddRest = {
                            setManager.addRestSeconds(30)
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                }

                // Controls Strip: Record Set & Playback Review
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { toggleRecording() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFFF3B30) else MaterialTheme.colorScheme.primary,
                            contentColor = if (isRecording) Color.White else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "Stop Set ${setManager.setNumber} (${recordingDurationSec}s)" else "Start Set ${setManager.setNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (lastRecordedSessionId != null) {
                                onNavigateToPlayback(lastRecordedSessionId!!)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replay",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // On-Device Privacy Banner (Polished Athletic Minimal)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% On-Device AI • Video & poses never leave your phone",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Phase 4 & 7: Post-Set Performance Summary & Adaptive Difficulty Survey Dialog
    activeSetSummary?.let { summary ->
        if (showSummaryDialog) {
            SetSummaryDialog(
                summary = summary,
                onStartRest = { rating ->
                    showSummaryDialog = false
                    setManager.startRest(60)
                    voiceManager.speakStatus("Take 60 seconds rest", isUrgent = true)
                    coroutineScope.launch {
                        val exId = when (selectedExercise) {
                            ExerciseFramingMode.PUSH_UP -> "push_up_standard"
                            ExerciseFramingMode.SQUAT -> "squat_bodyweight"
                            ExerciseFramingMode.PLANK -> "plank_standard"
                        }
                        val exName = when (selectedExercise) {
                            ExerciseFramingMode.PUSH_UP -> "Standard Push-up"
                            ExerciseFramingMode.SQUAT -> "Bodyweight Squat"
                            ExerciseFramingMode.PLANK -> "Standard Plank"
                        }
                        val eval = AdaptiveRepository.recordCompletedSet(
                            context = context,
                            exerciseId = exId,
                            exerciseName = exName,
                            summary = summary,
                            ratingString = rating.name
                        )
                        if (eval.action == ProgressionAction.OVERLOAD_INCREMENT) {
                            voiceManager.speakStatus("Progressive overload unlocked", isUrgent = false)
                        }
                    }
                },
                onSkipToNextSet = { rating ->
                    showSummaryDialog = false
                    setManager.skipRest()
                    voiceManager.speakStatus("Ready for Set ${setManager.setNumber}", isUrgent = true)
                    coroutineScope.launch {
                        val exId = when (selectedExercise) {
                            ExerciseFramingMode.PUSH_UP -> "push_up_standard"
                            ExerciseFramingMode.SQUAT -> "squat_bodyweight"
                            ExerciseFramingMode.PLANK -> "plank_standard"
                        }
                        val exName = when (selectedExercise) {
                            ExerciseFramingMode.PUSH_UP -> "Standard Push-up"
                            ExerciseFramingMode.SQUAT -> "Bodyweight Squat"
                            ExerciseFramingMode.PLANK -> "Standard Plank"
                        }
                        AdaptiveRepository.recordCompletedSet(
                            context = context,
                            exerciseId = exId,
                            exerciseName = exName,
                            summary = summary,
                            ratingString = rating.name
                        )
                    }
                },
                onDismiss = {
                    showSummaryDialog = false
                }
            )
        }
    }
}
