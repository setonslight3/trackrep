package com.setons.trackrep.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.setons.trackrep.ui.components.TrackRepSwitch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.reminder.WorkoutReminderScheduler
import com.setons.trackrep.schedule.UserProfileRepository
import com.setons.trackrep.schedule.WorkoutScheduleEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { UserProfileRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    // State
    var selectedGoal by remember { mutableStateOf("Full-Body Muscle Development") }
    var selectedLevel by remember { mutableStateOf("Beginner") }
    var selectedEquipment by remember { mutableStateOf("Bodyweight (No Equipment)") }
    val customEquipment = remember { mutableStateListOf<String>() }
    val selectedDays = remember { mutableStateListOf("MON", "WED", "FRI") }
    var preferredTime by remember { mutableStateOf("MORNING") }
    var reminderHour by remember { mutableIntStateOf(7) }
    var reminderMinute by remember { mutableIntStateOf(30) }
    var workoutDuration by remember { mutableIntStateOf(20) }
    var remindersEnabled by remember { mutableStateOf(true) }
    var isCameraEnabled by remember { mutableStateOf(true) }

    var generatedSchedule by remember { mutableStateOf<List<ScheduledWorkoutEntity>>(emptyList()) }

    // Permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    // Request notification permission if Android 13+ when on schedule step
    LaunchedEffect(currentStep) {
        if (currentStep == 3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (currentStep == 5) {
            // Generate coherent first week
            val tempProfile = UserProfileEntity(
                goal = selectedGoal,
                fitnessLevel = selectedLevel,
                equipment = selectedEquipment,
                customEquipment = customEquipment.joinToString(","),
                availableDaysCsv = selectedDays.joinToString(","),
                preferredTimeOfDay = preferredTime,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                workoutDurationMinutes = workoutDuration,
                isCameraEnabled = isCameraEnabled,
                remindersEnabled = remindersEnabled,
                isOnboardingCompleted = false
            )
            generatedSchedule = WorkoutScheduleEngine.generateCoherentFirstWeek(tempProfile)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Step progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                IconButton(onClick = { currentStep-- }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "STEP ${currentStep + 1} OF $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1f) / totalSteps.toFloat() },
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (currentStep < totalSteps - 1) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { currentStep = totalSteps - 1 }
                        .padding(8.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step Content with animation
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> StepGoalSelection(
                        selectedGoal = selectedGoal,
                        onSelectGoal = { selectedGoal = it }
                    )
                    1 -> StepFitnessLevel(
                        selectedLevel = selectedLevel,
                        onSelectLevel = { selectedLevel = it }
                    )
                    2 -> StepEquipment(
                        selectedEquipment = selectedEquipment,
                        onSelectEquipment = { selectedEquipment = it },
                        customEquipment = customEquipment
                    )
                    3 -> StepSchedule(
                        selectedDays = selectedDays,
                        preferredTime = preferredTime,
                        onSelectTime = { time, h, m ->
                            preferredTime = time
                            reminderHour = h
                            reminderMinute = m
                        },
                        duration = workoutDuration,
                        onSelectDuration = { workoutDuration = it },
                        remindersEnabled = remindersEnabled,
                        onToggleReminders = { remindersEnabled = it }
                    )
                    4 -> StepCameraPrivacy(
                        isCameraEnabled = isCameraEnabled,
                        onToggleCamera = { isCameraEnabled = it },
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    5 -> StepGeneratedWeek(
                        schedule = generatedSchedule,
                        level = selectedLevel,
                        daysCount = selectedDays.size
                    )
                }
            }
        }

        // Bottom action button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (currentStep < totalSteps - 1) {
                    currentStep++
                } else {
                    // Finalize onboarding
                    scope.launch {
                        val profile = UserProfileEntity(
                            goal = selectedGoal,
                            fitnessLevel = selectedLevel,
                            equipment = selectedEquipment,
                            customEquipment = customEquipment.joinToString(","),
                            availableDaysCsv = selectedDays.joinToString(","),
                            preferredTimeOfDay = preferredTime,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            workoutDurationMinutes = workoutDuration,
                            isCameraEnabled = isCameraEnabled,
                            remindersEnabled = remindersEnabled,
                            isOnboardingCompleted = true
                        )
                        repository.saveProfile(profile)
                        repository.saveWeeklySchedule(generatedSchedule)
                        if (remindersEnabled) {
                            WorkoutReminderScheduler.scheduleNextReminder(context)
                        }
                        onFinishOnboarding()
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (currentStep == totalSteps - 1) "Start Training Journey ⚡" else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (currentStep < totalSteps - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepGoalSelection(
    selectedGoal: String,
    onSelectGoal: (String) -> Unit
) {
    val goals = listOf(
        Pair("Full-Body Muscle Development", "Balanced hypertrophy across chest, shoulders, arms, back, core, glutes, quads & calves (Recommended)"),
        Pair("Calisthenics Strength & Mastery", "Master clean pull-ups, strict push-ups, pistol squats, and isometric bodyweight holds"),
        Pair("Athletic Conditioning & Burn", "High-cadence metabolic circuits designed for cardiovascular conditioning and lean power"),
        Pair("Mobility & Injury Prevention", "Joint-friendly progressions with extra focus on shoulder integrity and core bracing")
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Welcome to TrackRep",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Engineered by Setons • What is your primary training goal?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(20.dp))

        goals.forEach { (title, subtitle) ->
            val isSelected = title == selectedGoal
            OutlinedCard(
                onClick = { onSelectGoal(title) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepFitnessLevel(
    selectedLevel: String,
    onSelectLevel: (String) -> Unit
) {
    val levels = listOf(
        Triple("Beginner", "Tier 1: Foundation", "Wall push-ups, chair squats, knee planks. Developing joint mobility and baseline posture."),
        Triple("Novice", "Tier 2: Elevation", "Incline push-ups, reverse lunges, side planks. Building strength to reach ground-level repetitions."),
        Triple("Intermediate", "Tier 3: Standard Athletics", "Floor push-ups, full bodyweight squats, standard elbow planks. Solid volume capacity."),
        Triple("Advanced", "Tier 4: Dynamic Calisthenics", "Diamond push-ups, Bulgarian split squats, pike presses, hollow holds."),
        Triple("Expert", "Tier 5: Elite Neuromuscular", "Archer push-ups, pistol squats, dragon flags, handstand presses.")
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Experience Baseline",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Track will tailor workout difficulty and adaptive progressions to your current tier.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        levels.forEach { (level, tag, desc) ->
            val isSelected = level == selectedLevel
            OutlinedCard(
                onClick = { onSelectLevel(level) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepEquipment(
    selectedEquipment: String,
    onSelectEquipment: (String) -> Unit,
    customEquipment: MutableList<String>
) {
    val equipmentPresets = listOf(
        Pair("Bodyweight (No Equipment)", "100% calisthenics using only floor space and bodyweight resistance"),
        Pair("Some Equipment (Bar / Bands)", "Access to a pull-up bar, resistance bands, or chair/bench"),
        Pair("Full Gym / Calisthenics Park", "Access to dip bars, gymnastic rings, weights, and high bars")
    )

    val customItems = listOf("Pull-up Bar", "Resistance Bands", "Parallettes", "Dip Station", "Gymnastic Rings", "Dumbbells")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Equipment Selection",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "TrackRep is bodyweight-first: all workouts can be done with zero equipment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        equipmentPresets.forEach { (preset, desc) ->
            val isSelected = preset == selectedEquipment
            OutlinedCard(
                onClick = { onSelectEquipment(preset) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Optional Specific Gear (Tap to toggle):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            customItems.forEach { item ->
                val checked = item in customEquipment
                Surface(
                    onClick = {
                        if (checked) customEquipment.remove(item) else customEquipment.add(item)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (checked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                            color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSchedule(
    selectedDays: MutableList<String>,
    preferredTime: String,
    onSelectTime: (String, Int, Int) -> Unit,
    duration: Int,
    onSelectDuration: (Int) -> Unit,
    remindersEnabled: Boolean,
    onToggleReminders: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Schedule & Reminders",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Choose your training days and preferred workout time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Days selector
        Text(
            text = "Training Days (${selectedDays.size} days/week):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            allDays.forEach { day ->
                val isSelected = day in selectedDays
                Surface(
                    onClick = {
                        if (isSelected) {
                            if (selectedDays.size > 2) selectedDays.remove(day) // enforce at least 2 days
                        } else {
                            selectedDays.add(day)
                        }
                    },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(42.dp)
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

        Spacer(modifier = Modifier.height(10.dp))
        // Quick presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    selectedDays.clear()
                    selectedDays.addAll(listOf("MON", "WED", "FRI"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("3 Days", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = {
                    selectedDays.clear()
                    selectedDays.addAll(listOf("MON", "TUE", "THU", "FRI"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("4 Days", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = {
                    selectedDays.clear()
                    selectedDays.addAll(listOf("MON", "TUE", "WED", "THU", "FRI"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("5 Days", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Time of Day
        Text(
            text = "Preferred Workout Time:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isMorning = preferredTime == "MORNING"
            OutlinedCard(
                onClick = { onSelectTime("MORNING", 7, 30) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (isMorning) 2.dp else 1.dp, if (isMorning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.outlinedCardColors(containerColor = if (isMorning) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Morning", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("07:30 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            val isEvening = preferredTime == "EVENING"
            OutlinedCard(
                onClick = { onSelectTime("EVENING", 18, 0) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (isEvening) 2.dp else 1.dp, if (isEvening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.outlinedCardColors(containerColor = if (isEvening) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Evening", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("06:00 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Session Duration
        Text(
            text = "Target Session Duration:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 20, 30, 45).forEach { d ->
                val isSelected = duration == d
                Surface(
                    onClick = { onSelectDuration(d) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${d}m",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Reminder Toggle
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Workout Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Notification before daily workout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
                TrackRepSwitch(
                    checked = remindersEnabled,
                    onCheckedChange = onToggleReminders
                )
            }
        }
    }
}

@Composable
private fun StepCameraPrivacy(
    isCameraEnabled: Boolean,
    onToggleCamera: (Boolean) -> Unit,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "AI Vision & Privacy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Real-time rep counting and biomechanical posture analysis on-device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(18.dp))

        // Privacy Guarantee Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("100% On-Device Privacy Guarantee", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TrackRep processes your camera feed entirely in your phone's memory using on-device ML Kit pose detection. No video, image, or audio is ever uploaded to any cloud server or third party.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Positioning Tutorial Card
        OutlinedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Optimal Camera Placement", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Place phone on floor leaned against a wall (~15° tilt)\n• Step back 5 to 7 normal paces\n• Ensure full body (head to feet) is in camera frame\n• Coach will announce 'Ready' when aligned!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Card
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasCameraPermission) "Camera Permission Granted" else "Camera Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (hasCameraPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasCameraPermission) "Ready for live pose tracking" else "Needed for automatic rep counting",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (!hasCameraPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Grant", fontSize = 12.sp)
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StepGeneratedWeek(
    schedule: List<ScheduledWorkoutEntity>,
    level: String,
    daysCount: Int
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Your First Week is Ready! ⚡",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Coherent 7-day schedule personalized for $level level ($daysCount active days + recovery safeguards).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        schedule.forEach { day ->
            OutlinedCard(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (day.isRestDay) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (day.isRestDay) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(44.dp)
                    ) {
                        Text(
                            text = day.dayOfWeek.take(3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (day.isRestDay) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = day.dateString.takeLast(2),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = day.routineName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (day.isRestDay) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (day.isRestDay) "Active recovery • Muscle consolidation" else "Focus: ${day.targetMusclesCsv.replace(",", ", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day.isRestDay) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (day.isRestDay) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (day.isRestDay) "REST" else "TRAIN",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (day.isRestDay) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}