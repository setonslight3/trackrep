package com.setons.trackrep.exercise.model

import com.setons.trackrep.camera.ExerciseFramingMode

enum class BodyRegion(val displayName: String) {
    UPPER_BODY("Upper Body"),
    LOWER_BODY("Lower Body"),
    CORE("Core"),
    FULL_BODY("Full Body")
}

enum class MuscleGroup(val displayName: String, val region: BodyRegion) {
    CHEST("Chest", BodyRegion.UPPER_BODY),
    BACK("Back", BodyRegion.UPPER_BODY),
    SHOULDERS("Shoulders", BodyRegion.UPPER_BODY),
    ARMS("Arms", BodyRegion.UPPER_BODY),
    CORE("Core & Abs", BodyRegion.CORE),
    QUADS("Quads", BodyRegion.LOWER_BODY),
    HAMSTRINGS("Hamstrings", BodyRegion.LOWER_BODY),
    GLUTES("Glutes", BodyRegion.LOWER_BODY),
    CALVES("Calves", BodyRegion.LOWER_BODY),
    FULL_BODY("Full Body", BodyRegion.FULL_BODY)
}

enum class DifficultyLevel(
    val displayName: String,
    val rank: Int,
    val description: String
) {
    BEGINNER("Beginner", 1, "Foundational movements with minimal joint load"),
    NOVICE("Novice", 2, "Building baseline strength, balance & stability"),
    INTERMEDIATE("Intermediate", 3, "Standard bodyweight calisthenics with AI Vision"),
    ADVANCED("Advanced", 4, "High leverage, increased volume & explosive power"),
    ELITE("Elite", 5, "Unilateral bodyweight mastery & peak control")
}

enum class EquipmentType(val displayName: String) {
    BODYWEIGHT("Bodyweight / Living Room"),
    DUMBBELL("Dumbbells"),
    RESISTANCE_BAND("Resistance Band"),
    PULLUP_BAR("Pull-up Bar"),
    FULL_GYM("Full Gym")
}

data class Exercise(
    val id: String,
    val name: String,
    val targetMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val difficulty: DifficultyLevel,
    val equipment: EquipmentType = EquipmentType.BODYWEIGHT,
    val framingMode: ExerciseFramingMode? = null,
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultHoldSeconds: Int = 0,
    val restSeconds: Int = 60,
    val instructions: List<String>,
    val commonFlaws: List<String>,
    val proTip: String
) {
    val isVisionSupported: Boolean
        get() = framingMode != null

    val isIsometric: Boolean
        get() = defaultHoldSeconds > 0
}

data class WorkoutItem(
    val exerciseId: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetHoldSeconds: Int = 0,
    val restSeconds: Int = 60
)

data class WorkoutRoutine(
    val id: String,
    val name: String,
    val tagline: String,
    val difficulty: DifficultyLevel,
    val estimatedMinutes: Int,
    val targetMuscles: List<MuscleGroup>,
    val items: List<WorkoutItem>
)
