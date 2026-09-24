package com.setons.trackrep.screens.profile

import android.os.Build
import com.setons.trackrep.BuildConfig
import com.setons.trackrep.ui.components.TrackRepSwitch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.reminder.WorkoutReminderScheduler
import com.setons.trackrep.schedule.UserProfileRepository
import com.setons.trackrep.schedule.WorkoutScheduleEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { UserProfileRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var profile by remember { mutableStateOf(UserProfileEntity()) }
    var selectedLevel by remember { mutableStateOf("Beginner") }
    var selectedEquipment by remember { mutableStateOf("Bodyweight (No Equipment)") }
    val selectedDays = remember { mutableStateListOf("MON", "WED", "FRI") }
    var preferredTime by remember { mutableStateOf("MORNING") }
    var remindersEnabled by remember { mutableStateOf(true) }
    var workoutDuration by remember { mutableIntStateOf(20) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val loaded = repository.getProfile()
        profile = loaded
        selectedLevel = loaded.fitnessLevel
        selectedEquipment = loaded.equipment
        selectedDays.clear()
        selectedDays.addAll(loaded.availableDaysCsv.split(",").filter { it.isNotBlank() })
        if (selectedDays.isEmpty()) selectedDays.addAll(listOf("MON", "WED", "FRI"))
        preferredTime = loaded.preferredTimeOfDay
        remindersEnabled = loaded.remindersEnabled
        workoutDuration = loaded.workoutDurationMinutes
    }

    fun persistChanges(message: String = "Preferences updated & schedule recalibrated") {
        scope.launch {
            val updated = profile.copy(
                fitnessLevel = selectedLevel,
                equipment = selectedEquipment,
                availableDaysCsv = selectedDays.joinToString(","),
                preferredTimeOfDay = preferredTime,
                remindersEnabled = remindersEnabled,
                workoutDurationMinutes = workoutDuration
            )
            repository.saveProfile(updated)
            // Recalibrate schedule
            val newWeek = WorkoutScheduleEngine.generateCoherentFirstWeek(updated)
            repository.saveWeeklySchedule(newWeek)
            if (remindersEnabled) {
                WorkoutReminderScheduler.scheduleNextReminder(context)
            } else {
                WorkoutReminderScheduler.cancelReminder(context)
            }
            saveStatusMessage = message
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Adaptive training preferences, schedule & reminders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        saveStatusMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✓ $msg",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Theme Toggle Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (isDarkTheme) "Dark Theme: Black + Gold" else "Light Theme: White + Red",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDarkTheme) "Deep black canvas with metallic gold accents" else "Clean white canvas with crimson accents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                TrackRepSwitch(
                    checked = isDarkTheme,
                    onCheckedChange = onToggleTheme
                )
            }
        }

        // Fitness Level Card
        OutlinedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Experience Level (5 Tiers)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val levels = listOf("Beginner", "Novice", "Intermediate", "Advanced", "Expert")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    levels.forEach { level ->
                        val isSelected = level == selectedLevel
                        Surface(
                            onClick = {
                                selectedLevel = level
                                persistChanges("Updated level to $level")
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = level,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Training Schedule & Active Days Card
        OutlinedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Weekly Training Days (${selectedDays.size} days/week)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    allDays.forEach { day ->
                        val isSelected = day in selectedDays
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    if (selectedDays.size > 2) selectedDays.remove(day)
                                } else {
                                    selectedDays.add(day)
                                }
                                persistChanges("Updated training schedule days")
                            },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day.take(1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preferred Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMorning = preferredTime == "MORNING"
                    Surface(
                        onClick = {
                            preferredTime = "MORNING"
                            persistChanges("Set preferred time to Morning (07:30 AM)")
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMorning) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isMorning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Morning (7:30 AM)", fontSize = 12.sp, fontWeight = if (isMorning) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    val isEvening = preferredTime == "EVENING"
                    Surface(
                        onClick = {
                            preferredTime = "EVENING"
                            persistChanges("Set preferred time to Evening (06:00 PM)")
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEvening) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isEvening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Evening (6:00 PM)", fontSize = 12.sp, fontWeight = if (isEvening) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reminder switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daily Workout Notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TrackRepSwitch(
                        checked = remindersEnabled,
                        onCheckedChange = {
                            remindersEnabled = it
                            persistChanges(if (it) "Enabled daily reminders" else "Disabled daily reminders")
                        }
                    )
                }
            }
        }

        // Available Equipment Card
        OutlinedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Equipment Setup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val equipmentOptions = listOf("Bodyweight (No Equipment)", "Some Equipment (Bar / Bands)", "Full Gym / Calisthenics Park")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    equipmentOptions.forEach { opt ->
                        val isSelected = opt == selectedEquipment
                        Surface(
                            onClick = {
                                selectedEquipment = opt
                                persistChanges("Updated equipment to $opt")
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = opt,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Re-run Onboarding Setup
        OutlinedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Onboarding & Schedule Wizard",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Re-launch the 6-step setup flow to regenerate your 7-day program from scratch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.resetOnboarding()
                            onNavigateToOnboarding()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Re-run Onboarding Setup ⚡", fontWeight = FontWeight.Bold)
                }
            }
        }

        // About & Developer Attribution Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "About TrackRep",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "TrackRep • Developed by Setons\n" +
                            "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n" +
                            "AI Assistant: Track (Powered by Gemini + On-Device NLP)\n" +
                            "Architecture: Local-First • On-Device Vision • Room DB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Universal Device Compatibility:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "• Android: Universal support across Samsung Galaxy, Google Pixel, Xiaomi, OnePlus, Motorola, Tecno, Infinix, and all Android 8.0+ devices (API 24–36)\n" +
                            "• iOS & iPadOS: Cross-platform Web & PWA access with real-time camera tracking\n" +
                            "• Laptops & Desktops: Windows, macOS, Linux & ChromeOS via any webcam or browser\n" +
                            "• Universal Vision: Any smartphone, tablet, or laptop camera — zero external sensors required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}