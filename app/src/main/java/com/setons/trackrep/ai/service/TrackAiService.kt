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
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.workout.WorkoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Track AI Service interfacing with Gemini and executing on-device
 * domain reasoning when offline or without an external API key.
 *
 * Enforces Blueprint Architecture:
 * - "Never allow Gemini to write directly to the database."
 * - "Gemini receives structured context, not raw camera video by default."
 * - "Track service -> Gemini -> validated structured commands -> domain actions."
 * - As long as there is an API key configured, Gemini is queried directly and
 *   errors (quota, rate limits, invalid keys) are reported clearly rather than
 *   silently falling back to canned local responses.
 */
object TrackAiService {

    private const val PREFS_NAME = "trackrep_ai_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    sealed class GeminiCallResult {
        data class Success(val response: TrackAiResponse) : GeminiCallResult()
        data class Error(val code: Int?, val userFacingErrorMessage: String) : GeminiCallResult()
    }

    fun getApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setApiKey(context: Context, key: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trimmed = key?.trim()
        if (trimmed.isNullOrBlank()) {
            prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        } else {
            prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        }
    }

    suspend fun processMessage(
        context: Context,
        userMessage: String
    ): TrackAiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)

        // 1. If API key is configured, ALWAYS query Gemini directly (never fallback to local NLP)
        if (!apiKey.isNullOrBlank()) {
            val structuredContext = TrackContextBuilder.buildStructuredContext(context)
            val result = callGeminiApiWithDetailedResult(apiKey, userMessage, structuredContext)
            return@withContext when (result) {
                is GeminiCallResult.Success -> {
                    executeValidatedActions(context, result.response.actions, userMessage)
                    result.response
                }
                is GeminiCallResult.Error -> {
                    // Do not execute local NLP fallback when user has configured an API key.
                    // Directly report the exact error (quota, invalid key, timeout, etc.)
                    TrackAiResponse(
                        replyMessage = result.userFacingErrorMessage,
                        actions = emptyList()
                    )
                }
            }
        }

        // 2. On-Device Fallback Reasoning Engine (Only active when no Gemini API key is configured)
        val localResponse = evaluateOnDeviceReasoning(userMessage, context)
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
     * Calls Google Gemini API with fallback across flash model versions.
     * Returns detailed result with clear user-facing error explanations.
     */
    fun callGeminiApiWithDetailedResult(
        apiKey: String,
        userMessage: String,
        structuredContext: String
    ): GeminiCallResult {
        val candidateModels = listOf("gemini-2.5-flash", "gemini-2.5-flash-latest", "gemini-1.5-flash", "gemini-2.0-flash")
        var lastError: GeminiCallResult.Error? = null

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

When the athlete asks about their past workouts, workout history, performance, progress, or how they performed today, analyze their recent workout sessions provided in the structured context below and give an encouraging, biomechanically insightful breakdown of their reps, form consistency scores, fatigue trends, and cadence.

CONTEXT OF CURRENT ATHLETE & WORKOUT:
$structuredContext
""".trimIndent()

        val fullPrompt = "$systemPrompt\n\nATHLETE QUERY: $userMessage"

        for (model in candidateModels) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                conn.connectTimeout = 25000
                conn.readTimeout = 45000

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
                        if (model.contains("2.5") || model.contains("2.0")) {
                            put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                        }
                    })
                }
                val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

                conn.outputStream.use { os ->
                    os.write(payloadBytes)
                    os.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val rawResponse = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                    val root = JSONObject(rawResponse)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val textContent = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        val parsed = parseGeminiJsonResponse(textContent)
                        return GeminiCallResult.Success(parsed)
                    }
                }

                val errorStream = conn.errorStream ?: conn.inputStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else ""

                var apiMessage = ""
                var apiStatus = ""
                try {
                    val errRoot = JSONObject(errorBody).optJSONObject("error")
                    if (errRoot != null) {
                        apiMessage = errRoot.optString("message", "")
                        apiStatus = errRoot.optString("status", "")
                    }
                } catch (_: Exception) {}

                val userFacingError = when {
                    code == 429 || apiStatus == "RESOURCE_EXHAUSTED" || errorBody.contains("quota", ignoreCase = true) || errorBody.contains("RESOURCE_EXHAUSTED") -> {
                        "⚠️ Gemini API Quota Exceeded (HTTP 429): You have run out of usage or hit rate limits for this API key. Please check your Google AI Studio quota (aistudio.google.com) or wait a short while and try again."
                    }
                    code in listOf(400, 401, 403) || apiStatus in listOf("UNAUTHENTICATED", "PERMISSION_DENIED") || errorBody.contains("API_KEY_INVALID") -> {
                        "⚠️ Gemini API Key Error (HTTP $code): The configured API key is invalid or unauthorized. Tap the settings icon (⚙️) at the top right to verify or update your Gemini API key."
                    }
                    code == 404 -> {
                        lastError = GeminiCallResult.Error(code, "⚠️ Gemini Model Error (HTTP 404): Model '$model' was not found ($apiMessage).")
                        continue
                    }
                    code in 500..599 -> {
                        "⚠️ Gemini Server Error (HTTP $code): Google's Gemini service is temporarily unavailable. Please try again shortly."
                    }
                    else -> {
                        "⚠️ Gemini API Error (HTTP $code): ${if (apiMessage.isNotBlank()) apiMessage else "Unexpected error from Gemini API"}"
                    }
                }

                return GeminiCallResult.Error(code, userFacingError)
            } catch (e: java.net.UnknownHostException) {
                lastError = GeminiCallResult.Error(null, "⚠️ Network Connection Error: Could not reach Google Gemini. Please check your internet connection and try again.")
                continue
            } catch (e: java.net.SocketTimeoutException) {
                lastError = GeminiCallResult.Error(null, "⚠️ Request Timeout: Google Gemini took too long to respond. Please check your internet connection and try again.")
                continue
            } catch (e: Exception) {
                lastError = GeminiCallResult.Error(null, "⚠️ Gemini Connection Error: ${e.localizedMessage ?: "Failed to connect to Google Gemini"}. Please check your internet connection.")
                continue
            }
        }

        return lastError ?: GeminiCallResult.Error(null, "⚠️ Gemini Error: Could not communicate with Google Gemini API.")
    }

    /**
     * Backwards-compatible helper returning nullable TrackAiResponse.
     */
    fun callGeminiApi(apiKey: String, userMessage: String, structuredContext: String): TrackAiResponse? {
        val result = callGeminiApiWithDetailedResult(apiKey, userMessage, structuredContext)
        return (result as? GeminiCallResult.Success)?.response
    }

    /**
     * Parses the JSON output produced by Gemini into strongly-typed TrackActions.
     */
    fun parseGeminiJsonResponse(jsonStr: String): TrackAiResponse {
        return try {
            val cleanJson = jsonStr.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleanJson)
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

            TrackAiResponse(replyMessage = reply, actions = actions)
        } catch (_: Exception) {
            TrackAiResponse(
                replyMessage = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim(),
                actions = emptyList()
            )
        }
    }

    /**
     * On-Device Fallback Reasoning Engine.
     * Evaluates natural language queries using biomechanical rules when offline.
     */
    fun evaluateOnDeviceReasoning(query: String, context: Context? = null): TrackAiResponse {
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

            // 9. Today's Completed Exercises
            (q.contains("today") && (q.contains("complete") || q.contains("done") || q.contains("exercise") || q.contains("workout") || q.contains("did i") || q.contains("finish"))) ||
            q.contains("what did i do today") || q.contains("exercises today") || q.contains("completed today") -> {
                var todayReply: String? = null
                if (context != null) {
                    try {
                        val db = TrackRepDatabase.getDatabase(context)
                        val recent = runBlocking { db.sessionDao().getRecentSessions(25) }
                        val ymdFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val todayDateStr = ymdFormat.format(Date())
                        val todaySessions = recent.filter {
                            it.dateString == todayDateStr || ymdFormat.format(Date(it.timestampMs)) == todayDateStr
                        }
                        if (todaySessions.isNotEmpty()) {
                            val totalReps = todaySessions.sumOf { it.totalValidReps }
                            val avgScore = todaySessions.map { it.averageFormScore }.average().toInt()
                            val summaryLines = todaySessions.joinToString("\n") { s ->
                                "• ${s.exerciseName}: ${s.totalValidReps} valid reps (Form consistency: ${s.averageFormScore}%)"
                            }
                            todayReply = "Here are the exercises you completed today ($todayDateStr):\n\n$summaryLines\n\nTotal: $totalReps reps logged across ${todaySessions.size} set(s) with an average form score of $avgScore%.\nGreat job maintaining consistency today!"
                        } else {
                            todayReply = "You haven't completed any recorded exercise sets today yet ($todayDateStr).\nHead over to the Coach tab to begin today's routine!"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                reply = todayReply ?: "You haven't logged any exercise sets for today yet. Select an exercise in the Coach tab to start tracking your reps!"
            }

            // 10. History & Past Workouts Analysis
            q.contains("past") || q.contains("history") || q.contains("previous") || q.contains("analyze") || q.contains("analysis") || q.contains("last workout") || q.contains("recent") || q.contains("progress") || q.contains("how did i do") -> {
                var historyReply: String? = null
                if (context != null) {
                    try {
                        val db = TrackRepDatabase.getDatabase(context)
                        val recent = runBlocking { db.sessionDao().getRecentSessions(5) }
                        if (recent.isNotEmpty()) {
                            val totalReps = recent.sumOf { it.totalValidReps }
                            val avgScore = recent.map { it.averageFormScore }.average().toInt()
                            val summaryLines = recent.joinToString("\n") { s ->
                                "• ${s.dateString}: ${s.exerciseName} — ${s.totalValidReps} reps (Form score: ${s.averageFormScore}%)"
                            }
                            historyReply = "Here is the analysis of your recent logged workouts:\n\n$summaryLines\n\nOverall, you have completed $totalReps total reps with an average form consistency score of $avgScore%. Keep your tempo controlled and finish every repetition with full lockout!"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                reply = historyReply ?: "I'm ready to analyze your workout history! Once you complete and log sets with the camera coach, I'll provide detailed breakdowns of your rep counts, form consistency scores, and progressive overload."
            }

            // 11. Conversational Continuity (e.g. "continue", "next", "ok")
            q == "continue" || q.startsWith("continue") || q == "next" || q == "proceed" || q == "tell me more" || q == "go on" || q == "ok" || q == "okay" || q == "got it" -> {
                reply = "Ready when you are! Ask me about form cues (e.g. hip sagging, phone placement), swap exercises for joint relief (e.g. wrist or knee), adjust rep targets, or start a set in the Coach tab."
            }

            // 12. Unrecognized Query in On-Device Mode
            else -> {
                reply = "I'm operating in On-Device mode and didn't recognize \"$query\".\n\nIn this mode, you can ask:\n• \"What exercises did I complete today?\"\n• \"Analyze my past workouts\"\n• \"Swap push-ups for wrist relief\"\n• \"Increase push-up target by 2 reps\"\n• \"Fix hip sagging\" or \"Camera setup\"\n\n💡 Tip: For open-ended natural conversation and reasoning, add your Google Gemini API key in settings (⚙️ at top right)."
            }
        }

        return TrackAiResponse(replyMessage = reply, actions = actions)
    }
}
