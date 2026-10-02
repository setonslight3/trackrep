package com.setons.trackrep

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.setons.trackrep.navigation.CoachNavKey
import com.setons.trackrep.navigation.ExerciseLibraryNavKey
import com.setons.trackrep.navigation.HistoryNavKey
import com.setons.trackrep.navigation.HomeNavKey
import com.setons.trackrep.navigation.OnboardingNavKey
import com.setons.trackrep.navigation.ProfileNavKey
import com.setons.trackrep.navigation.SessionReviewNavKey
import com.setons.trackrep.navigation.TrackAiNavKey
import com.setons.trackrep.review.SessionPlaybackScreen
import com.setons.trackrep.schedule.UserProfileRepository
import com.setons.trackrep.screens.coach.CoachModeHolder
import com.setons.trackrep.screens.coach.CoachScreen
import com.setons.trackrep.screens.history.HistoryScreen
import com.setons.trackrep.screens.home.HomeScreen
import com.setons.trackrep.screens.library.ExerciseLibraryScreen
import com.setons.trackrep.screens.onboarding.OnboardingScreen
import com.setons.trackrep.screens.profile.ProfileScreen
import com.setons.trackrep.screens.trackai.TrackAiScreen
import com.setons.trackrep.theme.ThemeManager
import com.setons.trackrep.theme.TrackRepTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackRepApp() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val systemInDark = isSystemInDarkTheme()
    val isDarkTheme = themeManager.isDark(systemInDark)
    val repository = remember { UserProfileRepository.getInstance(context) }

    TrackRepTheme(
        darkTheme = isDarkTheme,
        darkAccent = themeManager.darkAccent,
        lightAccent = themeManager.lightAccent
    ) {
        val backStack = rememberNavBackStack(HomeNavKey)
        val currentDestination = backStack.lastOrNull() ?: HomeNavKey
        val isFullscreenDestination = currentDestination is SessionReviewNavKey || currentDestination is OnboardingNavKey || CoachModeHolder.isImmersiveFullscreen

        // If first launch, show onboarding and initialize catalog
        LaunchedEffect(Unit) {
            try {
                com.setons.trackrep.exercise.catalog.ExerciseCatalog.initialize(context)
                val completed = repository.isOnboardingCompleted()
                if (!completed) {
                    backStack.add(OnboardingNavKey)
                }
            } catch (e: Exception) {
                android.util.Log.e("TrackRep", "Failed to check onboarding state", e)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (!isFullscreenDestination) {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "TrackRep",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }
                }
            },
            bottomBar = {
                if (!isFullscreenDestination) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        val navItems = listOf(
                            Triple(HomeNavKey, "Today", Icons.Default.Home),
                            Triple(CoachNavKey, "Coach", Icons.Default.FitnessCenter),
                            Triple(TrackAiNavKey, "Track AI", Icons.Default.AutoAwesome),
                            Triple(HistoryNavKey, "History", Icons.Default.History),
                            Triple(ProfileNavKey, "Profile", Icons.Default.Person)
                        )

                        navItems.forEach { (key, label, icon) ->
                            val isSelected = currentDestination == key
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentDestination != key) {
                                        while (backStack.size > 1) {
                                            backStack.removeLastOrNull()
                                        }
                                        if (key != HomeNavKey) {
                                            backStack.add(key)
                                        }
                                    }
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                modifier = if (isFullscreenDestination) Modifier else Modifier.padding(innerPadding),
                entryProvider = entryProvider {
                    entry<HomeNavKey> {
                        HomeScreen(
                            onNavigateToCoach = {
                                while (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                                backStack.add(CoachNavKey)
                            },
                            onNavigateToTrackAi = {
                                while (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                                backStack.add(TrackAiNavKey)
                            },
                            onNavigateToLibrary = {
                                backStack.add(ExerciseLibraryNavKey)
                            }
                        )
                    }
                    entry<CoachNavKey> {
                        CoachScreen(
                            onNavigateToPlayback = { sessionId ->
                                backStack.add(SessionReviewNavKey(sessionId))
                            },
                            onNavigateToLibrary = {
                                backStack.add(ExerciseLibraryNavKey)
                            }
                        )
                    }
                    entry<ExerciseLibraryNavKey> {
                        ExerciseLibraryScreen(
                            onNavigateToCoach = { mode ->
                                CoachModeHolder.pendingExerciseMode = mode
                                while (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                                backStack.add(CoachNavKey)
                            }
                        )
                    }
                    entry<TrackAiNavKey> {
                        TrackAiScreen(
                            onNavigateToCoach = {
                                while (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                                backStack.add(CoachNavKey)
                            },
                            onNavigateToDestination = { destination ->
                                while (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                                when (destination.uppercase()) {
                                    "HISTORY" -> backStack.add(HistoryNavKey)
                                    "COACH" -> backStack.add(CoachNavKey)
                                    "EXERCISE_LIBRARY", "LIBRARY" -> backStack.add(ExerciseLibraryNavKey)
                                    "PROFILE", "THEME", "THEME_STUDIO" -> backStack.add(ProfileNavKey)
                                    "HOME" -> { /* Root HomeNavKey */ }
                                    else -> backStack.add(HistoryNavKey)
                                }
                            }
                        )
                    }
                    entry<HistoryNavKey> {
                        HistoryScreen(
                            onNavigateToPlayback = { sessionId ->
                                backStack.add(SessionReviewNavKey(sessionId))
                            }
                        )
                    }
                    entry<ProfileNavKey> {
                        ProfileScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { checked ->
                                themeManager.setThemeMode(if (checked) "DARK" else "LIGHT")
                            },
                            themeManager = themeManager,
                            onNavigateToOnboarding = {
                                backStack.add(OnboardingNavKey)
                            }
                        )
                    }
                    entry<SessionReviewNavKey> { key ->
                        SessionPlaybackScreen(
                            sessionId = key.sessionId,
                            onNavigateBack = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }
                    entry<OnboardingNavKey> {
                        OnboardingScreen(
                            onFinishOnboarding = {
                                while (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}