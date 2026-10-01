package com.setons.trackrep.screens.coach

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Visibility
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.Exercise
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.pose.ExerciseClassifier
import com.setons.trackrep.ui.demo.StickmanDemoPlayer
import com.setons.trackrep.ui.components.TrackRepSwitch
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.setons.trackrep.adaptive.AdaptiveRepository
import com.setons.trackrep.adaptive.ProgressionAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.schedule.UserProfileRepository
import kotlinx.coroutines.Dispatchers
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
import com.setons.trackrep.data.local.CoachPreferences
import com.setons.trackrep.exercise.model.WorkoutRoutine
import java.util.Locale
import java.util.UUID

object CoachModeHolder {
    var pendingExerciseMode: ExerciseFramingMode? = null
    var pendingExerciseId: String? = null
    var pendingTargetReps: Int = 10
    var pendingTargetHoldSeconds: Int = 0
    var pendingTargetSets: Int = 3
    var pendingCameraEnabled: Boolean = true
    var activeRoutine: WorkoutRoutine? = null
    var activeRoutineIndex: Int = 0
    var isImmersiveFullscreen by mutableStateOf(false)
    var updateEventId by mutableLongStateOf(0L)
    var workoutCompletedCount by mutableLongStateOf(0L)

    fun setPending(
        exerciseId: String,
        framingMode: ExerciseFramingMode,
        targetReps: Int = 10,
        targetHoldSeconds: Int = 0,
        targetSets: Int = 3,
        cameraEnabled: Boolean = true,
        routine: WorkoutRoutine? = null,
        routineIndex: Int = 0
    ) {
        this.pendingExerciseId = exerciseId
        this.pendingExerciseMode = framingMode
        this.pendingTargetReps = targetReps
        this.pendingTargetHoldSeconds = targetHoldSeconds
        this.pendingTargetSets = targetSets
        this.pendingCameraEnabled = cameraEnabled
        this.activeRoutine = routine
        this.activeRoutineIndex = routineIndex
        this.updateEventId++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var isCameraEnabled by remember { mutableStateOf(CoachModeHolder.pendingCameraEnabled) }
    var showExerciseDrawer by remember { mutableStateOf(false) }
    var showControlsDrawer by remember { mutableStateOf(false) }
    var showRoutineDrawer by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    LaunchedEffect(isFullscreen) {
        CoachModeHolder.isImmersiveFullscreen = isFullscreen
    }
    DisposableEffect(Unit) {
        onDispose {
            CoachModeHolder.isImmersiveFullscreen = false
        }
    }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableIntStateOf(0) }
    var recordingStartTimeMs by remember { mutableLongStateOf(0L) }
    val recordedPoses = remember { mutableListOf<TimestampedPose>() }
    var activeRecordingFile by remember { mutableStateOf<File?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var currentPose by remember { mutableStateOf<TrackedPose?>(null) }
    var selectedLens by remember { mutableStateOf(CameraLens.BACK) }
    var activeExercise by remember {
        mutableStateOf(
            ExerciseCatalog.getById(CoachModeHolder.pendingExerciseId ?: "push_up_standard")
                ?: ExerciseCatalog.exercises.first()
        )
    }
    var selectedExercise by remember { mutableStateOf(activeExercise.framingMode ?: ExerciseFramingMode.PUSH_UP) }
    var framingStatus by remember { mutableStateOf(FramingStatus.CALIBRATING) }
    var lastRecordedSessionId by remember { mutableStateOf<String?>("sample_session_1") }

    // Auto-detection & Demo States
    val exerciseClassifier = remember { ExerciseClassifier() }
    var isAutoDetectEnabled by remember { mutableStateOf(CoachPreferences.isAutoDetectEnabled(context)) }
    var autoDetectedExerciseName by remember { mutableStateOf<String?>(null) }
    var showAutoDetectBanner by remember { mutableStateOf(false) }
    var showStickmanDemo by remember { mutableStateOf(false) }

    // Workout Routine & Goal Tracking State
    var targetReps by remember { mutableIntStateOf(CoachModeHolder.pendingTargetReps) }
    var targetHoldSeconds by remember { mutableIntStateOf(CoachModeHolder.pendingTargetHoldSeconds) }
    var totalTargetSets by remember { mutableIntStateOf(CoachModeHolder.pendingTargetSets) }
    var currentRoutine by remember { mutableStateOf<WorkoutRoutine?>(CoachModeHolder.activeRoutine) }
    var currentRoutineIndex by remember { mutableIntStateOf(CoachModeHolder.activeRoutineIndex) }

    LaunchedEffect(showAutoDetectBanner) {
        if (showAutoDetectBanner) {
            delay(2800L)
            showAutoDetectBanner = false
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

    // React immediately whenever an exercise is selected (from Home, Library, or Drawer)
    LaunchedEffect(CoachModeHolder.updateEventId) {
        isAutoDetectEnabled = CoachPreferences.isAutoDetectEnabled(context)
        isCameraEnabled = CoachModeHolder.pendingCameraEnabled
        targetReps = CoachModeHolder.pendingTargetReps
        targetHoldSeconds = CoachModeHolder.pendingTargetHoldSeconds
        totalTargetSets = CoachModeHolder.pendingTargetSets
        currentRoutine = CoachModeHolder.activeRoutine
        currentRoutineIndex = CoachModeHolder.activeRoutineIndex

        CoachModeHolder.pendingExerciseId?.let { id ->
            ExerciseCatalog.getById(id)?.let { ex ->
                activeExercise = ex
                selectedExercise = ex.framingMode ?: ExerciseFramingMode.PUSH_UP
                framingStatus = FramingStatus.CALIBRATING
                setManager.reset()
                pushUpAnalyzer.reset()
                squatAnalyzer.reset()
                plankAnalyzer.reset()
                exerciseClassifier.reset()
                livePushUpTelemetry = PushUpLiveTelemetry()
                liveSquatTelemetry = SquatLiveTelemetry()
                livePlankTelemetry = PlankLiveTelemetry()
                if (CoachModeHolder.pendingTargetHoldSeconds > 0) {
                    plankAnalyzer.targetHoldSeconds = CoachModeHolder.pendingTargetHoldSeconds
                }
                voiceManager.speakStatus("Switched to ${ex.name}", isUrgent = true)
            }
            CoachModeHolder.pendingExerciseId = null
        }
        CoachModeHolder.pendingExerciseMode?.let { mode ->
            selectedExercise = mode
            val matching = ExerciseCatalog.exercises.firstOrNull { it.framingMode == mode }
            if (matching != null) {
                activeExercise = matching
            }
            framingStatus = FramingStatus.CALIBRATING
            CoachModeHolder.pendingExerciseMode = null
        }
    }

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
            voiceManager.speakRep(livePushUpTelemetry.validRepCount, targetReps)

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
            voiceManager.speakRep(liveSquatTelemetry.validRepCount, targetReps)

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
                ExerciseFramingMode.PUSH_UP, ExerciseFramingMode.PULL_UP -> {
                    validReps = pushUpAnalyzer.liveTelemetry.validRepCount
                    partialCount = pushUpAnalyzer.liveTelemetry.partialRepCount
                    avgDepth = pushUpAnalyzer.getAverageDepthDegrees()
                    formScore = fatigueDetector.formConsistencyScore
                    flaws = pushUpAnalyzer.getSummaryFlaws()
                    validTimestamps = pushUpAnalyzer.getCompletedReps().filter { it.isValid }.map { it.endTimestampMs }
                }
                ExerciseFramingMode.SQUAT, ExerciseFramingMode.CARDIO -> {
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

            val videoPathStr = capturedFile?.absolutePath
            val nowMs = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(nowMs))
            val todayDateYmd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

            val newSession = RecordedWorkoutSession(
                id = newSessionId,
                exerciseName = "${activeExercise.name} Set",
                videoPath = videoPathStr,
                durationSeconds = duration,
                repCount = finalRepCount,
                dateString = dateStr,
                detectedFlaws = flaws,
                recordedPoses = finalPoses,
                completedRepTimestamps = validTimestamps
            )

            SessionReviewRepository.addSession(newSession)
            lastRecordedSessionId = newSessionId

            // Auto-persist immediately to Room DB so AI & History have permanent local access
            val sessionEntity = WorkoutSessionEntity(
                id = newSessionId,
                routineId = currentRoutine?.id,
                routineName = currentRoutine?.name,
                exerciseId = activeExercise.id,
                exerciseName = activeExercise.name,
                timestampMs = nowMs,
                dateString = dateStr,
                durationSeconds = duration,
                totalValidReps = finalRepCount,
                totalPartialReps = partialCount,
                averageFormScore = formScore,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "COMPLETED",
                isCompleted = true,
                videoPath = videoPathStr
            )

            val setRecordEntity = SetRecordEntity(
                sessionId = newSessionId,
                exerciseId = activeExercise.id,
                setNumber = setManager.setNumber,
                validReps = finalRepCount,
                partialReps = partialCount,
                durationSeconds = duration,
                averageDepthDegrees = avgDepth,
                formConsistencyPercent = formScore,
                fatigueLevel = fatigueDetector.currentFatigueLevel.name
            )

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val db = TrackRepDatabase.getDatabase(context)
                    db.sessionDao().insertSession(sessionEntity)
                    db.setRecordDao().insertSet(setRecordEntity)
                    UserProfileRepository.getInstance(context).markWorkoutCompleted(todayDateYmd)
                    CoachModeHolder.workoutCompletedCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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
            voiceManager.speakStatus("${activeExercise.name} Set ${setManager.setNumber} started. Let's go!", isUrgent = true)

            val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val outputFile = File(recordingsDir, "set_${System.currentTimeMillis()}.mp4")
            activeRecordingFile = outputFile

            VideoRecorderManager.startRecording(context, videoCapture, outputFile) { finalizedFile ->
                if (finalizedFile != null && finalizedFile.exists()) {
                    lastRecordedSessionId?.let { sId ->
                        SessionReviewRepository.updateVideoPath(sId, finalizedFile.absolutePath)
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val db = TrackRepDatabase.getDatabase(context)
                                db.sessionDao().updateVideoPath(sId, finalizedFile.absolutePath)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
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

    // Biomechanical Stickman Demo Dialog (works in fullscreen, camera on, or camera off)
    if (showStickmanDemo) {
        Dialog(
            onDismissRequest = { showStickmanDemo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF8141414),
                border = BorderStroke(2.dp, DarkPrimaryGold),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = DarkPrimaryGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${activeExercise.name} Form Guide",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { showStickmanDemo = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StickmanDemoPlayer(
                        exerciseId = activeExercise.id,
                        heightDp = 220,
                        showControls = true
                    )
                }
            }
        }
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
                isFullscreen = true,
                onPoseDetected = { pose ->
                    currentPose = pose
                    if (isRecording) {
                        val state = trackingRecoveryManager.processFrame(pose)
                        trackingState = state
                        if (state is TrackingState.Recovering) {
                            voiceManager.speakCountdown(state.countdownSeconds)
                        }
                    } else if (isAutoDetectEnabled) {
                        val detected = exerciseClassifier.processPose(pose)
                        if (detected != null && detected.exerciseId != activeExercise.id) {
                            val newEx = ExerciseCatalog.getById(detected.exerciseId)
                            if (newEx != null) {
                                activeExercise = newEx
                                selectedExercise = detected.framingMode
                                framingStatus = FramingStatus.CALIBRATING
                                autoDetectedExerciseName = detected.displayName
                                showAutoDetectBanner = true
                                voiceManager.speakStatus("${detected.displayName} detected", isUrgent = false)
                            }
                        }
                    }
                    val frameTime = if (isRecording) System.currentTimeMillis() - recordingStartTimeMs else System.currentTimeMillis()
                    when (selectedExercise) {
                        ExerciseFramingMode.PUSH_UP, ExerciseFramingMode.PULL_UP -> {
                            livePushUpTelemetry = pushUpAnalyzer.processPose(pose, frameTime)
                        }
                        ExerciseFramingMode.SQUAT, ExerciseFramingMode.CARDIO -> {
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

            // Auto-Detected Exercise Banner Notification
            AnimatedVisibility(
                visible = showAutoDetectBanner,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xF0181408),
                    border = BorderStroke(1.5.dp, DarkPrimaryGold),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = DarkPrimaryGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Auto-Detected: ${autoDetectedExerciseName ?: activeExercise.name}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkPrimaryGold
                        )
                    }
                }
            }

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
                ExerciseFramingMode.PUSH_UP, ExerciseFramingMode.PULL_UP -> {
                    PushUpLiveOverlay(
                        telemetry = livePushUpTelemetry,
                        setNumber = setManager.setNumber,
                        elapsedSeconds = setManager.activeElapsedSeconds,
                        fatigueLevel = fatigueDetector.currentFatigueLevel,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 58.dp)
                    )
                }
                ExerciseFramingMode.SQUAT, ExerciseFramingMode.CARDIO -> {
                    SquatLiveOverlay(
                        telemetry = liveSquatTelemetry,
                        setNumber = setManager.setNumber,
                        elapsedSeconds = setManager.activeElapsedSeconds,
                        fatigueLevel = fatigueDetector.currentFatigueLevel,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 58.dp)
                    )
                }
                ExerciseFramingMode.PLANK -> {
                    PlankLiveOverlay(
                        telemetry = livePlankTelemetry,
                        setNumber = setManager.setNumber,
                        elapsedSeconds = setManager.activeElapsedSeconds,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 58.dp)
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

            // Floating Top Controls in Fullscreen (Clean Minimal 3-Anchor Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Exit Fullscreen
                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = DarkPrimaryGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Center Exercise Selector Capsule
                Surface(
                    onClick = {
                        if (!isRecording) {
                            showExerciseDrawer = true
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, if (isRecording) Color(0xFFFF5252) else DarkPrimaryGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRecording) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(12.dp)
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
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = DarkPrimaryGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeExercise.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkPrimaryGold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 3. Right: Quick Flip & More Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedLens = if (selectedLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = DarkPrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showControlsDrawer = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = DarkPrimaryGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Floating Bottom Controls in Fullscreen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        text = if (isRecording) "Stop Set (${recordingDurationSec}s)" else "Start Set ${setManager.setNumber}",
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
                                text = "Watch Replay",
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
            // Modern Athletic Header with Clean Action Controls (ONLY 2 ACTION ICONS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Motion Coach",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exercise Selector Pill
                    Surface(
                        onClick = { showExerciseDrawer = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E1E1E),
                        border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.5f)),
                        modifier = Modifier.widthIn(max = 160.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = DarkPrimaryGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeExercise.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkPrimaryGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // More Options Drawer
                    IconButton(
                        onClick = { showControlsDrawer = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E1E1E), CircleShape)
                            .border(1.dp, DarkPrimaryGold.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = DarkPrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Routine Progress Banner (if active workout routine)
            currentRoutine?.let { routine ->
                Surface(
                    onClick = { showRoutineDrawer = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1A11),
                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = DarkPrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${routine.name} • ${currentRoutineIndex + 1}/${routine.items.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkPrimaryGold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkPrimaryGold.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Routine List",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkPrimaryGold,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            } else if (!isCameraEnabled) {
                // CAMERA IS OFF VIEW (Sleek offline card + high-contrast exercise picker!)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = DarkPrimaryGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Camera Vision is Off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Turn camera on to track real-time reps, form angles, and get live posture feedback.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isCameraEnabled = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimaryGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Turn Camera On", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Selected Exercise Details & Change Exercise Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1E1E),
                            border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Selected Exercise",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkPrimaryGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = activeExercise.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { showExerciseDrawer = true }
                                    ) {
                                        Text("Change", color = DarkPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { showStickmanDemo = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Watch Stickman Form Demo", color = DarkPrimaryGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Live Camera View with Skeleton Tracking Lines & Framing Overlay
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
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
                                } else if (isAutoDetectEnabled) {
                                    val detected = exerciseClassifier.processPose(pose)
                                    if (detected != null && detected.exerciseId != activeExercise.id) {
                                        val newEx = ExerciseCatalog.getById(detected.exerciseId)
                                        if (newEx != null) {
                                            activeExercise = newEx
                                            selectedExercise = detected.framingMode
                                            framingStatus = FramingStatus.CALIBRATING
                                            autoDetectedExerciseName = detected.displayName
                                            showAutoDetectBanner = true
                                            voiceManager.speakStatus("${detected.displayName} detected", isUrgent = false)
                                        }
                                    }
                                }
                                val frameTime = if (isRecording) System.currentTimeMillis() - recordingStartTimeMs else System.currentTimeMillis()
                                when (selectedExercise) {
                                    ExerciseFramingMode.PUSH_UP, ExerciseFramingMode.PULL_UP -> {
                                        livePushUpTelemetry = pushUpAnalyzer.processPose(pose, frameTime)
                                    }
                                    ExerciseFramingMode.SQUAT, ExerciseFramingMode.CARDIO -> {
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
                            ExerciseFramingMode.PUSH_UP, ExerciseFramingMode.PULL_UP -> {
                                PushUpLiveOverlay(
                                    telemetry = livePushUpTelemetry,
                                    setNumber = setManager.setNumber,
                                    elapsedSeconds = setManager.activeElapsedSeconds,
                                    fatigueLevel = fatigueDetector.currentFatigueLevel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            ExerciseFramingMode.SQUAT, ExerciseFramingMode.CARDIO -> {
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
                }

                // Controls Strip: Record Set & Playback Review
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isCameraEnabled = false },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Turn Camera Off",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    }

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

        }
    }

    // Routine progression and next exercise helper
    val nextExerciseItem = remember(currentRoutine, currentRoutineIndex) {
        val routine = currentRoutine
        if (routine != null && currentRoutineIndex + 1 < routine.items.size) {
            routine.items[currentRoutineIndex + 1]
        } else null
    }
    val nextExerciseName = remember(nextExerciseItem) {
        nextExerciseItem?.let { item ->
            ExerciseCatalog.getById(item.exerciseId)?.name ?: item.exerciseId
        }
    }

    fun advanceToNextExercise(rating: AdaptiveSetRating) {
        showSummaryDialog = false
        coroutineScope.launch {
            activeSetSummary?.let { summary ->
                AdaptiveRepository.recordCompletedSet(
                    context = context,
                    exerciseId = activeExercise.id,
                    exerciseName = activeExercise.name,
                    summary = summary,
                    ratingString = rating.name
                )
            }
        }
        val routine = currentRoutine
        if (routine != null && currentRoutineIndex + 1 < routine.items.size) {
            val nextIdx = currentRoutineIndex + 1
            currentRoutineIndex = nextIdx
            CoachModeHolder.activeRoutineIndex = nextIdx
            val item = routine.items[nextIdx]
            ExerciseCatalog.getById(item.exerciseId)?.let { ex ->
                activeExercise = ex
                selectedExercise = ex.framingMode ?: ExerciseFramingMode.PUSH_UP
                framingStatus = FramingStatus.CALIBRATING
                targetReps = item.targetReps
                targetHoldSeconds = item.targetHoldSeconds
                totalTargetSets = item.targetSets
                if (item.targetHoldSeconds > 0) {
                    plankAnalyzer.targetHoldSeconds = item.targetHoldSeconds
                }
                setManager.reset()
                pushUpAnalyzer.reset()
                squatAnalyzer.reset()
                plankAnalyzer.reset()
                exerciseClassifier.reset()
                livePushUpTelemetry = PushUpLiveTelemetry()
                liveSquatTelemetry = SquatLiveTelemetry()
                livePlankTelemetry = PlankLiveTelemetry()
                voiceManager.speakStatus("Next up: ${ex.name}. Set 1 ready!", isUrgent = true)
            }
        } else {
            val todayDateYmd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    UserProfileRepository.getInstance(context).markWorkoutCompleted(todayDateYmd)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            CoachModeHolder.workoutCompletedCount++
            voiceManager.speakStatus("Full workout routine completed! Outstanding job!", isUrgent = true)
        }
    }

    fun selectRoutineExercise(index: Int) {
        val routine = currentRoutine ?: return
        if (index in routine.items.indices) {
            currentRoutineIndex = index
            CoachModeHolder.activeRoutineIndex = index
            val item = routine.items[index]
            ExerciseCatalog.getById(item.exerciseId)?.let { ex ->
                activeExercise = ex
                selectedExercise = ex.framingMode ?: ExerciseFramingMode.PUSH_UP
                framingStatus = FramingStatus.CALIBRATING
                targetReps = item.targetReps
                targetHoldSeconds = item.targetHoldSeconds
                totalTargetSets = item.targetSets
                if (item.targetHoldSeconds > 0) {
                    plankAnalyzer.targetHoldSeconds = item.targetHoldSeconds
                }
                setManager.reset()
                pushUpAnalyzer.reset()
                squatAnalyzer.reset()
                plankAnalyzer.reset()
                exerciseClassifier.reset()
                livePushUpTelemetry = PushUpLiveTelemetry()
                liveSquatTelemetry = SquatLiveTelemetry()
                livePlankTelemetry = PlankLiveTelemetry()
                voiceManager.speakStatus("Switched to ${ex.name}", isUrgent = true)
            }
        }
    }

    // Phase 4 & 7: Post-Set Performance Summary & Adaptive Difficulty Survey Dialog
    activeSetSummary?.let { summary ->
        if (showSummaryDialog) {
            SetSummaryDialog(
                summary = summary,
                totalTargetSets = totalTargetSets,
                nextExerciseName = nextExerciseName,
                onNextExercise = { rating ->
                    advanceToNextExercise(rating)
                },
                onStartRest = { rating ->
                    showSummaryDialog = false
                    setManager.startRest(60)
                    voiceManager.speakStatus("Take 60 seconds rest", isUrgent = true)
                    coroutineScope.launch {
                        val exId = activeExercise.id
                        val exName = activeExercise.name
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
                        val exId = activeExercise.id
                        val exName = activeExercise.name
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

    // -------------------------------------------------------------
    // DRAWER 1: EXERCISE SELECTION & AI AUTO-DETECTION BOTTOM SHEET
    // -------------------------------------------------------------
    if (showExerciseDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showExerciseDrawer = false },
            containerColor = Color(0xFF161616),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exercise & AI Detection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Select your workout movement or let AI auto-switch",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = { showExerciseDrawer = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // AI Auto-Detect Movement Toggle Card with High-Contrast TrackRepSwitch
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF201D16)),
                    border = BorderStroke(1.5.dp, if (isAutoDetectEnabled) DarkPrimaryGold else Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = DarkPrimaryGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Auto-Detect Exercise",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAutoDetectEnabled) "Active • Camera auto-detects Push-ups, Squats, Planks & more" else "Disabled • Manual movement selection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAutoDetectEnabled) DarkPrimaryGold else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        TrackRepSwitch(
                            checked = isAutoDetectEnabled,
                            onCheckedChange = {
                                isAutoDetectEnabled = it
                                CoachPreferences.setAutoDetectEnabled(context, it)
                                if (it) exerciseClassifier.reset()
                            }
                        )
                    }
                }

                // Current Active Exercise Summary Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CURRENT TARGET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkPrimaryGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = activeExercise.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${activeExercise.difficulty.displayName} • ${activeExercise.targetMuscle.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showExerciseDrawer = false
                                    showStickmanDemo = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkPrimaryGold,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Form Demo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    showExerciseDrawer = false
                                    onNavigateToLibrary()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("All 35 Exercises", color = DarkPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 1. If an active routine is loaded, show today's routine movements with 1-tap switch
                currentRoutine?.let { routine ->
                    Text(
                        text = "TODAY'S ROUTINE (${routine.items.size} MOVEMENTS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkPrimaryGold,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    routine.items.forEachIndexed { idx, item ->
                        val ex = ExerciseCatalog.getById(item.exerciseId)
                        val isCurrent = ex?.id == activeExercise.id
                        val isDone = idx < currentRoutineIndex

                        Surface(
                            onClick = {
                                selectRoutineExercise(idx)
                                showExerciseDrawer = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isCurrent -> DarkPrimaryGold.copy(alpha = 0.18f)
                                isDone -> Color(0xFF1B241C)
                                else -> Color(0xFF1E1E1E)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isCurrent -> DarkPrimaryGold
                                    isDone -> SuccessGreen.copy(alpha = 0.6f)
                                    else -> Color(0xFF333333)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                when {
                                                    isCurrent -> DarkPrimaryGold
                                                    isDone -> SuccessGreen
                                                    else -> Color(0xFF333333)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isCurrent || isDone) Color.Black else Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = ex?.name ?: item.exerciseId,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) DarkPrimaryGold else Color.White
                                        )
                                        Text(
                                            text = "${item.targetSets} sets • " +
                                                    if (item.targetHoldSeconds > 0) "${item.targetHoldSeconds}s hold"
                                                    else "${item.targetReps} reps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkPrimaryGold.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = DarkPrimaryGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. Full Exercises Category Filter & Quick Switch (Bird Dog, Squats, Planks, Push-ups)
                var selectedCategory by remember { mutableStateOf("Core & Back") }
                val categories = listOf("Core & Back", "Chest", "Legs", "Arms", "Cardio")

                Text(
                    text = "ALL MOVEMENTS (QUICK SELECT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isCatSelected = cat == selectedCategory
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCatSelected) DarkPrimaryGold else Color(0xFF222222),
                            border = BorderStroke(1.dp, if (isCatSelected) DarkPrimaryGold else Color(0xFF444444))
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCatSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                val filteredExercises = remember(selectedCategory) {
                    ExerciseCatalog.exercises.filter { ex ->
                        when (selectedCategory) {
                            "Core & Back" -> ex.targetMuscle == MuscleGroup.BACK || ex.targetMuscle == MuscleGroup.CORE || ex.framingMode == ExerciseFramingMode.PLANK
                            "Chest" -> ex.targetMuscle == MuscleGroup.CHEST || ex.framingMode == ExerciseFramingMode.PUSH_UP
                            "Legs" -> ex.targetMuscle == MuscleGroup.QUADS || ex.targetMuscle == MuscleGroup.GLUTES || ex.targetMuscle == MuscleGroup.HAMSTRINGS || ex.targetMuscle == MuscleGroup.CALVES || ex.framingMode == ExerciseFramingMode.SQUAT
                            "Arms" -> ex.targetMuscle == MuscleGroup.ARMS || ex.targetMuscle == MuscleGroup.SHOULDERS
                            "Cardio" -> ex.framingMode == ExerciseFramingMode.CARDIO
                            else -> true
                        }
                    }
                }

                filteredExercises.forEach { ex ->
                    val isCurrent = ex.id == activeExercise.id
                    Surface(
                        onClick = {
                            activeExercise = ex
                            selectedExercise = ex.framingMode ?: ExerciseFramingMode.PUSH_UP
                            targetReps = ex.defaultReps
                            targetHoldSeconds = ex.defaultHoldSeconds
                            totalTargetSets = ex.defaultSets
                            framingStatus = FramingStatus.CALIBRATING
                            val rIdx = currentRoutine?.items?.indexOfFirst { it.exerciseId == ex.id } ?: -1
                            if (rIdx >= 0) {
                                currentRoutineIndex = rIdx
                                CoachModeHolder.activeRoutineIndex = rIdx
                            }
                            setManager.reset()
                            pushUpAnalyzer.reset()
                            squatAnalyzer.reset()
                            plankAnalyzer.reset()
                            exerciseClassifier.reset()
                            livePushUpTelemetry = PushUpLiveTelemetry()
                            liveSquatTelemetry = SquatLiveTelemetry()
                            livePlankTelemetry = PlankLiveTelemetry()
                            if (ex.defaultHoldSeconds > 0) {
                                plankAnalyzer.targetHoldSeconds = ex.defaultHoldSeconds
                            }
                            voiceManager.speakStatus("Switched to ${ex.name}", isUrgent = true)
                            showExerciseDrawer = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) DarkPrimaryGold.copy(alpha = 0.15f) else Color(0xFF1E1E1E),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) DarkPrimaryGold else Color(0xFF333333)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = when (ex.framingMode) {
                                        ExerciseFramingMode.PUSH_UP -> Icons.Default.FitnessCenter
                                        ExerciseFramingMode.SQUAT -> Icons.Default.AccessibilityNew
                                        ExerciseFramingMode.PLANK -> Icons.Default.Timer
                                        ExerciseFramingMode.PULL_UP -> Icons.Default.FitnessCenter
                                        ExerciseFramingMode.CARDIO -> Icons.Default.DirectionsRun
                                        null -> Icons.Default.FitnessCenter
                                    },
                                    contentDescription = null,
                                    tint = if (isCurrent) DarkPrimaryGold else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) DarkPrimaryGold else Color.White
                                    )
                                    Text(
                                        text = "${ex.difficulty.displayName} • ${ex.targetMuscle.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (isCurrent) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DarkPrimaryGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // -------------------------------------------------------------
    // DRAWER 2: CAMERA & SYSTEM CONTROLS BOTTOM SHEET
    // -------------------------------------------------------------
    if (showControlsDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showControlsDrawer = false },
            containerColor = Color(0xFF161616),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Camera & Coach Controls",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Vision feed, audio cues, and display options",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = { showControlsDrawer = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // 1. Camera Vision Power (ON / OFF)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    border = BorderStroke(1.dp, if (isCameraEnabled) DarkPrimaryGold else Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isCameraEnabled) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = if (isCameraEnabled) SuccessGreen else Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Camera Vision",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isCameraEnabled) "Camera is actively tracking" else "Camera is paused (Off)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                        TrackRepSwitch(
                            checked = isCameraEnabled,
                            onCheckedChange = { isCameraEnabled = it }
                        )
                    }
                }

                // 2. Camera Lens (Front / Back)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = null,
                                    tint = DarkPrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Camera Lens",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (selectedLens == CameraLens.BACK) "Rear Camera (Environment)" else "Front Camera (Selfie)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                selectedLens = if (selectedLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkPrimaryGold)
                        ) {
                            Text(
                                text = if (selectedLens == CameraLens.BACK) "Use Front" else "Use Back",
                                color = DarkPrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 3. Audio Voice Feedback Toggle
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (voiceManager.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = DarkPrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Voice Coach Cues",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (voiceManager.isMuted) "Audio muted" else "Active spoken rep & form coaching",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                        TrackRepSwitch(
                            checked = !voiceManager.isMuted,
                            onCheckedChange = { voiceManager.toggleMute() }
                        )
                    }
                }

                // 4. Immersive Fullscreen Mode
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    tint = DarkPrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Fullscreen Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Immersive tracking without navigation bars",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                showControlsDrawer = false
                                isFullscreen = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimaryGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Expand", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // 5. Setup & Positioning Guide Tutorial
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = DarkPrimaryGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Setup & Calibration Guide",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "5-7 paces, floor level, 15° tilt placement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showControlsDrawer = false
                                showTutorial = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkPrimaryGold)
                        ) {
                            Text("Open", color = DarkPrimaryGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // -------------------------------------------------------------
    // DRAWER 3: WORKOUT ROUTINE CHECKLIST & DIRECT SWITCH DRAWER
    // -------------------------------------------------------------
    if (showRoutineDrawer) {
        val routine = currentRoutine
        if (routine != null) {
            ModalBottomSheet(
                onDismissRequest = { showRoutineDrawer = false },
                containerColor = Color(0xFF161616),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = routine.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Today's Routine • ${routine.items.size} Movements",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                        IconButton(onClick = { showRoutineDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    routine.items.forEachIndexed { idx, item ->
                        val ex = ExerciseCatalog.getById(item.exerciseId)
                        val isCurrent = idx == currentRoutineIndex
                        val isDone = idx < currentRoutineIndex

                        Surface(
                            onClick = {
                                selectRoutineExercise(idx)
                                showRoutineDrawer = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isCurrent -> DarkPrimaryGold.copy(alpha = 0.15f)
                                isDone -> Color(0xFF1B241C)
                                else -> Color(0xFF1F1F1F)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isCurrent -> DarkPrimaryGold
                                    isDone -> SuccessGreen.copy(alpha = 0.6f)
                                    else -> Color(0xFF333333)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                when {
                                                    isCurrent -> DarkPrimaryGold
                                                    isDone -> SuccessGreen
                                                    else -> Color(0xFF333333)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) Color.Black else Color.White
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = ex?.name ?: item.exerciseId,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) DarkPrimaryGold else Color.White
                                        )
                                        Text(
                                            text = if (item.targetHoldSeconds > 0) "${item.targetHoldSeconds}s hold • ${item.targetSets} sets" else "${item.targetReps} reps • ${item.targetSets} sets",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DarkPrimaryGold,
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
