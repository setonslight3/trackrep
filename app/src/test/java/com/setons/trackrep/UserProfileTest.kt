package com.setons.trackrep

import com.setons.trackrep.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun testDefaultUserProfile() {
        val profile = UserProfileEntity()

        assertEquals("default_user", profile.id)
        assertEquals("Full-Body Muscle Development", profile.goal)
        assertEquals("Beginner", profile.fitnessLevel)
        assertEquals("Bodyweight (No Equipment)", profile.equipment)
        assertEquals("MON,WED,FRI", profile.availableDaysCsv)
        assertEquals("MORNING", profile.preferredTimeOfDay)
        assertEquals(7, profile.reminderHour)
        assertEquals(30, profile.reminderMinute)
        assertEquals(20, profile.workoutDurationMinutes)
        assertTrue(profile.isCameraEnabled)
        assertTrue(profile.remindersEnabled)
        assertFalse("Onboarding should be incomplete by default", profile.isOnboardingCompleted)
    }

    @Test
    fun testUpdatedUserProfile() {
        val profile = UserProfileEntity(
            fitnessLevel = "Intermediate",
            equipment = "Some Equipment (Bar / Bands)",
            availableDaysCsv = "MON,TUE,THU,FRI",
            preferredTimeOfDay = "EVENING",
            reminderHour = 18,
            reminderMinute = 0,
            workoutDurationMinutes = 30,
            isOnboardingCompleted = true
        )

        assertEquals("Intermediate", profile.fitnessLevel)
        assertEquals("Some Equipment (Bar / Bands)", profile.equipment)
        assertEquals("MON,TUE,THU,FRI", profile.availableDaysCsv)
        assertEquals("EVENING", profile.preferredTimeOfDay)
        assertEquals(18, profile.reminderHour)
        assertEquals(0, profile.reminderMinute)
        assertEquals(30, profile.workoutDurationMinutes)
        assertTrue(profile.isOnboardingCompleted)
    }
}