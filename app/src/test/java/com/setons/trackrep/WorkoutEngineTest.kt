package com.setons.trackrep

import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.workout.WorkoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEngineTest {

    @Test
    fun testCuratedRoutinesCompleteness() {
        val routines = WorkoutEngine.getRoutines()
        assertTrue("Must provide at least 4 curated routines", routines.size >= 4)

        routines.forEach { routine ->
            assertNotNull(routine.id)
            assertTrue(routine.name.isNotBlank())
            assertTrue(routine.tagline.isNotBlank())
            assertTrue(routine.estimatedMinutes > 0)
            assertTrue("Routine must contain at least 3 exercises", routine.items.size >= 3)
            assertTrue(routine.targetMuscles.isNotEmpty())

            val resolved = WorkoutEngine.getExercisesForRoutine(routine)
            assertEquals("All workout items must resolve to catalog exercises", routine.items.size, resolved.size)
        }
    }

    @Test
    fun testDefaultTodayRoutineValidity() {
        val defaultRoutine = WorkoutEngine.getDefaultTodayRoutine()
        assertNotNull(defaultRoutine)
        assertEquals("routine_total_body_burn", defaultRoutine.id)
        assertEquals(DifficultyLevel.INTERMEDIATE, defaultRoutine.difficulty)

        val exercises = WorkoutEngine.getExercisesForRoutine(defaultRoutine)
        assertTrue("Total-body burn should feature AI vision exercises", exercises.any { it.second.isVisionSupported })
    }

    @Test
    fun testDifficultyMapping() {
        DifficultyLevel.values().forEach { level ->
            val routine = WorkoutEngine.getRoutineForDifficulty(level)
            assertNotNull(routine)
            assertTrue(routine.items.isNotEmpty())
        }
    }

    @Test
    fun testBalancedWorkoutGenerator() {
        val generated = WorkoutEngine.generateBalancedWorkout(DifficultyLevel.ADVANCED, targetDurationMinutes = 25)
        assertNotNull(generated)
        assertEquals(DifficultyLevel.ADVANCED, generated.difficulty)
        assertEquals(25, generated.estimatedMinutes)
        assertEquals(4, generated.items.size)

        val resolved = WorkoutEngine.getExercisesForRoutine(generated)
        assertEquals(4, resolved.size)

        val targetMuscles = resolved.map { it.second.targetMuscle }
        assertTrue("Should include chest or shoulder pressing", targetMuscles.contains(MuscleGroup.CHEST) || targetMuscles.contains(MuscleGroup.SHOULDERS))
        assertTrue("Should include quad or glute leg movement", targetMuscles.contains(MuscleGroup.QUADS) || targetMuscles.contains(MuscleGroup.GLUTES))
        assertTrue("Should include core movement", targetMuscles.contains(MuscleGroup.CORE))
        assertTrue("Should include posterior/back/hamstring movement", targetMuscles.contains(MuscleGroup.BACK) || targetMuscles.contains(MuscleGroup.HAMSTRINGS))
    }
}
