package com.setons.trackrep.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.TrendingUp
import com.setons.trackrep.adaptive.AdaptiveRepository
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.theme.SuccessGreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.Exercise
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.exercise.model.WorkoutRoutine
import com.setons.trackrep.theme.SuccessGreen
import com.setons.trackrep.screens.coach.CoachModeHolder
import com.setons.trackrep.ui.demo.StickmanDemoPlayer
import com.setons.trackrep.workout.WorkoutEngine

enum class LibraryTab {
    EXERCISES,
    ROUTINES
}

@Composable
fun ExerciseLibraryScreen(
    onNavigateToCoach: (ExerciseFramingMode?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.EXERCISES) }
    var selectedExerciseForDetail by remember { mutableStateOf<Exercise?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedDifficultyFilter by remember { mutableStateOf<DifficultyLevel?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Athletic Header & Tab Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Training Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "30+ calibrated movements • 5 difficulty tiers • Balanced workouts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector: Exercises vs Routines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { selectedTab = LibraryTab.EXERCISES },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == LibraryTab.EXERCISES) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (selectedTab == LibraryTab.EXERCISES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exercises (${ExerciseCatalog.getAll().size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == LibraryTab.EXERCISES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = LibraryTab.ROUTINES },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == LibraryTab.ROUTINES) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (selectedTab == LibraryTab.ROUTINES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Routines (${WorkoutEngine.getRoutines().size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == LibraryTab.ROUTINES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            LibraryTab.EXERCISES -> {
                ExercisesTabContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedMuscle = selectedMuscleFilter,
                    onMuscleSelect = { selectedMuscleFilter = if (selectedMuscleFilter == it) null else it },
                    selectedDifficulty = selectedDifficultyFilter,
                    onDifficultySelect = { selectedDifficultyFilter = if (selectedDifficultyFilter == it) null else it },
                    onExerciseClick = { selectedExerciseForDetail = it }
                )
            }
            LibraryTab.ROUTINES -> {
                RoutinesTabContent(
                    routines = WorkoutEngine.getRoutines(),
                    onSelectRoutine = { routine ->
                        val firstItem = routine.items.firstOrNull()
                        val ex = firstItem?.let { ExerciseCatalog.getById(it.exerciseId) }
                        if (ex != null && firstItem != null) {
                            CoachModeHolder.setPending(
                                exerciseId = ex.id,
                                framingMode = ex.framingMode ?: ExerciseFramingMode.PUSH_UP,
                                targetReps = firstItem.targetReps,
                                targetHoldSeconds = firstItem.targetHoldSeconds,
                                targetSets = firstItem.targetSets,
                                cameraEnabled = true,
                                routine = routine,
                                routineIndex = 0
                            )
                        }
                        onNavigateToCoach(ex?.framingMode ?: ExerciseFramingMode.PUSH_UP)
                    }
                )
            }
        }
    }

    // Exercise Detail Dialog
    selectedExerciseForDetail?.let { exercise ->
        ExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { selectedExerciseForDetail = null },
            onLaunchCoach = { mode ->
                selectedExerciseForDetail = null
                val resolvedMode = mode ?: exercise.framingMode ?: ExerciseFramingMode.PUSH_UP
                CoachModeHolder.setPending(
                    exerciseId = exercise.id,
                    framingMode = resolvedMode,
                    targetReps = exercise.defaultReps,
                    targetHoldSeconds = exercise.defaultHoldSeconds,
                    targetSets = exercise.defaultSets,
                    cameraEnabled = true
                )
                onNavigateToCoach(resolvedMode)
            }
        )
    }
}

@Composable
fun ExercisesTabContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedMuscle: MuscleGroup?,
    onMuscleSelect: (MuscleGroup) -> Unit,
    selectedDifficulty: DifficultyLevel?,
    onDifficultySelect: (DifficultyLevel) -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    val allExercises = remember { ExerciseCatalog.getAll() }
    val filteredExercises = remember(searchQuery, selectedMuscle, selectedDifficulty) {
        allExercises.filter { ex ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                ex.name.lowercase().contains(q) ||
                ex.targetMuscle.displayName.lowercase().contains(q) ||
                ex.secondaryMuscles.any { it.displayName.lowercase().contains(q) } ||
                ex.difficulty.displayName.lowercase().contains(q)
            }
            val matchesMuscle = selectedMuscle == null || ex.targetMuscle == selectedMuscle || ex.secondaryMuscles.contains(selectedMuscle)
            val matchesDiff = selectedDifficulty == null || ex.difficulty == selectedDifficulty
            matchesSearch && matchesMuscle && matchesDiff
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by exercise, muscle, or level...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(52.dp)
        )

        // Muscle Group Filter Chips Row
        val muscleScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(muscleScroll)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MuscleGroup.values().forEach { muscle ->
                val isSelected = selectedMuscle == muscle
                Surface(
                    onClick = { onMuscleSelect(muscle) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = muscle.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Difficulty Tier Filter Chips Row
        val diffScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(diffScroll)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DifficultyLevel.values().forEach { level ->
                val isSelected = selectedDifficulty == level
                Surface(
                    onClick = { onDifficultySelect(level) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "L${level.rank}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = level.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        // Exercise List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MOVEMENTS (${filteredExercises.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (selectedMuscle != null || selectedDifficulty != null || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Filtered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            items(filteredExercises, key = { it.id }) { exercise ->
                ExerciseCard(exercise = exercise, onClick = { onExerciseClick(exercise) })
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (exercise.isVisionSupported) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Badges row: Target muscle + Difficulty + Volume
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Muscle Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = exercise.targetMuscle.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Difficulty Tier
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "L${exercise.difficulty.rank} • ${exercise.difficulty.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Recommended Reps / Hold
                    val volumeText = if (exercise.isIsometric) {
                        "${exercise.defaultHoldSeconds}s hold"
                    } else {
                        "${exercise.defaultReps} reps"
                    }
                    Text(
                        text = "${exercise.defaultSets} × $volumeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: AI Vision badge or Arrow
            if (exercise.isVisionSupported) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "AI Vision",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoutinesTabContent(
    routines: List<WorkoutRoutine>,
    onSelectRoutine: (WorkoutRoutine) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "CURATED WORKOUT ROUTINES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        items(routines, key = { it.id }) { routine ->
            RoutineCard(routine = routine, onStartClick = { onSelectRoutine(routine) })
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RoutineCard(
    routine: WorkoutRoutine,
    onStartClick: () -> Unit
) {
    val exercises = remember(routine) { WorkoutEngine.getExercisesForRoutine(routine) }
    val visionCount = remember(exercises) { exercises.count { it.second.isVisionSupported } }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title + Difficulty & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = routine.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${routine.estimatedMinutes} MIN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Muscle badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Level ${routine.difficulty.rank} • ${routine.difficulty.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                if (visionCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$visionCount AI Vision Moves",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exercise items preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                exercises.forEachIndexed { index, (item, ex) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ex.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ex.isVisionSupported) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        val volume = if (item.targetHoldSeconds > 0) "${item.targetHoldSeconds}s hold" else "${item.targetReps} reps"
                        Text(
                            text = "${item.targetSets} sets × $volume",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start Routine Button
            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Start Routine with Coach", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onLaunchCoach: (ExerciseFramingMode?) -> Unit
) {
    val context = LocalContext.current
    var progression by remember { mutableStateOf<ExerciseProgressionEntity?>(null) }
    LaunchedEffect(exercise.id) {
        progression = AdaptiveRepository.getProgressionForExercise(context, exercise.id)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${exercise.difficulty.displayName} • ${exercise.equipment.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                // AI Vision Badge Banner if supported
                if (exercise.isVisionSupported) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "AI Vision Coach Supported",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Real-time on-device joint angle analysis and rep counting.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // AI Biomechanical Stickman Demonstration
                StickmanDemoPlayer(
                    exerciseId = exercise.id,
                    heightDp = 210,
                    showControls = true
                )

                // Personal Progression & PR Card (if trained before)
                progression?.let { prog ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Personal Record & Target", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    val prText = if (exercise.isIsometric) "PR: ${prog.personalRecordHoldSeconds}s hold" else "PR: ${prog.personalRecordReps} reps"
                                    Text(prText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = SuccessGreen.copy(alpha = 0.2f)) {
                                val targetText = if (exercise.isIsometric) "Target: ${prog.targetHoldSeconds}s" else "Target: ${prog.targetReps} reps"
                                Text(targetText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                    }
                }

                // Volume Recommendation Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${exercise.defaultSets}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val targetLabel = if (exercise.isIsometric) "Hold" else "Target"
                            val targetVal = if (exercise.isIsometric) "${exercise.defaultHoldSeconds}s" else "${exercise.defaultReps} reps"
                            Text(targetLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(targetVal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${exercise.restSeconds}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Execution Steps
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FORM EXECUTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    exercise.instructions.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Common Flaws to Avoid
                if (exercise.commonFlaws.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "COMMON MISTAKES TO AVOID",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        exercise.commonFlaws.forEach { flaw ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(14.dp))
                                Text(
                                    text = flaw,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Pro Tip Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("COACH PRO TIP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = exercise.proTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Launch Coach Action Button
                Button(
                    onClick = {
                        onLaunchCoach(exercise.framingMode)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (exercise.isVisionSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (exercise.isVisionSupported) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (exercise.isVisionSupported) Icons.Default.CameraAlt else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (exercise.isVisionSupported) "Launch in AI Coach" else "Start Exercise",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
