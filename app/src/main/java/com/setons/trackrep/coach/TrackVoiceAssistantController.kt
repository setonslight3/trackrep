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
 *
 * Enhanced in v1.3.3:
 * - Multi-hypothesis candidate evaluation (EXTRA_MAX_RESULTS = 5)
 * - Accent-aware regional language tagging with Locale.getDefault()
 * - Extended speech silence timeouts to prevent premature user cut-off
 * - Partial transcript fallback recovery
 * - Phonetic resilience and compound command parsing via VoiceCommandParser
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

    // Preserves recent partial transcript so background noise or silence does not discard user input
    private var lastRecognizedPartial: String? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed", e)
        }
    }

    private fun getSpeechLanguageTag(): String {
        return try {
            val def = Locale.getDefault()
            val tag = def.toLanguageTag()
            if (tag.isNotBlank()) tag else "en-US"
        } catch (_: Exception) {
            "en-US"
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

        if (com.setons.trackrep.music.TrackMusicController.isMusicActive(context)) {
            Log.d(TAG, "Music is actively playing; standby wake-word listening suspended to preserve audio playback.")
            isStandbyListeningActive = false
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
        lastRecognizedPartial = null

        mainHandler.post {
            startCommandListening()
        }
    }

    fun dismiss() {
        autoDismissJob?.cancel()
        com.setons.trackrep.music.TrackMusicController.abandonDucking(context)
        isUiVisible = false
        phase = VoiceAssistantPhase.IDLE
        currentSpeechText = ""
        lastRecognizedPartial = null
        statusHeadline = "Say \"Track\" anytime"

        // Only resurrect standby wake-word listening if music is not active
        if (TrackVoicePreferences.isWakeWordEnabled(context) && !com.setons.trackrep.music.TrackMusicController.isMusicActive(context)) {
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

        if (com.setons.trackrep.music.TrackMusicController.isMusicActive(context)) {
            Log.d(TAG, "Music active during standby recognizer init; keeping recognizer closed.")
            destroyRecognizerSafely()
            return
        }

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

            val languageTag = getSpeechLanguageTag()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
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
                    // Standby timeout or pause: quietly restart listening with fresh session if music is not active
                    if (isUserEnabledStandby && phase == VoiceAssistantPhase.IDLE && !com.setons.trackrep.music.TrackMusicController.isMusicActive(context)) {
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
                    val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    handleStandbySpeechCandidates(candidates)

                    // Loop standby listening if still IDLE and music is not active
                    if (isUserEnabledStandby && phase == VoiceAssistantPhase.IDLE && !com.setons.trackrep.music.TrackMusicController.isMusicActive(context)) {
                        mainHandler.postDelayed({
                            initAndStartStandbyRecognizer(forceRecreate = false)
                        }, 300L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    handleStandbySpeechCandidates(candidates)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            isStandbyListeningActive = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start standby speech recognizer", e)
            isStandbyListeningActive = false
            mainHandler.postDelayed({
                if (isUserEnabledStandby && phase == VoiceAssistantPhase.IDLE && !com.setons.trackrep.music.TrackMusicController.isMusicActive(context)) {
                    initAndStartStandbyRecognizer(forceRecreate = true)
                }
            }, 1000L)
        }
    }

    private fun handleStandbySpeechCandidates(candidates: List<String>?) {
        if (candidates.isNullOrEmpty() || phase != VoiceAssistantPhase.IDLE) return

        val configuredWakeWord = TrackVoicePreferences.getWakeWord(context)

        for (cand in candidates) {
            if (VoiceCommandParser.matchesWakeWord(cand, configuredWakeWord)) {
                onWakeWordDetected(cand, configuredWakeWord)
                return
            }
        }
    }

    private fun onWakeWordDetected(fullText: String, configuredWakeWord: String) {
        playWakeTone()
        isUiVisible = true
        autoDismissJob?.cancel()

        // Extract command if spoken in same utterance (e.g. "Track start pushups")
        val extractedCommand = VoiceCommandParser.extractCommandAfterWakeWord(fullText, configuredWakeWord)

        if (extractedCommand.length >= 3) {
            val parsed = VoiceCommandParser.parseCandidates(listOf(extractedCommand))
            currentSpeechText = "\"$extractedCommand\""
            executeParsedVoiceCommand(parsed)
        } else {
            phase = VoiceAssistantPhase.LISTENING
            statusHeadline = "Listening..."
            currentSpeechText = ""
            lastRecognizedPartial = null
            mainHandler.post {
                startCommandListening()
            }
        }
    }

    private fun startCommandListening() {
        try {
            com.setons.trackrep.music.TrackMusicController.requestDucking(context)
            destroyRecognizerSafely()
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                return
            }

            val languageTag = getSpeechLanguageTag()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                // Speech endpointing & timeout resilience
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (phase == VoiceAssistantPhase.LISTENING) {
                        // Check if we captured a usable partial transcript before error
                        val partial = lastRecognizedPartial
                        if (!partial.isNullOrBlank()) {
                            val fallbackParsed = VoiceCommandParser.parseCandidates(listOf(partial))
                            if (fallbackParsed.confidenceScore >= 0.85f) {
                                currentSpeechText = "\"${fallbackParsed.originalText}\""
                                executeParsedVoiceCommand(fallbackParsed)
                                return
                            }
                        }

                        currentSpeechText = "Didn't catch that. Say \"Track\" anytime."
                        scheduleAutoDismiss(3200L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val parsed = VoiceCommandParser.parseCandidates(candidates, lastRecognizedPartial)

                    if (parsed.originalText.isNotBlank()) {
                        currentSpeechText = "\"${parsed.originalText}\""
                        executeParsedVoiceCommand(parsed)
                    } else {
                        currentSpeechText = "Didn't catch that. Say \"Track\" anytime."
                        scheduleAutoDismiss(2500L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = candidates?.firstOrNull()?.trim()
                    if (!partial.isNullOrBlank()) {
                        lastRecognizedPartial = partial
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
        val parsed = VoiceCommandParser.parseCandidates(listOf(command))
        executeParsedVoiceCommand(parsed)
    }

    private fun executeParsedVoiceCommand(parsed: VoiceCommandParser.ParsedCommand) {
        phase = VoiceAssistantPhase.THINKING
        statusHeadline = "Thinking..."

        when (val intent = parsed.intent) {
            is VoiceCommandParser.VoiceIntent.StartWorkout -> {
                if (intent.targetExerciseId != null && intent.mode != null) {
                    CoachModeHolder.setPending(intent.targetExerciseId, intent.mode)
                }
                onNavigateToDestination("COACH")
                CoachModeHolder.triggerStartRecording++
                speakAndComplete(parsed.feedbackSpeech)
            }

            is VoiceCommandParser.VoiceIntent.StopWorkout -> {
                CoachModeHolder.triggerStopRecording++
                speakAndComplete(parsed.feedbackSpeech)
            }

            is VoiceCommandParser.VoiceIntent.SwitchExercise -> {
                onNavigateToDestination("COACH")
                CoachModeHolder.setPending(intent.exerciseId, intent.mode)
                speakAndComplete(parsed.feedbackSpeech)
            }

            is VoiceCommandParser.VoiceIntent.ToggleTorch -> {
                CoachModeHolder.triggerToggleTorch++
                speakAndComplete(parsed.feedbackSpeech)
            }

            is VoiceCommandParser.VoiceIntent.ToggleGhostShadow -> {
                val current = CoachPreferences.isGhostShadowEnabled(context)
                val target = intent.explicitEnable ?: !current
                CoachPreferences.setGhostShadowEnabled(context, target)
                val reply = if (target) "Ghost shadow AR form guide enabled." else "Ghost shadow turned off."
                speakAndComplete(reply)
            }

            is VoiceCommandParser.VoiceIntent.ToggleHandsFree -> {
                val current = CoachPreferences.isHandsFreeStartEnabled(context)
                val target = intent.explicitEnable ?: !current
                CoachPreferences.setHandsFreeStartEnabled(context, target)
                val reply = if (target) "Hands-free countdown auto-start enabled." else "Hands-free auto-start disabled."
                speakAndComplete(reply)
            }

            is VoiceCommandParser.VoiceIntent.SetTempo -> {
                CoachPreferences.setTempoPreset(context, intent.preset)
                speakAndComplete(parsed.feedbackSpeech)
            }

            is VoiceCommandParser.VoiceIntent.Navigate -> {
                onNavigateToDestination(intent.destination)
                speakAndComplete(parsed.feedbackSpeech)
            }

            is VoiceCommandParser.VoiceIntent.PlayMusic -> {
                val feedback = com.setons.trackrep.music.TrackMusicController.play(context, intent.query, intent.isPlaylist)
                speakAndComplete(feedback)
            }

            is VoiceCommandParser.VoiceIntent.PauseMusic -> {
                val feedback = com.setons.trackrep.music.TrackMusicController.pauseMusic(context)
                speakAndComplete(feedback)
            }

            is VoiceCommandParser.VoiceIntent.ResumeMusic -> {
                val feedback = com.setons.trackrep.music.TrackMusicController.resumeMusic(context)
                speakAndComplete(feedback)
            }

            is VoiceCommandParser.VoiceIntent.SkipMusic -> {
                val feedback = com.setons.trackrep.music.TrackMusicController.skipMusic(context)
                speakAndComplete(feedback)
            }

            is VoiceCommandParser.VoiceIntent.StopMusic -> {
                val feedback = com.setons.trackrep.music.TrackMusicController.stopMusic(context)
                speakAndComplete(feedback)
            }

            is VoiceCommandParser.VoiceIntent.AdjustVolume -> {
                val feedback = if (intent.isUp) {
                    com.setons.trackrep.music.TrackMusicController.volumeUp(context)
                } else {
                    com.setons.trackrep.music.TrackMusicController.volumeDown(context)
                }
                speakAndComplete(feedback)
            }

            is VoiceCommandParser.VoiceIntent.AskAi -> {
                scope.launch {
                    try {
                        val aiResponse = TrackAiService.processMessage(context, intent.cleanQuery)
                        speakAndComplete(aiResponse.replyMessage)
                    } catch (e: Exception) {
                        Log.e(TAG, "Track AI reasoning error", e)
                        speakAndComplete("I'm ready! You can tell me to start a set, switch exercises, or toggle the flashlight.")
                    }
                }
            }
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
