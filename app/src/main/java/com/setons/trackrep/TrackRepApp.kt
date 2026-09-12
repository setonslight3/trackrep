package com.setons.trackrep

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.setons.trackrep.navigation.CoachNavKey
import com.setons.trackrep.navigation.HistoryNavKey
import com.setons.trackrep.navigation.HomeNavKey
import com.setons.trackrep.navigation.ProfileNavKey
import com.setons.trackrep.navigation.TrackAiNavKey
import com.setons.trackrep.screens.coach.CoachScreen
import com.setons.trackrep.screens.history.HistoryScreen
import com.setons.trackrep.screens.home.HomeScreen
import com.setons.trackrep.screens.profile.ProfileScreen
import com.setons.trackrep.screens.trackai.TrackAiScreen
import com.setons.trackrep.theme.TrackRepTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackRepApp() {
    val systemInDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemInDark) }

    TrackRepTheme(darkTheme = isDarkTheme) {
        val backStack = rememberNavBackStack(HomeNavKey)
        val currentDestination = backStack.lastOrNull() ?: HomeNavKey

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
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
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
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
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                modifier = Modifier.padding(innerPadding),
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
                            }
                        )
                    }
                    entry<CoachNavKey> {
                        CoachScreen()
                    }
                    entry<TrackAiNavKey> {
                        TrackAiScreen()
                    }
                    entry<HistoryNavKey> {
                        HistoryScreen()
                    }
                    entry<ProfileNavKey> {
                        ProfileScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = it }
                        )
                    }
                }
            )
        }
    }
}
