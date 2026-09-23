package com.setons.trackrep

import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.schedule.MissedSessionStrategy
import com.setons.trackrep.schedule.WorkoutScheduleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WorkoutScheduleEngineTest {

    @Test
    fun testGenerateCoherentFirstWeek_Beginner3Days() {
        val profile = UserProfileEntity(
            fitnessLevel = "Beginner",
            availableDaysCsv = "MON,WED,FRI",
            preferredTimeOfDay = "MORNING"
        )

        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 21) // Monday Sept 21, 2026
        }

        val week = WorkoutScheduleEngine.generateCoherentFirstWeek(profile, calendar)

        assertEquals("Must generate 7 days in the week", 7, week.size)

        // Monday (Day 1)
        val monday = week[0]
        assertEquals("MONDAY", monday.dayOfWeek)
        assertFalse("Monday should be a training day", monday.isRestDay)
        assertTrue("Monday should be full body foundation", monday.routineId.contains("foundation") || monday.routineId.contains("routine"))

        // Tuesday (Day 2)
        val tuesday = week[1]
        assertEquals("TUESDAY", tuesday.dayOfWeek)
        assertTrue("Tuesday should be a rest day", tuesday.isRestDay)
        assertEquals("Rest & Active Recovery", tuesday.routineName)

        // Wednesday (Day 3)
        val wednesday = week[2]
        assertEquals("WEDNESDAY", wednesday.dayOfWeek)
        assertFalse("Wednesday should be a training day", wednesday.isRestDay)

        // Thursday (Day 4)
        val thursday = week[3]
        assertEquals("THURSDAY", thursday.dayOfWeek)
        assertTrue("Thursday should be a rest day", thursday.isRestDay)

        // Friday (Day 5)
        val friday = week[4]
        assertEquals("FRIDAY", friday.dayOfWeek)
        assertFalse("Friday should be a training day", friday.isRestDay)

        // Saturday & Sunday
        assertTrue("Saturday should be rest day", week[5].isRestDay)
        assertTrue("Sunday should be rest day", week[6].isRestDay)

        val trainingCount = week.count { !it.isRestDay }
        val restCount = week.count { it.isRestDay }
        assertEquals(3, trainingCount)
        assertEquals(4, restCount)
    }

    @Test
    fun testGenerateCoherentFirstWeek_Intermediate4Days() {
        val profile = UserProfileEntity(
            fitnessLevel = "Intermediate",
            availableDaysCsv = "MON,TUE,THU,FRI",
            preferredTimeOfDay = "EVENING"
        )

        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 21)
        }

        val week = WorkoutScheduleEngine.generateCoherentFirstWeek(profile, calendar)
        assertEquals(7, week.size)

        val trainingCount = week.count { !it.isRestDay }
        assertEquals("Must have exactly 4 training days", 4, trainingCount)

        // Verify intermediate routine selection
        val routines = week.filter { !it.isRestDay }.map { it.routineId }
        assertTrue("Should include total body burn or upper/lower routines",
            routines.any { it.contains("total_body_burn") || it.contains("upper_core") || it.contains("lower_posterior") }
        )
    }

    @Test
    fun testDetectMissedSessions() {
        val schedule = listOf(
            ScheduledWorkoutEntity(
                dayOfWeek = "MONDAY",
                dayIndex = 1,
                dateString = "2026-09-21",
                routineId = "routine_total_body_burn",
                routineName = "Total-Body Athletic Burn",
                targetMusclesCsv = "CHEST,QUADS",
                isRestDay = false,
                status = "SCHEDULED"
            ),
            ScheduledWorkoutEntity(
                dayOfWeek = "TUESDAY",
                dayIndex = 2,
                dateString = "2026-09-22",
                routineId = "routine_active_recovery",
                routineName = "Rest & Active Recovery",
                targetMusclesCsv = "MOBILITY",
                isRestDay = true,
                status = "SCHEDULED"
            ),
            ScheduledWorkoutEntity(
                dayOfWeek = "WEDNESDAY",
                dayIndex = 3,
                dateString = "2026-09-23",
                routineId = "routine_lower_posterior_power",
                routineName = "Legs & Posterior Power",
                targetMusclesCsv = "QUADS,HAMSTRINGS",
                isRestDay = false,
                status = "SCHEDULED"
            )
        )

        // Completed set is empty, current date is Wednesday Sept 23
        val completedDates = emptySet<String>()
        val missed = WorkoutScheduleEngine.detectMissedSessions(schedule, completedDates, "2026-09-23")

        assertEquals("Monday workout should be detected as missed", 1, missed.size)
        assertEquals("MONDAY", missed[0].dayOfWeek)
        assertEquals("2026-09-21", missed[0].dateString)
    }

    @Test
    fun testResolveMissedSession_CatchUpToday() {
        val schedule = listOf(
            ScheduledWorkoutEntity(
                id = "mon_1",
                dayOfWeek = "MONDAY",
                dayIndex = 1,
                dateString = "2026-09-21",
                routineId = "routine_upper_core_blast",
                routineName = "Upper Body Push & Core",
                targetMusclesCsv = "CHEST,SHOULDERS",
                isRestDay = false,
                status = "SCHEDULED"
            ),
            ScheduledWorkoutEntity(
                id = "tue_2",
                dayOfWeek = "TUESDAY",
                dayIndex = 2,
                dateString = "2026-09-22",
                routineId = "routine_active_recovery",
                routineName = "Rest & Active Recovery",
                targetMusclesCsv = "MOBILITY",
                isRestDay = true,
                status = "SCHEDULED"
            )
        )

        val missed = schedule[0]
        val updated = WorkoutScheduleEngine.resolveMissedSession(
            missed = missed,
            strategy = MissedSessionStrategy.CATCH_UP_TODAY,
            currentSchedule = schedule,
            todayDateString = "2026-09-22"
        )

        val tuesday = updated.first { it.dateString == "2026-09-22" }
        assertFalse("Tuesday should now be active training day", tuesday.isRestDay)
        assertEquals("routine_upper_core_blast", tuesday.routineId)
        assertTrue(tuesday.notes.contains("Catch-up"))

        val monday = updated.first { it.dateString == "2026-09-21" }
        assertEquals("RESCHEDULED", monday.status)
    }

    @Test
    fun testResolveMissedSession_MoveToNextRestDay() {
        val schedule = listOf(
            ScheduledWorkoutEntity(
                id = "mon_1",
                dayOfWeek = "MONDAY",
                dayIndex = 1,
                dateString = "2026-09-21",
                routineId = "routine_total_body_burn",
                routineName = "Total-Body Athletic Burn",
                targetMusclesCsv = "CHEST,QUADS",
                isRestDay = false,
                status = "SCHEDULED"
            ),
            ScheduledWorkoutEntity(
                id = "tue_2",
                dayOfWeek = "TUESDAY",
                dayIndex = 2,
                dateString = "2026-09-22",
                routineId = "routine_lower_posterior_power",
                routineName = "Legs & Posterior Power",
                targetMusclesCsv = "QUADS",
                isRestDay = false,
                status = "SCHEDULED"
            ),
            ScheduledWorkoutEntity(
                id = "wed_3",
                dayOfWeek = "WEDNESDAY",
                dayIndex = 3,
                dateString = "2026-09-23",
                routineId = "routine_active_recovery",
                routineName = "Rest & Active Recovery",
                targetMusclesCsv = "MOBILITY",
                isRestDay = true,
                status = "SCHEDULED"
            )
        )

        val missed = schedule[0] // Monday
        val updated = WorkoutScheduleEngine.resolveMissedSession(
            missed = missed,
            strategy = MissedSessionStrategy.MOVE_TO_NEXT_REST_DAY,
            currentSchedule = schedule,
            todayDateString = "2026-09-22"
        )

        // Tuesday is unchanged
        val tuesday = updated.first { it.dateString == "2026-09-22" }
        assertEquals("routine_lower_posterior_power", tuesday.routineId)

        // Wednesday (upcoming rest day) receives the missed workout
        val wednesday = updated.first { it.dateString == "2026-09-23" }
        assertFalse("Wednesday rest day converted to training without stacking", wednesday.isRestDay)
        assertEquals("routine_total_body_burn", wednesday.routineId)
        assertTrue(wednesday.notes.contains("Rescheduled to rest day"))
    }

    @Test
    fun testResolveMissedSession_SkipAndAdapt() {
        val schedule = listOf(
            ScheduledWorkoutEntity(
                id = "mon_1",
                dayOfWeek = "MONDAY",
                dayIndex = 1,
                dateString = "2026-09-21",
                routineId = "routine_total_body_burn",
                routineName = "Total-Body Athletic Burn",
                targetMusclesCsv = "CHEST",
                isRestDay = false,
                status = "SCHEDULED"
            )
        )

        val updated = WorkoutScheduleEngine.resolveMissedSession(
            missed = schedule[0],
            strategy = MissedSessionStrategy.SKIP_AND_ADAPT,
            currentSchedule = schedule,
            todayDateString = "2026-09-22"
        )

        val monday = updated[0]
        assertEquals("SKIPPED", monday.status)
        assertTrue(monday.notes.contains("Skipped"))
    }
}