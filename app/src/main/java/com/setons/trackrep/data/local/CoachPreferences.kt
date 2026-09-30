package com.setons.trackrep.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persisted preferences for AI Vision Coaching and Auto-Detection.
 */
object CoachPreferences {
    private const val PREFS_NAME = "trackrep_coach_prefs"
    private const val KEY_AUTO_DETECT = "auto_detect_exercise_enabled"

    // In-memory observable state synchronized with SharedPreferences
    var isAutoDetectState by mutableStateOf(false)
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isAutoDetectState = prefs.getBoolean(KEY_AUTO_DETECT, false)
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
}
