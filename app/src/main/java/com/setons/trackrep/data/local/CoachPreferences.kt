package com.setons.trackrep.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persisted preferences for AI Vision Coaching, Auto-Detection, Hands-Free Countdown,
 * Beginner Ghost Shadow Form Overlay, and Tempo Metronome.
 */
object CoachPreferences {
    private const val PREFS_NAME = "trackrep_coach_prefs"
    private const val KEY_AUTO_DETECT = "auto_detect_exercise_enabled"
    private const val KEY_HANDS_FREE_START = "hands_free_auto_start_enabled"
    private const val KEY_GHOST_SHADOW = "ghost_shadow_overlay_enabled"
    private const val KEY_TEMPO_PRESET = "tempo_metronome_preset"

    // In-memory observable state synchronized with SharedPreferences
    var isAutoDetectState by mutableStateOf(false)
        private set

    var isHandsFreeStartState by mutableStateOf(true)
        private set

    var isGhostShadowState by mutableStateOf(false)
        private set

    var tempoPresetState by mutableStateOf("OFF")
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isAutoDetectState = prefs.getBoolean(KEY_AUTO_DETECT, false)
            isHandsFreeStartState = prefs.getBoolean(KEY_HANDS_FREE_START, true)
            isGhostShadowState = prefs.getBoolean(KEY_GHOST_SHADOW, false)
            tempoPresetState = prefs.getString(KEY_TEMPO_PRESET, "OFF") ?: "OFF"
            isInitialized = true
        }
    }

    fun isAutoDetectEnabled(context: Context): Boolean {
        init(context)
        return isAutoDetectState
    }

    fun setAutoDetectEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_DETECT, enabled).apply()
        isAutoDetectState = enabled
    }

    fun isHandsFreeStartEnabled(context: Context): Boolean {
        init(context)
        return isHandsFreeStartState
    }

    fun setHandsFreeStartEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HANDS_FREE_START, enabled).apply()
        isHandsFreeStartState = enabled
    }

    fun isGhostShadowEnabled(context: Context): Boolean {
        init(context)
        return isGhostShadowState
    }

    fun setGhostShadowEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GHOST_SHADOW, enabled).apply()
        isGhostShadowState = enabled
    }

    fun getTempoPreset(context: Context): String {
        init(context)
        return tempoPresetState
    }

    fun setTempoPreset(context: Context, presetId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TEMPO_PRESET, presetId).apply()
        tempoPresetState = presetId
    }
}
