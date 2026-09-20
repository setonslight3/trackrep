package com.setons.trackrep.coach

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Audio coaching engine for TrackRep.
 * Uses Android native offline Text-To-Speech with intelligent debouncing
 * to provide hands-free voice coaching during workouts on mobile.
 */
class CoachVoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var isMuted by mutableStateOf(false)
        private set

    private val lastSpokenTimestamps = mutableMapOf<String, Long>()
    private val DEBOUNCE_WINDOW_MS = 3500L

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setSpeechRate(1.05f) // Crisp athletic delivery
                    engine.setPitch(1.0f)
                    isInitialized = true
                }
            }
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            tts?.stop()
        }
        return isMuted
    }

    fun setMute(muted: Boolean) {
        isMuted = muted
        if (isMuted) {
            tts?.stop()
        }
    }

    fun speakRep(count: Int) {
        if (isMuted || !isInitialized) return
        val text = when (count) {
            1 -> "One"
            2 -> "Two"
            3 -> "Three"
            4 -> "Four"
            5 -> "Five"
            6 -> "Six"
            7 -> "Seven"
            8 -> "Eight"
            9 -> "Nine"
            10 -> "Ten, halfway there"
            12 -> "Twelve"
            15 -> "Fifteen, strong work"
            20 -> "Twenty, beast mode"
            else -> count.toString()
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rep_$count")
    }

    fun speakFormCue(cue: String, isUrgent: Boolean = false) {
        if (isMuted || !isInitialized) return
        val now = System.currentTimeMillis()
        val lastSpoken = lastSpokenTimestamps[cue] ?: 0L
        if (!isUrgent && (now - lastSpoken < DEBOUNCE_WINDOW_MS)) {
            return // Skip repeated chatter
        }

        lastSpokenTimestamps[cue] = now
        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(cue, queueMode, null, "cue_${cue.hashCode()}")
    }

    fun speakCountdown(seconds: Int) {
        if (isMuted || !isInitialized) return
        tts?.speak(seconds.toString(), TextToSpeech.QUEUE_ADD, null, "countdown_$seconds")
    }

    fun speakStatus(message: String, isUrgent: Boolean = true) {
        if (isMuted || !isInitialized) return
        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(message, queueMode, null, "status_${message.hashCode()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
