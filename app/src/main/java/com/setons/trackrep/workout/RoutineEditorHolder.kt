package com.setons.trackrep.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global navigation holder allowing any screen or Track AI to request opening
 * the routine editor / schedule customizer directly on the Home screen.
 */
object RoutineEditorHolder {
    var shouldOpenEditorOnHome by mutableStateOf(false)
    var targetDayDateString: String? by mutableStateOf(null)

    fun requestOpenEditor(dateString: String? = null) {
        targetDayDateString = dateString
        shouldOpenEditorOnHome = true
    }

    fun consumeRequest(): String? {
        if (!shouldOpenEditorOnHome) return null
        val date = targetDayDateString
        shouldOpenEditorOnHome = false
        targetDayDateString = null
        return date ?: ""
    }
}
