package com.setons.trackrep

import com.setons.trackrep.ai.action.AdjustTargetAction
import com.setons.trackrep.ai.action.ReplaceExerciseAction
import com.setons.trackrep.ai.action.RescheduleWorkoutAction
import com.setons.trackrep.ai.action.TrackActionExecutor
import com.setons.trackrep.ai.service.TrackAiService
import com.setons.trackrep.workout.WorkoutEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackAiServiceTest {

    @Before
    fun setUp() {
        WorkoutEngine.resetToDefault()
    }

    @Test
    fun testWristDiscomfortTriggersExerciseReplacement() {
        val query = "My wrists hurt during pushups. Can we swap them?"
        val response = TrackAiService.evaluateOnDeviceReasoning(query)

        assertTrue(response.actions.isNotEmpty())
        val action = response.actions.first()
        assertTrue(action is ReplaceExerciseAction)
        val replaceAction = action as ReplaceExerciseAction
        assertEquals("push_up_standard", replaceAction.oldExerciseId)
        assertEquals("push_up_incline", replaceAction.newExerciseId)
        assertTrue(response.replyMessage.contains("Incline Push-ups"))
    }

    @Test
    fun testMissedWorkoutTriggersRescheduling() {
        val query = "I missed yesterday's workout. Reschedule it please."
        val response = TrackAiService.evaluateOnDeviceReasoning(query)

        assertTrue(response.actions.isNotEmpty())
        val action = response.actions.first()
        assertTrue(action is RescheduleWorkoutAction)
        val rescheduleAction = action as RescheduleWorkoutAction
        assertEquals(1, rescheduleAction.daysOffset)
        assertTrue(response.replyMessage.contains("never stack missed volume"))
    }

    @Test
    fun testIncreaseRepsTriggersTargetAdjustment() {
        val query = "Increase push-up reps by 2, make it harder"
        val response = TrackAiService.evaluateOnDeviceReasoning(query)

        assertTrue(response.actions.isNotEmpty())
        val action = response.actions.first()
        assertTrue(action is AdjustTargetAction)
        val targetAction = action as AdjustTargetAction
        assertEquals("push_up_standard", targetAction.exerciseId)
        assertNotNull(targetAction.newTargetReps)
        assertTrue(targetAction.newTargetReps!! > 10)
    }

    @Test
    fun testNaturalLanguageRequestSafelyChangesAppRoutine() = runBlocking {
        // Acceptance test requirement: Natural-language requests safely change the app
        val beforeRoutine = WorkoutEngine.getActiveOrTodayRoutine()
        assertTrue(beforeRoutine.items.any { it.exerciseId == "push_up_standard" })

        // Natural language query to swap pushups
        val response = TrackAiService.evaluateOnDeviceReasoning("My wrists hurt, replace pushups with something easier")
        val action = response.actions.first()

        // Execute action through TrackActionExecutor
        val execResult = TrackActionExecutor.execute(context = null, action = action, requestPrompt = "My wrists hurt")
        assertTrue(execResult.isSuccess)

        // Verify domain routine was demonstrably updated
        val afterRoutine = WorkoutEngine.getActiveOrTodayRoutine()
        assertTrue(afterRoutine.items.any { it.exerciseId == "push_up_incline" })
        assertTrue(afterRoutine.items.none { it.exerciseId == "push_up_standard" })
        assertTrue(afterRoutine.name.contains("Track Adapted"))
    }

    @Test
    fun testParseGeminiJsonResponse() {
        val jsonPayload = """
        {
          "replyMessage": "I have adjusted your push-up target and rescheduled your session.",
          "actions": [
            {
              "type": "ADJUST_TARGET",
              "exerciseId": "push_up_standard",
              "newTargetReps": 14,
              "reason": "Volume increase"
            },
            {
              "type": "RESCHEDULE_WORKOUT",
              "daysOffset": 2,
              "reason": "Travel schedule"
            }
          ]
        }
        """.trimIndent()

        val parsed = TrackAiService.parseGeminiJsonResponse(jsonPayload)
        assertEquals("I have adjusted your push-up target and rescheduled your session.", parsed.replyMessage)
        assertEquals(2, parsed.actions.size)
        assertTrue(parsed.actions[0] is AdjustTargetAction)
        assertTrue(parsed.actions[1] is RescheduleWorkoutAction)
    }
}
