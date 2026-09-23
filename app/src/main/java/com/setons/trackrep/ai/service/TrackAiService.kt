package com.setons.trackrep.ai.service

import android.content.Context
import com.setons.trackrep.ai.action.AdjustTargetAction
import com.setons.trackrep.ai.action.ExplainWorkoutAdjustmentAction
import com.setons.trackrep.ai.action.ModifyRoutineAction
import com.setons.trackrep.ai.action.ReplaceExerciseAction
import com.setons.trackrep.ai.action.RescheduleWorkoutAction
import com.setons.trackrep.ai.action.TrackAction
import com.setons.trackrep.ai.action.TrackActionExecutor
import com.setons.trackrep.ai.action.TrackActionValidator
import com.setons.trackrep.ai.action.TrackAiResponse
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.workout.WorkoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dedicated Track AI Service interfacing with Gemini and executing on-device
 * domain reasoning when offline or without an external API key.
 *
 * Enforces Blueprint Architecture:
 * - "Never allow Gemini to write directly to the database."
 * - "Gemini receives structured context, not raw camera video by default."
 * - "Track service -> Gemini -> validated structured commands -> domain actions."
 */
object TrackAiService {

    private const val PREFS_NAME = "trackrep_ai_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    fun getApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setApiKey(context: Context, key: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GEMINI_API_KEY, key?.trim()).apply()
    }

    suspend fun processMessage(
        context: Context,
        userMessage: String
    ): TrackAiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)

        // 1. If API key is available, attempt live Gemini API call
        if (!apiKey.isNullOrBlank()) {
            try {
                val structuredContext = TrackContextBuilder.buildStructuredContext(context)
                val response = callGeminiApi(apiKey, userMessage, structuredContext)
                if (response != null) {
                    executeValidatedActions(context, response.actions, userMessage)
                    return@withContext response
                }
            } catch (e: Exception) {
                // Seamlessly fall back to on-device reasoning if API call fails
            }
        }

        // 2. On-Device Fallback Reasoning Engine
        val localResponse = evaluateOnDeviceReasoning(userMessage)
        executeValidatedActions(context, localResponse.actions, userMessage)
        localResponse
    }

    private suspend fun executeValidatedActions(
        context: Context,
        actions: List<TrackAction>,
        prompt: String
    ) {
        for (action in actions) {
            val validation = TrackActionValidator.validate(action)
            if (validation.isValid) {
                TrackActionExecutor.execute(context, action, prompt)
            }
        }
    }

    /**
     * Calls Gemini 2.5 Flash API with strict JSON schema instructions.
     */
    private fun callGeminiApi(apiKey: String, userMessage: String, structuredContext: String): TrackAiResponse? {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 12000

        val systemPrompt = """
You are Track, an elite athletic AI coach developed by Setons for TrackRep.
You analyze athlete inquiries, biomechanics, and workout adaptations.

CRITICAL ARCHITECTURAL RULE:
You cannot directly alter user data. You must output a JSON object conforming to:
{
  "replyMessage": "Warm, concise, biomechanically sound coaching message",
  "actions": [
    {
      "type": "REPLACE_EXERCISE",
      "oldExerciseId": "push_up_standard",
      "newExerciseId": "push_up_incline",
      "reason": "Wrist relief"
    },
    {
      "type": "ADJUST_TARGET",
      "exerciseId": "push_up_standard",
      "newTargetReps": 12,
      "reason": "User requested higher volume"
    },
    {
      "type": "RESCHEDULE_WORKOUT",
      "daysOffset": 1,
      "reason": "Missed workout reschedule"
    },
    {
      "type": "EXPLAIN_ADJUSTMENT",
      "topic": "Form Cues",
      "explanation": "Detailed biomechanical guidance"
    }
  ]
}

CONTEXT OF CURRENT ATHLETE & WORKOUT:
$structuredContext
"""

        val fullPrompt = "$systemPrompt\n\nATHLETE QUERY: $userMessage"
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", fullPrompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.4)
            })
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val code = conn.responseCode
        if (code !in 200..299) return null

        val rawResponse = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val textContent = candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        return parseGeminiJsonResponse(textContent)
    }

    /**
     * Parses the JSON output produced by Gemini into strongly-typed TrackActions.
     */
    fun parseGeminiJsonResponse(jsonStr: String): TrackAiResponse {
        val obj = JSONObject(jsonStr)
        val reply = obj.optString("replyMessage", "I've reviewed your request.")
        val actions = mutableListOf<TrackAction>()

        val arr = obj.optJSONArray("actions")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                when (item.optString("type")) {
                    "REPLACE_EXERCISE" -> {
                        actions.add(
                            ReplaceExerciseAction(
                                oldExerciseId = item.getString("oldExerciseId"),
                                newExerciseId = item.getString("newExerciseId"),
                                reason = item.optString("reason", "Adapted by Track")
                            )
                        )
                    }
                    "ADJUST_TARGET" -> {
                        actions.add(
                            AdjustTargetAction(
                                exerciseId = item.getString("exerciseId"),
                                newTargetReps = if (item.has("newTargetReps")) item.getInt("newTargetReps") else null,
                                newTargetSets = if (item.has("newTargetSets")) item.getInt("newTargetSets") else null,
                                newTargetHoldSeconds = if (item.has("newTargetHoldSeconds")) item.getInt("newTargetHoldSeconds") else null,
                                newRestSeconds = if (item.has("newRestSeconds")) item.getInt("newRestSeconds") else null,
                                reason = item.optString("reason", "Adjusted by Track")
                            )
                        )
                    }
                    "RESCHEDULE_WORKOUT" -> {
                        actions.add(
                            RescheduleWorkoutAction(
                                daysOffset = item.optInt("daysOffset", 1),
                                reason = item.optString("reason", "Rescheduled by Track")
                            )
                        )
                    }
                    "EXPLAIN_ADJUSTMENT" -> {
                        actions.add(
                            ExplainWorkoutAdjustmentAction(
                                topic = item.optString("topic", "Coaching"),
                                explanation = item.optString("explanation", "")
                            )
                        )
                    }
                }
            }
        }

        return TrackAiResponse(replyMessage = reply, actions = actions)
    }

    /**
     * On-Device Fallback Reasoning Engine.
     * Evaluates natural language queries using biomechanical rules when offline.
     */
    fun evaluateOnDeviceReasoning(query: String): TrackAiResponse {
        val q = query.lowercase().trim()
        val actions = mutableListOf<TrackAction>()
        val reply: String

        when {
            // 1. Pain / Joint Discomfort: Wrist Relief
            q.contains("wrist") -> {
                val newExId = "push_up_incline"
                actions.add(
                    ReplaceExerciseAction(
                        oldExerciseId = "push_up_standard",
                        newExerciseId = newExId,
                        reason = "Wrist Joint Decompression"
                    )
                )
                reply = "Wrist discomfort during standard push-ups is typically caused by the extreme 90° dorsiflexion angle under full bodyweight. I have replaced standard push-ups with Incline Push-ups in your routine—this decreases wrist shear stress by over 40% while preserving chest and triceps hypertrophy."
            }

            // 2. Pain / Joint Discomfort: Shoulder Relief
            q.contains("shoulder") || q.contains("rotator") -> {
                val newExId = "glute_bridge"
                actions.add(
                    ReplaceExerciseAction(
                        oldExerciseId = "pike_push_up",
                        newExerciseId = newExId,
                        reason = "Anterior Deltoid & Rotator Cuff Deload"
                    )
                )
                reply = "To protect your shoulder capsule and avoid subacromial impingement, I have removed vertical overhead pressing and substituted Glute Bridges to direct today's training stimulus through the posterior chain."
            }

            // 3. Pain / Joint Discomfort: Knee Relief
            q.contains("knee") || q.contains("patella") -> {
                actions.add(
                    ReplaceExerciseAction(
                        oldExerciseId = "squat_bodyweight",
                        newExerciseId = "glute_bridge",
                        reason = "Patellofemoral Joint Relief"
                    )
                )
                reply = "I've swapped Bodyweight Squats for Glute Bridges. This eliminates patellofemoral shear forces on the knee while strengthening the glutes and hamstrings."
            }

            // 4. Missed Workout / Rescheduling
            q.contains("missed") || q.contains("reschedule") || q.contains("yesterday") || q.contains("skip") -> {
                actions.add(
                    RescheduleWorkoutAction(
                        daysOffset = 1,
                        reason = "Missed Session Safe Rescheduling"
                    )
                )
                reply = "No problem at all! In TrackRep, we never stack missed volume onto the next day, as doubling up compromises recovery and invites injury. I have shifted your planned session forward by 1 day so you can execute it fresh."
            }

            // 5. Target Increments (e.g. "increase push-up by 2", "more reps", "make it harder")
            q.contains("increase") || q.contains("more reps") || q.contains("harder") -> {
                val targetEx = if (q.contains("squat")) "squat_bodyweight" else "push_up_standard"
                val currentReps = WorkoutEngine.getActiveOrTodayRoutine().items.find { it.exerciseId == targetEx }?.targetReps ?: 10
                val newReps = currentReps + 2
                actions.add(
                    AdjustTargetAction(
                        exerciseId = targetEx,
                        newTargetReps = newReps,
                        reason = "Athlete Requested Overload (+2 reps)"
                    )
                )
                reply = "Challenge accepted! I've adjusted your target volume for ${if (targetEx.contains("squat")) "Squats" else "Push-ups"} to $newReps reps (+2 progressive overload). Maintain crisp lockout and full depth."
            }

            // 6. Target Decrement (e.g. "decrease reps", "too hard", "easier")
            q.contains("decrease") || q.contains("fewer reps") || q.contains("too hard") || q.contains("easier") -> {
                val targetEx = if (q.contains("squat")) "squat_bodyweight" else "push_up_standard"
                val currentReps = WorkoutEngine.getActiveOrTodayRoutine().items.find { it.exerciseId == targetEx }?.targetReps ?: 10
                val newReps = maxOf(4, currentReps - 2)
                actions.add(
                    AdjustTargetAction(
                        exerciseId = targetEx,
                        newTargetReps = newReps,
                        reason = "Athlete Requested Volume Back-off (-2 reps)"
                    )
                )
                reply = "Smart move. Quality of movement always trumps sheer quantity. I've scaled your target down to $newReps reps so you can focus on clean eccentric control."
            }

            // 7. Form Guidance: Hips Sagging / Plank
            q.contains("sag") || q.contains("hip") || q.contains("core") -> {
                actions.add(
                    ExplainWorkoutAdjustmentAction(
                        topic = "Pelvic Alignment & Anti-Extension",
                        explanation = "Keep glutes squeezed and brace your abdomen like you're taking a punch."
                    )
                )
                reply = "Hips sagging indicates loss of anterior core tension. To correct this, actively squeeze your glutes hard and tuck your pelvis slightly (posterior pelvic tilt). Imagine pulling your belt buckle toward your ribcage."
            }

            // 8. Form Guidance: Camera Positioning
            q.contains("camera") || q.contains("phone") || q.contains("position") -> {
                actions.add(
                    ExplainWorkoutAdjustmentAction(
                        topic = "Camera Setup Guidance",
                        explanation = "5–7 paces away, ~15° tilt at floor level."
                    )
                )
                reply = "For optimal ML Kit joint detection, place your phone on the floor approximately 5 to 7 normal paces away, tilted about 15° upward. Ensure the camera sees your full body from head to toes within the luxury gold brackets."
            }

            // 9. Default Coaching Response
            else -> {
                reply = "I'm Track, your adaptive coach! You can ask me to swap exercises if you have joint discomfort, reschedule missed workouts, adjust target reps, or explain any form cues."
            }
        }

        return TrackAiResponse(replyMessage = reply, actions = actions)
    }
}
