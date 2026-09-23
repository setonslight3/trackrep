package com.setons.trackrep.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeNavKey : NavKey

@Serializable
data object CoachNavKey : NavKey

@Serializable
data object TrackAiNavKey : NavKey

@Serializable
data object HistoryNavKey : NavKey

@Serializable
data object ProfileNavKey : NavKey

@Serializable
data object ExerciseLibraryNavKey : NavKey

@Serializable
data class SessionReviewNavKey(val sessionId: String) : NavKey

@Serializable
data object OnboardingNavKey : NavKey
