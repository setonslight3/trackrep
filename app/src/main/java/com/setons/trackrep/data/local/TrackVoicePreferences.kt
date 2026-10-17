package com.setons.trackrep.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persisted preferences for Track AI Voice Assistant, "Track" Wake Word detection,
 * and spoken voice responses.
 */
object TrackVoicePreferences {
    private const val PREFS_NAME = "trackrep_voice_prefs"
    private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
    private const val KEY_WAKE_WORD = "wake_word_phrase"
    private const val KEY_VOICE_RESPONSES = "voice_responses_enabled"

    // Observable states synchronized with SharedPreferences
    var isWakeWordEnabledState by mutableStateOf(true)
        private set

    var wakeWordState by mutableStateOf("track")
        private set

    var isVoiceResponsesEnabledState by mutableStateOf(true)
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isWakeWordEnabledState = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
            wakeWordState = prefs.getString(KEY_WAKE_WORD, "track") ?: "track"
            isVoiceResponsesEnabledState = prefs.getBoolean(KEY_VOICE_RESPONSES, true)
            isInitialized = true
        }
    }

    fun isWakeWordEnabled(context: Context): Boolean {
        init(context)
        return isWakeWordEnabledState
    }

    fun setWakeWordEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        isWakeWordEnabledState = enabled
    }

    fun getWakeWord(context: Context): String {
        init(context)
        return wakeWordState
    }

    fun setWakeWord(context: Context, phrase: String) {
        val normalized = phrase.trim().lowercase()
        val finalPhrase = if (normalized.isBlank()) "track" else normalized
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WAKE_WORD, finalPhrase).apply()
        wakeWordState = finalPhrase
    }

    fun isVoiceResponsesEnabled(context: Context): Boolean {
        init(context)
        return isVoiceResponsesEnabledState
    }

    fun setVoiceResponsesEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VOICE_RESPONSES, enabled).apply()
        isVoiceResponsesEnabledState = enabled
    }
}
