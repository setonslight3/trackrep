package com.setons.trackrep.coach

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.setons.trackrep.ai.service.TrackAiService
import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.data.local.CoachPreferences
import com.setons.trackrep.data.local.TrackVoicePreferences
import com.setons.trackrep.screens.coach.CoachModeHolder
import com.setons.trackrep.ui.components.VoiceAssistantPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Controller for Track Voice Assistant.
 * Manages "Track" wake word detection, Google Gemini-style UI,
 * continuous standby listening resilience, and immediate user wake reactivation.
 */
class TrackVoiceAssistantController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val voiceManager: CoachVoiceManager,
    private val onNavigateToDestination: (String) -> Unit
) {
    companion object {
        private const val TAG = "TrackVoiceController"
    }

    var isUiVisible by mutableStateOf(false)
        private set

    var phase by mutableStateOf(VoiceAssistantPhase.IDLE)
        private set

    var statusHeadline by mutableStateOf("Say \"Track\" anytime")
        private set

    var currentSpeechText by mutableStateOf("")
        private set

    // Observable indicator showing whether wake word standby listening is actively running
    var isStandbyListeningActive by mutableStateOf(false)
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoDismissJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    // Health-check / watchdog to ensure standby listening restarts if Android drops it
    private var isUserEnabledStandby = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed", e)
        }
    }

    /**
     * Start or restart continuous wake-word listening loop.
     */
    fun startListeningIfEnabled(forceRecreate: Boolean = false) {
        if (!TrackVoicePreferences.isWakeWordEnabled(context)) {
            stopListening()
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "RECORD_AUDIO permission not granted; standby wake-word listening disabled.")
            isStandbyListeningActive = false
            return
        }

        isUserEnabledStandby = true

        mainHandler.post {
            initAndStartStandbyRecognizer(forceRecreate)
        }
    }

    /**
     * Explicitly turn voice listening back on and trigger a listening session immediately.
     * This provides the user with a guaranteed 1-tap way to reactivate Track.
     */
    fun turnVoiceListeningOnAndListen() {
        TrackVoicePreferences.setWakeWordEnabled(context, true)
        isUserEnabledStandby = true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            currentSpeechText = "Microphone permission required. Please enable in Settings."
            statusHeadline = "Permission Needed"
            isUiVisible = true
            phase = VoiceAssistantPhase.LISTENING
            return
        }

        triggerManualListen()
    }

    /**
     * Stop speech recognizer and background loop.
     */
    fun stopListening() {
        isUserEnabledStandby = false
        isStandbyListeningActive = false
        mainHandler.post {
            destroyRecognizerSafely()
        }
    }

    /**
     * Manual trigger (e.g. tapping the capsule mic button or top-bar mic chip).
     */
    fun triggerManualListen() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            currentSpeechText = "Microphone permission required. Please enable in Settings."
            statusHeadline = "Permission Needed"
            isUiVisible = true
            phase = VoiceAssistantPhase.LISTENING
            return
        }

        autoDismissJob?.cancel()
        playWakeTone()
        isUiVisible = true
        phase = VoiceAssistantPhase.LISTENING
        statusHeadline = "Listening..."
        currentSpeechText = ""

        mainHandler.post {
            startCommandListening()
        }
    }

    fun dismiss() {
        autoDismissJob?.cancel()
        isUiVisible = false
        phase = VoiceAssistantPhase.IDLE
        currentSpeechText = ""
        statusHeadline = "Say \"Track\" anytime"

        // Always resurrect standby wake-word listening with fresh listener
        if (TrackVoicePreferences.isWakeWordEnabled(context)) {
            mainHandler.postDelayed({
                startListeningIfEnabled(forceRecreate = true)
            }, 450L)
        }
    }

    fun executeDirectCommand(command: String) {
        autoDismissJob?.cancel()
        isUiVisible = true
        currentSpeechText = "\"$command\""
        executeVoiceCommand(command)
    }

    private fun playWakeTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
    }

    private fun destroyRecognizerSafely() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying recognizer", e)
        } finally {
            speechRecognizer = null
            isStandbyListeningActive = false
        }
    }

    private fun initAndStartStandbyRecognizer(forceRecreate: Boolean = false) {
        if (!isUserEnabledStandby || phase != VoiceAssistantPhase.IDLE) return

        try {
            if (forceRecreate || speechRecognizer == null) {
                destroyRecognizerSafely()
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                } else {
                    Log.w(TAG, "SpeechRecognizer not available on this device")
                    isStandbyListeningActive = false
                    return
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isStandbyListeningActive = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    isStandbyListeningActive = false
                    // Standby timeout or pause: quietly restart listening with fresh session
                    if (isUserEnabledStandby && phase == VoiceAssistantPhase.IDLE) {
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> 650L
                            else -> 400L
                        }
                        mainHandler.postDelayed({
                            initAndStartStandbyRecognizer(forceRecreate = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT)
                        }, delay)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isStandbyListeningActive = false
                    handleSpeechCandidates(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
                    // Loop standby listening if still IDLE
                    if (isUserEnabledStandby && phase == VoiceAssistantPhase.IDLE) {
                        mainHandler.postDelayed({
                            initAndStartStandbyRecognizer(forceRecreate = false)
                        }, 300L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    handleSpeechCandidates(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            isStandbyListeningActive = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start standby speech recognizer", e)
            isStandbyListeningActive = false
            mainHandler.postDelayed({
                if (isUserEnabledStandby && phase == VoiceAssistantPhase.IDLE) {
                    initAndStartStandbyRecognizer(forceRecreate = true)
                }
            }, 1000L)
        }
    }

    private fun handleSpeechCandidates(candidates: List<String>?) {
        if (candidates.isNullOrEmpty()) return

        val configuredWakeWord = TrackVoicePreferences.getWakeWord(context).lowercase()

        for (rawCandidate in candidates) {
            val text = rawCandidate.trim().lowercase()

            if (phase == VoiceAssistantPhase.IDLE) {
                val matchesWakeWord = text.contains(configuredWakeWord) || text.contains("track") || text.contains("hey track")

                if (matchesWakeWord) {
                    onWakeWordDetected(text, configuredWakeWord)
                    return
                }
            } else if (phase == VoiceAssistantPhase.LISTENING) {
                currentSpeechText = rawCandidate
            }
        }
    }

    private fun onWakeWordDetected(fullText: String, wakeWord: String) {
        playWakeTone()
        isUiVisible = true
        autoDismissJob?.cancel()

        // Extract command if spoken in same sentence (e.g. "Track start workout")
        val extractedCommand = fullText
            .replace(wakeWord, "")
            .replace("hey", "")
            .replace("please", "")
            .trim()

        if (extractedCommand.length >= 3) {
            currentSpeechText = "\"$extractedCommand\""
            executeVoiceCommand(extractedCommand)
        } else {
            phase = VoiceAssistantPhase.LISTENING
            statusHeadline = "Listening..."
            currentSpeechText = ""
            mainHandler.post {
                startCommandListening()
            }
        }
    }

    private fun startCommandListening() {
        try {
            destroyRecognizerSafely()
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                return
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (phase == VoiceAssistantPhase.LISTENING) {
                        currentSpeechText = "Didn't catch that. Say \"Track\" anytime."
                        scheduleAutoDismiss(3200L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val match = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!match.isNullOrBlank()) {
                        currentSpeechText = "\"$match\""
                        executeVoiceCommand(match)
                    } else {
                        scheduleAutoDismiss(2500L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        currentSpeechText = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Start command listening failed", e)
            scheduleAutoDismiss(2500L)
        }
    }

    private fun executeVoiceCommand(command: String) {
        phase = VoiceAssistantPhase.THINKING
        statusHeadline = "Thinking..."

        scope.launch {
            val q = command.trim().lowercase()

            // 1. Direct workout action matches
            val directResponse = handleDirectWorkoutActions(q)
            if (directResponse != null) {
                speakAndComplete(directResponse)
                return@launch
            }

            // 2. Direct app navigation matches
            val navResponse = handleDirectNavigation(q)
            if (navResponse != null) {
                speakAndComplete(navResponse)
                return@launch
            }

            // 3. Fallback to comprehensive TrackAiService (Gemini / On-Device reasoning)
            try {
                val aiResponse = TrackAiService.processMessage(context, command)
                speakAndComplete(aiResponse.replyMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Track AI reasoning error", e)
                speakAndComplete("I'm ready! You can tell me to start a set, switch exercises, or toggle the flashlight.")
            }
        }
    }

    private fun handleDirectWorkoutActions(q: String): String? {
        return when {
            // Start set / start workout
            q.contains("start workout") || q.contains("start training") || q.contains("start set") || q.contains("begin set") || q.contains("start") -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.triggerStartRecording++
                "Starting workout now! Calibrate your position and 3, 2, 1, Go!"
            }

            // Stop set / finish workout
            q.contains("stop set") || q.contains("end set") || q.contains("finish set") || q.contains("stop workout") || q.contains("pause set") -> {
                CoachModeHolder.triggerStopRecording++
                "Stopping set. Great effort!"
            }

            // Flashlight / Torch
            q.contains("torch") || q.contains("flashlight") || q.contains("flash") -> {
                CoachModeHolder.triggerToggleTorch++
                "Toggling camera flashlight."
            }

            // Ghost shadow form guide
            q.contains("ghost") || q.contains("shadow") || q.contains("hologram") || q.contains("ar form") -> {
                val current = CoachPreferences.isGhostShadowEnabled(context)
                val target = !current
                CoachPreferences.setGhostShadowEnabled(context, target)
                if (target) "Ghost shadow AR form guide enabled." else "Ghost shadow turned off."
            }

            // Hands-Free auto start
            q.contains("hands free") || q.contains("auto start") || q.contains("countdown") -> {
                val current = CoachPreferences.isHandsFreeStartEnabled(context)
                val target = !current
                CoachPreferences.setHandsFreeStartEnabled(context, target)
                if (target) "Hands-free countdown auto-start enabled." else "Hands-free auto-start disabled."
            }

            // Tempo metronome presets
            q.contains("tempo") -> {
                when {
                    q.contains("3 1 1") || q.contains("hypertrophy") -> {
                        CoachPreferences.setTempoPreset(context, "3-1-1")
                        "Tempo set to 3-1-1 Hypertrophy TUT."
                    }
                    q.contains("2 1 2") || q.contains("beginner") -> {
                        CoachPreferences.setTempoPreset(context, "2-1-2")
                        "Tempo set to 2-1-2 Beginner Control."
                    }
                    q.contains("2 0 1") || q.contains("power") || q.contains("explosive") -> {
                        CoachPreferences.setTempoPreset(context, "2-0-1")
                        "Tempo set to 2-0-1 Athletic Power."
                    }
                    q.contains("4 2 1") || q.contains("slow") -> {
                        CoachPreferences.setTempoPreset(context, "4-2-1")
                        "Tempo set to 4-2-1 Slow Burn."
                    }
                    q.contains("off") || q.contains("disable") || q.contains("stop") -> {
                        CoachPreferences.setTempoPreset(context, "OFF")
                        "Tempo metronome turned off."
                    }
                    else -> {
                        CoachPreferences.setTempoPreset(context, "2-1-2")
                        "Tempo metronome set to 2-1-2."
                    }
                }
            }

            // Switch exercises
            q.contains("push up") || q.contains("pushup") -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.setPending("push_up_standard", ExerciseFramingMode.PUSH_UP)
                "Switched to Standard Push-ups."
            }
            q.contains("squat") -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.setPending("squat_standard", ExerciseFramingMode.SQUAT)
                "Switched to Bodyweight Squats."
            }
            q.contains("plank") -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.setPending("plank_standard", ExerciseFramingMode.PLANK)
                "Switched to Plank Hold."
            }
            q.contains("dip") -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.setPending("dips_standard", ExerciseFramingMode.PUSH_UP)
                "Switched to Dips."
            }
            q.contains("pull up") || q.contains("pullup") -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.setPending("pull_up_standard", ExerciseFramingMode.PULL_UP)
                "Switched to Pull-ups."
            }

            else -> null
        }
    }

    private fun handleDirectNavigation(q: String): String? {
        return when {
            q.contains("go to history") || q.contains("show history") || q.contains("view history") || q.contains("my records") -> {
                onNavigateToDestination("HISTORY")
                "Opening your workout history and records."
            }
            q.contains("go to library") || q.contains("show exercises") || q.contains("exercise catalog") -> {
                onNavigateToDestination("EXERCISE_LIBRARY")
                "Opening the exercise library."
            }
            q.contains("go to coach") || q.contains("open coach") || q.contains("open camera") -> {
                onNavigateToDestination("COACH")
                "Opening AI Vision Coach."
            }
            q.contains("go to settings") || q.contains("open settings") || q.contains("go to profile") || q.contains("change theme") -> {
                onNavigateToDestination("PROFILE")
                "Opening profile and settings."
            }
            q.contains("go to home") || q.contains("go to today") || q.contains("home screen") -> {
                onNavigateToDestination("HOME")
                "Navigating to Today's screen."
            }
            q.contains("edit routine") || q.contains("swap routine") || q.contains("routine editor") -> {
                onNavigateToDestination("EDIT_ROUTINE")
                "Opening the routine customizer."
            }
            else -> null
        }
    }

    private fun speakAndComplete(reply: String) {
        phase = VoiceAssistantPhase.SPEAKING
        statusHeadline = "Track Coach"
        currentSpeechText = reply

        if (TrackVoicePreferences.isVoiceResponsesEnabled(context)) {
            voiceManager.speakStatus(reply, isUrgent = true)
        }

        val delayMillis = (reply.length * 65L).coerceIn(3800L, 8500L)
        scheduleAutoDismiss(delayMillis)
    }

    private fun scheduleAutoDismiss(delayMs: Long) {
        autoDismissJob?.cancel()
        autoDismissJob = scope.launch {
            delay(delayMs)
            dismiss()
        }
    }
}
