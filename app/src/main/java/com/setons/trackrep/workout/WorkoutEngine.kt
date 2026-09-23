package com.setons.trackrep.workout

import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.BodyRegion
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.Exercise
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.exercise.model.WorkoutItem
import com.setons.trackrep.exercise.model.WorkoutRoutine

object WorkoutEngine {

    val curatedRoutines: List<WorkoutRoutine> = listOf(
        WorkoutRoutine(
            id = "routine_full_body_foundation",
            name = "Full-Body Foundation",
            tagline = "Joint-friendly introduction to bodyweight mechanics & core bracing",
            difficulty = DifficultyLevel.BEGINNER,
            estimatedMinutes = 15,
            targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE, MuscleGroup.BACK, MuscleGroup.GLUTES),
            items = listOf(
                WorkoutItem("push_up_wall", targetSets = 3, targetReps = 12, restSeconds = 45),
                WorkoutItem("squat_chair", targetSets = 3, targetReps = 12, restSeconds = 45),
                WorkoutItem("plank_knee", targetSets = 3, targetReps = 0, targetHoldSeconds = 30, restSeconds = 45),
                WorkoutItem("glute_bridge", targetSets = 3, targetReps = 15, restSeconds = 45),
                WorkoutItem("bird_dog", targetSets = 3, targetReps = 10, restSeconds = 45)
            )
        ),
        WorkoutRoutine(
            id = "routine_novice_builder",
            name = "Baseline Strength & Balance",
            tagline = "Elevated pressing, unilateral leg control & posterior chain activation",
            difficulty = DifficultyLevel.NOVICE,
            estimatedMinutes = 20,
            targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE, MuscleGroup.BACK, MuscleGroup.ARMS),
            items = listOf(
                WorkoutItem("push_up_incline", targetSets = 3, targetReps = 10, restSeconds = 60),
                WorkoutItem("lunge_reverse", targetSets = 3, targetReps = 10, restSeconds = 60),
                WorkoutItem("plank_side", targetSets = 3, targetReps = 0, targetHoldSeconds = 30, restSeconds = 45),
                WorkoutItem("bench_dips", targetSets = 3, targetReps = 12, restSeconds = 60),
                WorkoutItem("superman_hold", targetSets = 3, targetReps = 12, restSeconds = 45)
            )
        ),
        WorkoutRoutine(
            id = "routine_total_body_burn",
            name = "Total-Body Athletic Burn",
            tagline = "Signature TrackRep full-body session with AI Vision on-device coaching",
            difficulty = DifficultyLevel.INTERMEDIATE,
            estimatedMinutes = 25,
            targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE, MuscleGroup.SHOULDERS, MuscleGroup.GLUTES),
            items = listOf(
                WorkoutItem("push_up_standard", targetSets = 3, targetReps = 10, restSeconds = 60),
                WorkoutItem("squat_bodyweight", targetSets = 3, targetReps = 12, restSeconds = 60),
                WorkoutItem("plank_standard", targetSets = 3, targetReps = 0, targetHoldSeconds = 45, restSeconds = 60),
                WorkoutItem("pike_push_up", targetSets = 3, targetReps = 8, restSeconds = 60),
                WorkoutItem("single_leg_bridge", targetSets = 3, targetReps = 10, restSeconds = 60)
            )
        ),
        WorkoutRoutine(
            id = "routine_upper_core_blast",
            name = "Upper Body Push & Core",
            tagline = "Hypertrophy-focused diamond pressing, pike shoulders & hollow gymnastics",
            difficulty = DifficultyLevel.ADVANCED,
            estimatedMinutes = 22,
            targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS, MuscleGroup.CORE),
            items = listOf(
                WorkoutItem("push_up_diamond", targetSets = 3, targetReps = 8, restSeconds = 60),
                WorkoutItem("push_up_decline", targetSets = 3, targetReps = 8, restSeconds = 75),
                WorkoutItem("pike_push_up", targetSets = 3, targetReps = 8, restSeconds = 60),
                WorkoutItem("hollow_body_hold", targetSets = 3, targetReps = 0, targetHoldSeconds = 30, restSeconds = 60),
                WorkoutItem("v_ups", targetSets = 3, targetReps = 10, restSeconds = 60)
            )
        ),
        WorkoutRoutine(
            id = "routine_lower_posterior_power",
            name = "Legs & Posterior Power",
            tagline = "Explosive quad jumps, unilateral split squats & calf development",
            difficulty = DifficultyLevel.ADVANCED,
            estimatedMinutes = 22,
            targetMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
            items = listOf(
                WorkoutItem("squat_jump", targetSets = 3, targetReps = 10, restSeconds = 75),
                WorkoutItem("squat_bulgarian", targetSets = 3, targetReps = 8, restSeconds = 60),
                WorkoutItem("good_mornings", targetSets = 3, targetReps = 12, restSeconds = 45),
                WorkoutItem("calf_raise_single", targetSets = 3, targetReps = 12, restSeconds = 45),
                WorkoutItem("squat_bodyweight", targetSets = 3, targetReps = 15, restSeconds = 60)
            )
        ),
        WorkoutRoutine(
            id = "routine_elite_calisthenics",
            name = "Elite Calisthenics Master",
            tagline = "Peak neuromuscular strength: unilateral pistols, archers & dragon flags",
            difficulty = DifficultyLevel.ELITE,
            estimatedMinutes = 30,
            targetMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE, MuscleGroup.SHOULDERS, MuscleGroup.HAMSTRINGS),
            items = listOf(
                WorkoutItem("push_up_archer", targetSets = 3, targetReps = 6, restSeconds = 90),
                WorkoutItem("squat_pistol", targetSets = 3, targetReps = 5, restSeconds = 90),
                WorkoutItem("dragon_flag", targetSets = 3, targetReps = 5, restSeconds = 90),
                WorkoutItem("handstand_hold", targetSets = 3, targetReps = 0, targetHoldSeconds = 30, restSeconds = 90),
                WorkoutItem("nordic_curl", targetSets = 3, targetReps = 5, restSeconds = 90)
            )
        )
    )

    fun getRoutines(): List<WorkoutRoutine> = curatedRoutines

    fun getRoutineById(id: String): WorkoutRoutine? = curatedRoutines.firstOrNull { it.id == id }

    fun getDefaultTodayRoutine(): WorkoutRoutine {
        return curatedRoutines.first { it.id == "routine_total_body_burn" }
    }

    fun getRoutineForDifficulty(level: DifficultyLevel): WorkoutRoutine {
        return curatedRoutines.firstOrNull { it.difficulty == level }
            ?: curatedRoutines.minByOrNull { Math.abs(it.difficulty.rank - level.rank) }
            ?: getDefaultTodayRoutine()
    }

    /**
     * Resolves the full Exercise objects for every item inside a routine.
     */
    fun getExercisesForRoutine(routine: WorkoutRoutine): List<Pair<WorkoutItem, Exercise>> {
        return routine.items.mapNotNull { item ->
            val ex = ExerciseCatalog.getById(item.exerciseId)
            if (ex != null) item to ex else null
        }
    }

    /**
     * Generates a balanced workout session targeting all primary muscle regions
     * (Push, Legs, Core, Posterior) without overlapping fatigue.
     */
    fun generateBalancedWorkout(
        difficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE,
        targetDurationMinutes: Int = 20
    ): WorkoutRoutine {
        val pool = ExerciseCatalog.getByDifficulty(difficulty).ifEmpty {
            ExerciseCatalog.getAll()
        }

        val pushEx = pool.firstOrNull { it.targetMuscle == MuscleGroup.CHEST || it.targetMuscle == MuscleGroup.SHOULDERS }
            ?: ExerciseCatalog.getById("push_up_standard")!!

        val legEx = pool.firstOrNull { it.targetMuscle == MuscleGroup.QUADS || it.targetMuscle == MuscleGroup.GLUTES }
            ?: ExerciseCatalog.getById("squat_bodyweight")!!

        val coreEx = pool.firstOrNull { it.targetMuscle == MuscleGroup.CORE }
            ?: ExerciseCatalog.getById("plank_standard")!!

        val postEx = pool.firstOrNull { it.targetMuscle == MuscleGroup.BACK || it.targetMuscle == MuscleGroup.HAMSTRINGS }
            ?: ExerciseCatalog.getById("good_mornings")!!

        val selected = listOf(pushEx, legEx, coreEx, postEx)
        val items = selected.map { ex ->
            WorkoutItem(
                exerciseId = ex.id,
                targetSets = ex.defaultSets,
                targetReps = ex.defaultReps,
                targetHoldSeconds = ex.defaultHoldSeconds,
                restSeconds = ex.restSeconds
            )
        }

        return WorkoutRoutine(
            id = "custom_balanced_${System.currentTimeMillis()}",
            name = "Balanced ${difficulty.displayName} Circuit",
            tagline = "Balanced ${selected.size}-exercise circuit targeting push, pull, legs & core",
            difficulty = difficulty,
            estimatedMinutes = targetDurationMinutes,
            targetMuscles = selected.map { it.targetMuscle }.distinct(),
            items = items
        )
    }
}
