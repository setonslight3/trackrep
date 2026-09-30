package com.setons.trackrep

import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.ui.demo.StickmanArchetype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StickmanDemoPlayerTest {

    @Test
    fun allCatalogExercisesHaveValidArchetypes() {
        val exercises = ExerciseCatalog.exercises
        assertEquals(35, exercises.size)

        exercises.forEach { ex ->
            val archetype = StickmanArchetype.fromExerciseId(ex.id)
            assertNotNull("Archetype must not be null for ${ex.id}", archetype)
        }
    }

    @Test
    fun squatsHaveDistinctSpecializedArchetypes() {
        val chairSquat = StickmanArchetype.fromExerciseId("squat_chair")
        val bodyweightSquat = StickmanArchetype.fromExerciseId("squat_bodyweight")
        val jumpSquat = StickmanArchetype.fromExerciseId("squat_jump")
        val bulgarianSquat = StickmanArchetype.fromExerciseId("squat_bulgarian")
        val pistolSquat = StickmanArchetype.fromExerciseId("squat_pistol")

        assertEquals(StickmanArchetype.SQUAT_CHAIR, chairSquat)
        assertEquals(StickmanArchetype.SQUAT_BODYWEIGHT, bodyweightSquat)
        assertEquals(StickmanArchetype.SQUAT_JUMP, jumpSquat)
        assertEquals(StickmanArchetype.SQUAT_BULGARIAN, bulgarianSquat)
        assertEquals(StickmanArchetype.SQUAT_PISTOL, pistolSquat)

        // Ensure all 5 squats are distinct from each other!
        val squatSet = setOf(chairSquat, bodyweightSquat, jumpSquat, bulgarianSquat, pistolSquat)
        assertEquals(5, squatSet.size)
    }

    @Test
    fun equipmentAndInvertedExercisesHaveDistinctArchetypes() {
        assertEquals(StickmanArchetype.PUSH_UP_WALL, StickmanArchetype.fromExerciseId("push_up_wall"))
        assertEquals(StickmanArchetype.PUSH_UP_INCLINE, StickmanArchetype.fromExerciseId("push_up_incline"))
        assertEquals(StickmanArchetype.PUSH_UP_DECLINE, StickmanArchetype.fromExerciseId("push_up_decline"))
        assertEquals(StickmanArchetype.BENCH_DIPS, StickmanArchetype.fromExerciseId("bench_dips"))
        assertEquals(StickmanArchetype.PULL_UP, StickmanArchetype.fromExerciseId("pull_up_standard"))
        assertEquals(StickmanArchetype.INVERTED_ROW, StickmanArchetype.fromExerciseId("inverted_row"))
        assertEquals(StickmanArchetype.HANDSTAND_HOLD, StickmanArchetype.fromExerciseId("handstand_hold"))
        assertEquals(StickmanArchetype.PLANK_SIDE, StickmanArchetype.fromExerciseId("plank_side"))
        assertEquals(StickmanArchetype.SINGLE_LEG_BRIDGE, StickmanArchetype.fromExerciseId("single_leg_bridge"))
    }

    @Test
    fun chairBoxSquatInstructionsAreAccurate() {
        val chairSquat = ExerciseCatalog.getById("squat_chair")
        assertNotNull(chairSquat)
        val instructions = chairSquat!!.instructions
        assert(instructions.any { it.contains("chair or box", ignoreCase = true) })
        assert(instructions.any { it.contains("lightly tap", ignoreCase = true) })
    }

    @Test
    fun bulgarianSplitSquatInstructionsAreAccurate() {
        val bulgarian = ExerciseCatalog.getById("squat_bulgarian")
        assertNotNull(bulgarian)
        val instructions = bulgarian!!.instructions
        assert(instructions.any { it.contains("bench, chair, or box", ignoreCase = true) })
        assert(instructions.any { it.contains("laces down", ignoreCase = true) })
    }

    @Test
    fun pistolSquatInstructionsAreAccurate() {
        val pistol = ExerciseCatalog.getById("squat_pistol")
        assertNotNull(pistol)
        val instructions = pistol!!.instructions
        assert(instructions.any { it.contains("non-working leg", ignoreCase = true) })
        assert(instructions.any { it.contains("counterweight", ignoreCase = true) })
    }
}
