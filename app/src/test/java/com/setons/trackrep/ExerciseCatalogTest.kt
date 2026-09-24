package com.setons.trackrep

import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun testCatalogHasSubstantialMovements() {
        val all = ExerciseCatalog.getAll()
        assertTrue("Catalog should contain at least 25 exercises", all.size >= 25)
    }

    @Test
    fun testAllDifficultyTiersAreCovered() {
        DifficultyLevel.values().forEach { level ->
            val exercisesForLevel = ExerciseCatalog.getByDifficulty(level)
            assertTrue("Difficulty level $level must have at least 2 exercises", exercisesForLevel.size >= 2)
        }
    }

    @Test
    fun testMajorMuscleGroupsAreCovered() {
        val testGroups = listOf(
            MuscleGroup.CHEST,
            MuscleGroup.QUADS,
            MuscleGroup.CORE,
            MuscleGroup.GLUTES,
            MuscleGroup.HAMSTRINGS,
            MuscleGroup.BACK,
            MuscleGroup.SHOULDERS,
            MuscleGroup.ARMS,
            MuscleGroup.CALVES
        )

        testGroups.forEach { muscle ->
            val exercises = ExerciseCatalog.getByMuscle(muscle)
            assertTrue("Muscle group $muscle must have at least one exercise", exercises.isNotEmpty())
        }
    }

    @Test
    fun testUniversalVisionSupportForAllExercises() {
        val all = ExerciseCatalog.getAll()
        val visionExercises = ExerciseCatalog.getVisionSupported()

        assertEquals("100% of catalog exercises must have AI Vision support", all.size, visionExercises.size)

        val modes = visionExercises.mapNotNull { it.framingMode }.toSet()
        assertTrue("Should support Push-up vision", modes.contains(ExerciseFramingMode.PUSH_UP))
        assertTrue("Should support Squat vision", modes.contains(ExerciseFramingMode.SQUAT))
        assertTrue("Should support Plank vision", modes.contains(ExerciseFramingMode.PLANK))
        assertTrue("Should support Pull-up vision", modes.contains(ExerciseFramingMode.PULL_UP))
        assertTrue("Should support Cardio vision", modes.contains(ExerciseFramingMode.CARDIO))
    }

    @Test
    fun testFuzzySearch() {
        // Name search
        val pushUps = ExerciseCatalog.search("push-up")
        assertTrue(pushUps.isNotEmpty())
        assertTrue(pushUps.any { it.id == "push_up_standard" })

        // Muscle search
        val quadExercises = ExerciseCatalog.search("quads")
        assertTrue(quadExercises.isNotEmpty())

        // Difficulty search
        val eliteExercises = ExerciseCatalog.search("elite")
        assertTrue(eliteExercises.isNotEmpty())
        assertTrue(eliteExercises.all { it.difficulty == DifficultyLevel.ELITE })

        // Blank query returns all
        assertEquals(ExerciseCatalog.getAll().size, ExerciseCatalog.search("").size)
    }

    @Test
    fun testExerciseIntegrity() {
        ExerciseCatalog.getAll().forEach { ex ->
            assertNotNull("Exercise ID should not be null", ex.id)
            assertTrue("Exercise name should not be blank", ex.name.isNotBlank())
            assertTrue("Exercise must have instructions", ex.instructions.isNotEmpty())
            assertTrue("Exercise must have sets > 0", ex.defaultSets > 0)
            assertTrue("Exercise must have reps > 0 or hold > 0", ex.defaultReps > 0 || ex.defaultHoldSeconds > 0)
        }
    }
}
