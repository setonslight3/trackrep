package com.setons.trackrep.coach

import com.setons.trackrep.camera.ExerciseFramingMode
import java.util.Locale

/**
 * Robust phonetic and natural language intent parser for TrackRep Voice Assistant.
 *
 * Solves:
 * 1. Accented speech variations & acoustic homophones ("push ops" -> push-ups, "squad" -> squats).
 * 2. Multi-hypothesis ranking across all candidate results returned by SpeechRecognizer.
 * 3. Compound action recognition (e.g. "start pushups" switches exercise AND starts workout).
 * 4. Punctuation, filler word, and prefix stripping ("hey track please start my workout").
 * 5. Standby wake-word sound-alike tolerance ("truck", "trek", "trax", "tract").
 */
object VoiceCommandParser {

    enum class ToggleAction { ENABLE, DISABLE, TOGGLE }

    sealed class VoiceIntent {
        data class StartWorkout(
            val targetExerciseId: String? = null,
            val mode: ExerciseFramingMode? = null,
            val exerciseName: String? = null
        ) : VoiceIntent()

        object StopWorkout : VoiceIntent()

        data class SwitchExercise(
            val exerciseId: String,
            val mode: ExerciseFramingMode,
            val exerciseName: String
        ) : VoiceIntent()

        data class ToggleTorch(val explicitEnable: Boolean? = null) : VoiceIntent()

        data class ToggleGhostShadow(val explicitEnable: Boolean? = null) : VoiceIntent()

        data class ToggleHandsFree(val explicitEnable: Boolean? = null) : VoiceIntent()

        data class SetTempo(val preset: String, val description: String) : VoiceIntent()

        data class Navigate(val destination: String, val spokenLabel: String) : VoiceIntent()

        data class PlayMusic(val query: String?, val isPlaylist: Boolean = false) : VoiceIntent()

        object PauseMusic : VoiceIntent()

        object ResumeMusic : VoiceIntent()

        object SkipMusic : VoiceIntent()

        object StopMusic : VoiceIntent()

        data class AdjustVolume(val isUp: Boolean) : VoiceIntent()

        data class SetDefaultMusic(val query: String, val isPlaylist: Boolean = false) : VoiceIntent()

        data class AskAi(val cleanQuery: String) : VoiceIntent()
    }

    data class ParsedCommand(
        val intent: VoiceIntent,
        val originalText: String,
        val confidenceScore: Float,
        val feedbackSpeech: String
    )

    private val WAKE_WORD_SOUND_ALIKES = listOf(
        "track", "truck", "trek", "trax", "tract", "trac", "thrack", "crack",
        "trackrep", "track rep", "hey track", "ok track", "hi track", "hello track", "yo track"
    )

    private val FILLER_PREFIXES = listOf(
        "hey track", "ok track", "hi track", "hello track", "yo track", "track",
        "please", "can you", "could you", "i want to", "let's", "lets", "go ahead and",
        "just", "now", "tell me", "how about"
    )

    /**
     * Check if a speech string triggers the standby wake word.
     */
    fun matchesWakeWord(text: String, configuredWakeWord: String = "track"): Boolean {
        val norm = normalize(text)
        val conf = configuredWakeWord.trim().lowercase(Locale.ROOT)

        if (norm.contains(conf)) return true
        for (soundAlike in WAKE_WORD_SOUND_ALIKES) {
            if (norm.contains(soundAlike)) return true
        }
        return false
    }

    /**
     * If user spoke "Track start pushups" in a single breath, extract "start pushups".
     */
    fun extractCommandAfterWakeWord(text: String, configuredWakeWord: String = "track"): String {
        var clean = normalize(text)
        val conf = configuredWakeWord.trim().lowercase(Locale.ROOT)

        // Strip configured wake word
        if (clean.startsWith(conf)) {
            clean = clean.removePrefix(conf).trim()
        }

        // Strip common wake words and prefixes
        for (prefix in FILLER_PREFIXES) {
            if (clean.startsWith(prefix)) {
                clean = clean.removePrefix(prefix).trim()
            }
        }

        // Strip leading filler words like "to", "and", "a"
        if (clean.startsWith("to ")) clean = clean.removePrefix("to ").trim()
        if (clean.startsWith("and ")) clean = clean.removePrefix("and ").trim()

        return clean
    }

    /**
     * Parse across all speech candidates and return the best parsed command.
     */
    fun parseCandidates(
        candidates: List<String>?,
        fallbackPartial: String? = null
    ): ParsedCommand {
        val candidateList = mutableListOf<String>()
        if (!candidates.isNullOrEmpty()) {
            candidateList.addAll(candidates)
        }
        if (!fallbackPartial.isNullOrBlank() && !candidateList.contains(fallbackPartial)) {
            candidateList.add(fallbackPartial)
        }

        if (candidateList.isEmpty()) {
            return ParsedCommand(
                intent = VoiceIntent.AskAi("help"),
                originalText = "",
                confidenceScore = 0f,
                feedbackSpeech = "Didn't catch that. Say \"Track\" anytime."
            )
        }

        var bestParsed: ParsedCommand? = null

        for (cand in candidateList) {
            val parsed = parseSingle(cand)
            if (bestParsed == null || parsed.confidenceScore > bestParsed.confidenceScore) {
                bestParsed = parsed
                // If perfect direct intent match, take it immediately
                if (parsed.confidenceScore >= 0.95f) {
                    return parsed
                }
            }
        }

        return bestParsed ?: parseSingle(candidateList.first())
    }

    /**
     * Parse a single speech string into a high-confidence intent.
     */
    fun parseSingle(rawText: String): ParsedCommand {
        val clean = extractCommandAfterWakeWord(rawText)
        if (clean.isBlank()) {
            return ParsedCommand(
                intent = VoiceIntent.AskAi("help"),
                originalText = rawText,
                confidenceScore = 0.1f,
                feedbackSpeech = "I'm listening! Tell me to start a workout, switch exercises, or toggle the flashlight."
            )
        }

        // Check for Exercise Mentions
        val exerciseMatch = resolveExercise(clean)

        // Check for Workout Lifecycle Actions (Start / Stop)
        val isStartAction = matchesStartWorkout(clean)
        val isStopAction = matchesStopWorkout(clean)

        // 1. Compound: Start + Exercise (e.g. "Start push-ups", "Let's do squats")
        if (isStartAction && exerciseMatch != null) {
            return ParsedCommand(
                intent = VoiceIntent.StartWorkout(
                    targetExerciseId = exerciseMatch.id,
                    mode = exerciseMatch.framingMode,
                    exerciseName = exerciseMatch.displayName
                ),
                originalText = rawText,
                confidenceScore = 0.98f,
                feedbackSpeech = "Starting ${exerciseMatch.displayName}! Calibrate your position and 3, 2, 1, Go!"
            )
        }

        // 2. Pure Stop Workout
        if (isStopAction) {
            return ParsedCommand(
                intent = VoiceIntent.StopWorkout,
                originalText = rawText,
                confidenceScore = 0.96f,
                feedbackSpeech = "Stopping set. Great effort!"
            )
        }

        // 3. Pure Start Workout (General)
        if (isStartAction) {
            return ParsedCommand(
                intent = VoiceIntent.StartWorkout(),
                originalText = rawText,
                confidenceScore = 0.94f,
                feedbackSpeech = "Starting workout now! Calibrate your position and 3, 2, 1, Go!"
            )
        }

        // 4. Pure Switch Exercise (e.g. "Push-ups", "Switch to squats", "Plank hold")
        if (exerciseMatch != null) {
            return ParsedCommand(
                intent = VoiceIntent.SwitchExercise(
                    exerciseId = exerciseMatch.id,
                    mode = exerciseMatch.framingMode,
                    exerciseName = exerciseMatch.displayName
                ),
                originalText = rawText,
                confidenceScore = 0.95f,
                feedbackSpeech = "Switched to ${exerciseMatch.displayName}."
            )
        }

        // 5. Flashlight / Torch
        val torchAction = resolveTorch(clean)
        if (torchAction != null) {
            val explicitEnable = when (torchAction) {
                ToggleAction.ENABLE -> true
                ToggleAction.DISABLE -> false
                ToggleAction.TOGGLE -> null
            }
            val speech = when (torchAction) {
                ToggleAction.ENABLE -> "Turning on camera flashlight."
                ToggleAction.DISABLE -> "Turning off camera flashlight."
                ToggleAction.TOGGLE -> "Toggling camera flashlight."
            }
            return ParsedCommand(
                intent = VoiceIntent.ToggleTorch(explicitEnable),
                originalText = rawText,
                confidenceScore = 0.92f,
                feedbackSpeech = speech
            )
        }

        // 6. Ghost Shadow Form Overlay
        val ghostAction = resolveGhostShadow(clean)
        if (ghostAction != null) {
            val explicitEnable = when (ghostAction) {
                ToggleAction.ENABLE -> true
                ToggleAction.DISABLE -> false
                ToggleAction.TOGGLE -> null
            }
            val speech = when (ghostAction) {
                ToggleAction.ENABLE -> "Ghost shadow AR form guide enabled."
                ToggleAction.DISABLE -> "Ghost shadow turned off."
                ToggleAction.TOGGLE -> "Toggling ghost shadow form overlay."
            }
            return ParsedCommand(
                intent = VoiceIntent.ToggleGhostShadow(explicitEnable),
                originalText = rawText,
                confidenceScore = 0.92f,
                feedbackSpeech = speech
            )
        }

        // 7. Hands-Free Auto Start
        val handsFreeAction = resolveHandsFree(clean)
        if (handsFreeAction != null) {
            val explicitEnable = when (handsFreeAction) {
                ToggleAction.ENABLE -> true
                ToggleAction.DISABLE -> false
                ToggleAction.TOGGLE -> null
            }
            val speech = when (handsFreeAction) {
                ToggleAction.ENABLE -> "Hands-free countdown auto-start enabled."
                ToggleAction.DISABLE -> "Hands-free auto-start disabled."
                ToggleAction.TOGGLE -> "Toggling hands-free countdown."
            }
            return ParsedCommand(
                intent = VoiceIntent.ToggleHandsFree(explicitEnable),
                originalText = rawText,
                confidenceScore = 0.91f,
                feedbackSpeech = speech
            )
        }

        // 8. Tempo Metronome Presets
        val tempoMatch = resolveTempo(clean)
        if (tempoMatch != null) {
            return ParsedCommand(
                intent = VoiceIntent.SetTempo(tempoMatch.first, tempoMatch.second),
                originalText = rawText,
                confidenceScore = 0.93f,
                feedbackSpeech = tempoMatch.second
            )
        }

        // 9. App Navigation
        val navMatch = resolveNavigation(clean)
        if (navMatch != null) {
            return ParsedCommand(
                intent = VoiceIntent.Navigate(navMatch.first, navMatch.second),
                originalText = rawText,
                confidenceScore = 0.91f,
                feedbackSpeech = navMatch.second
            )
        }

        // 10. Music Volume Controls
        val volumeMatch = resolveMusicVolume(clean)
        if (volumeMatch != null) {
            val speech = if (volumeMatch) "Volume increased." else "Volume decreased."
            return ParsedCommand(
                intent = VoiceIntent.AdjustVolume(volumeMatch),
                originalText = rawText,
                confidenceScore = 0.94f,
                feedbackSpeech = speech
            )
        }

        // 11. Music Pause / Stop Controls
        val musicPauseOrStop = resolveMusicPauseOrStop(clean)
        if (musicPauseOrStop != null) {
            return ParsedCommand(
                intent = musicPauseOrStop.first,
                originalText = rawText,
                confidenceScore = 0.95f,
                feedbackSpeech = musicPauseOrStop.second
            )
        }

        // 12. Music Skip Track
        if (matchesMusicSkip(clean)) {
            return ParsedCommand(
                intent = VoiceIntent.SkipMusic,
                originalText = rawText,
                confidenceScore = 0.95f,
                feedbackSpeech = "Skipping to next song."
            )
        }

        // 13. Music Resume / Unpause
        if (matchesMusicResume(clean)) {
            return ParsedCommand(
                intent = VoiceIntent.ResumeMusic,
                originalText = rawText,
                confidenceScore = 0.94f,
                feedbackSpeech = "Resuming music."
            )
        }

        // 14. Set Default Music ("set default music to [song]", "change default music to [song]")
        val defaultMusicMatch = resolveSetDefaultMusic(clean)
        if (defaultMusicMatch != null) {
            return ParsedCommand(
                intent = defaultMusicMatch.first,
                originalText = rawText,
                confidenceScore = 0.95f,
                feedbackSpeech = defaultMusicMatch.second
            )
        }

        // 15. YouTube Music Playback (Specific song, playlist, or generic workout music)
        val playMusicMatch = resolvePlayMusic(clean)
        if (playMusicMatch != null) {
            return ParsedCommand(
                intent = playMusicMatch.first,
                originalText = rawText,
                confidenceScore = 0.95f,
                feedbackSpeech = playMusicMatch.second
            )
        }

        // 15. General AI Query
        return ParsedCommand(
            intent = VoiceIntent.AskAi(clean),
            originalText = rawText,
            confidenceScore = 0.75f,
            feedbackSpeech = "" // Will be generated by TrackAiService
        )
    }

    private data class ExerciseInfo(
        val id: String,
        val framingMode: ExerciseFramingMode,
        val displayName: String
    )

    private fun resolveExercise(text: String): ExerciseInfo? {
        val t = " $text "

        // Diamond push up
        if (t.containsAny("diamond push up", "diamond pushup", "diamond pushups", "diamond press", "diamond push-up")) {
            return ExerciseInfo("push_up_diamond", ExerciseFramingMode.PUSH_UP, "Diamond Push-ups")
        }
        // Incline push up
        if (t.containsAny("incline push up", "incline pushup", "incline pushups", "incline push-up")) {
            return ExerciseInfo("push_up_incline", ExerciseFramingMode.PUSH_UP, "Incline Push-ups")
        }
        // Decline push up
        if (t.containsAny("decline push up", "decline pushup", "decline pushups", "decline push-up")) {
            return ExerciseInfo("push_up_decline", ExerciseFramingMode.PUSH_UP, "Decline Push-ups")
        }
        // Wall push up
        if (t.containsAny("wall push up", "wall pushup", "wall pushups", "wall push-up")) {
            return ExerciseInfo("push_up_wall", ExerciseFramingMode.PUSH_UP, "Wall Push-ups")
        }
        // Archer push up
        if (t.containsAny("archer push up", "archer pushup", "archer pushups", "archer push-up", "archer")) {
            return ExerciseInfo("push_up_archer", ExerciseFramingMode.PUSH_UP, "Archer Push-ups")
        }
        // Pike push up
        if (t.containsAny("pike push up", "pike pushup", "pike pushups", "pike push-up", "pike")) {
            return ExerciseInfo("pike_push_up", ExerciseFramingMode.PUSH_UP, "Pike Push-ups")
        }
        // General / Standard Push-up (and phonetic mishearings like "push ops", "bush up", "press up")
        if (t.containsAny(
                "push up", "pushup", "pushups", "push-up", "push-ups", "push ops", "pushops",
                "bush up", "bush ups", "bush ops", "press up", "press ups", "press-up", "press-ups",
                "chest push", "pushing up"
            )
        ) {
            return ExerciseInfo("push_up_standard", ExerciseFramingMode.PUSH_UP, "Standard Push-ups")
        }

        // Jump squat
        if (t.containsAny("jump squat", "jump squats", "jumping squat", "jumping squats")) {
            return ExerciseInfo("squat_jump", ExerciseFramingMode.SQUAT, "Jump Squats")
        }
        // Bulgarian split squat
        if (t.containsAny("bulgarian split squat", "bulgarian squat", "bulgarian squats", "bulgarian")) {
            return ExerciseInfo("squat_bulgarian", ExerciseFramingMode.SQUAT, "Bulgarian Split Squats")
        }
        // Pistol squat
        if (t.containsAny("pistol squat", "pistol squats", "pistol", "single leg squat")) {
            return ExerciseInfo("squat_pistol", ExerciseFramingMode.SQUAT, "Pistol Squats")
        }
        // Chair squat
        if (t.containsAny("chair squat", "chair squats", "box squat", "box squats")) {
            return ExerciseInfo("squat_chair", ExerciseFramingMode.SQUAT, "Chair Squats")
        }
        // Standard / Bodyweight Squats (and phonetic variants like "squad", "squating")
        if (t.containsAny("squat", "squats", "squating", "squatting", "squad", "squads", "deep squat", "deep squats")) {
            return ExerciseInfo("squat_bodyweight", ExerciseFramingMode.SQUAT, "Bodyweight Squats")
        }

        // Side plank
        if (t.containsAny("side plank", "side planks")) {
            return ExerciseInfo("plank_side", ExerciseFramingMode.PLANK, "Side Plank")
        }
        // Knee plank
        if (t.containsAny("knee plank", "knee planks")) {
            return ExerciseInfo("plank_knee", ExerciseFramingMode.PLANK, "Knee Plank")
        }
        // Standard Plank (and phonetic variants like "plant", "planks", "core hold")
        if (t.containsAny("plank", "planks", "planking", "plant", "plants", "core plank", "elbow plank", "front plank", "plank hold")) {
            return ExerciseInfo("plank_standard", ExerciseFramingMode.PLANK, "Standard Plank")
        }

        // Pull-up / Chin-up
        if (t.containsAny(
                "pull up", "pullup", "pullups", "pull-up", "pull-ups", "pull ups",
                "chin up", "chinup", "chinups", "chin-up", "chin-ups", "chin ups", "pull ops"
            )
        ) {
            return ExerciseInfo("pull_up_standard", ExerciseFramingMode.PULL_UP, "Standard Pull-ups")
        }

        // Dips / Bench Dips
        if (t.containsAny("dip", "dips", "dipping", "bench dip", "bench dips", "tricep dip", "tricep dips", "triceps dips", "deeps")) {
            return ExerciseInfo("bench_dips", ExerciseFramingMode.PUSH_UP, "Bench Dips")
        }

        // Burpees
        if (t.containsAny("burpee", "burpees", "burpy", "burpies")) {
            return ExerciseInfo("burpee_standard", ExerciseFramingMode.PUSH_UP, "Burpees")
        }

        // Mountain Climbers
        if (t.containsAny("mountain climber", "mountain climbers", "mountain climb", "mountain climbing")) {
            return ExerciseInfo("mountain_climber", ExerciseFramingMode.PLANK, "Mountain Climbers")
        }

        // Lunges
        if (t.containsAny("lunge", "lunges", "reverse lunge", "reverse lunges")) {
            return ExerciseInfo("lunge_reverse", ExerciseFramingMode.SQUAT, "Reverse Lunges")
        }

        // Glute Bridge
        if (t.containsAny("glute bridge", "glute bridges", "hip thrust", "hip thrusts")) {
            return ExerciseInfo("glute_bridge", ExerciseFramingMode.PLANK, "Glute Bridges")
        }

        // V-ups
        if (t.containsAny("v up", "v ups", "v-up", "v-ups", "vee up", "vee ups")) {
            return ExerciseInfo("v_ups", ExerciseFramingMode.PLANK, "V-Ups")
        }

        // Hollow Body Hold
        if (t.containsAny("hollow body", "hollow body hold", "hollow hold")) {
            return ExerciseInfo("hollow_body_hold", ExerciseFramingMode.PLANK, "Hollow Body Hold")
        }

        return null
    }

    private fun matchesStartWorkout(text: String): Boolean {
        val t = " $text "
        return t.containsAny(
            "start", "begin", "go", "launch", "record", "let's go", "lets go", "commence",
            "start workout", "start training", "start set", "begin set", "begin workout",
            "start recording", "count reps", "track reps", "count my reps", "track my reps"
        )
    }

    private fun matchesStopWorkout(text: String): Boolean {
        val t = " $text "
        return t.containsAny(
            "stop", "end", "finish", "done", "pause", "terminate", "complete", "halt",
            "stop workout", "stop set", "end set", "finish set", "stop recording",
            "i'm done", "im done", "pause set", "cancel set"
        )
    }

    private fun resolveTorch(text: String): ToggleAction? {
        val t = " $text "
        val hasTorchWord = t.containsAny("torch", "flashlight", "flash", "light", "illumination", "camera light", "camera flash")
        if (!hasTorchWord) return null

        if (t.containsAny("turn on", "switch on", "enable", "activate", "on")) return ToggleAction.ENABLE
        if (t.containsAny("turn off", "switch off", "disable", "shut off", "kill", "off")) return ToggleAction.DISABLE
        return ToggleAction.TOGGLE
    }

    private fun resolveGhostShadow(text: String): ToggleAction? {
        val t = " $text "
        val hasGhostWord = t.containsAny("ghost", "shadow", "hologram", "ar form", "silhouette", "form guide", "ghost shadow")
        if (!hasGhostWord) return null

        if (t.containsAny("enable", "turn on", "show", "activate", "on")) return ToggleAction.ENABLE
        if (t.containsAny("disable", "turn off", "hide", "off")) return ToggleAction.DISABLE
        return ToggleAction.TOGGLE
    }

    private fun resolveHandsFree(text: String): ToggleAction? {
        val t = " $text "
        val hasHandsFreeWord = t.containsAny("hands free", "hands-free", "auto start", "countdown start")
        if (!hasHandsFreeWord) return null

        if (t.containsAny("enable", "turn on", "activate", "on")) return ToggleAction.ENABLE
        if (t.containsAny("disable", "turn off", "off")) return ToggleAction.DISABLE
        return ToggleAction.TOGGLE
    }

    private fun resolveTempo(text: String): Pair<String, String>? {
        val t = " $text "
        val hasTempoWord = t.containsAny("tempo", "cadence", "metronome", "time under tension", "tut")
        if (!hasTempoWord) return null

        return when {
            t.containsAny("3 1 1", "3-1-1", "311", "three one one", "hypertrophy") ->
                Pair("3-1-1", "Tempo set to 3-1-1 Hypertrophy TUT.")
            t.containsAny("2 1 2", "2-1-2", "212", "two one two", "beginner", "control") ->
                Pair("2-1-2", "Tempo set to 2-1-2 Beginner Control.")
            t.containsAny("2 0 1", "2-0-1", "201", "two zero one", "power", "explosive", "fast") ->
                Pair("2-0-1", "Tempo set to 2-0-1 Athletic Power.")
            t.containsAny("4 2 1", "4-2-1", "421", "four two one", "slow", "slow burn") ->
                Pair("4-2-1", "Tempo set to 4-2-1 Slow Burn.")
            t.containsAny("off", "disable", "turn off", "stop", "zero", "none") ->
                Pair("OFF", "Tempo metronome turned off.")
            else ->
                Pair("2-1-2", "Tempo metronome set to 2-1-2.")
        }
    }

    private fun resolveNavigation(text: String): Pair<String, String>? {
        val t = " $text "

        // Routine customizer
        if (t.containsAny("edit routine", "custom routine", "swap routine", "change routine", "routine editor", "customize routine", "modify routine")) {
            return Pair("EDIT_ROUTINE", "Opening the routine customizer.")
        }

        // History / records
        if (t.containsAny("history", "records", "progress", "past workouts", "my reps", "best reps", "pr", "stats")) {
            return Pair("HISTORY", "Opening your workout history and records.")
        }

        // Exercise library
        if (t.containsAny("library", "exercises", "catalog", "exercise list", "all exercises", "workout library")) {
            return Pair("EXERCISE_LIBRARY", "Opening the exercise library.")
        }

        // Coach / camera
        if (t.containsAny("coach", "camera", "ai vision", "open coach", "open camera", "vision coach")) {
            return Pair("COACH", "Opening AI Vision Coach.")
        }

        // Profile / settings
        if (t.containsAny("settings", "profile", "theme", "themes", "color theme", "dark mode", "light mode", "change theme", "preferences")) {
            return Pair("PROFILE", "Opening profile and settings.")
        }

        // Home
        if (t.containsAny("home", "today", "today's workout", "weekly schedule", "schedule")) {
            return Pair("HOME", "Navigating to Today's screen.")
        }

        return null
    }

    private fun resolveMusicVolume(text: String): Boolean? {
        val t = " $text "
        val hasVolumeWord = t.containsAny("volume", "louder", "quieter")
        if (!hasVolumeWord) return null

        if (t.containsAny("turn the volume up", "turn volume up", "volume up", "increase volume", "louder", "turn up the music", "turn up volume", "raise volume", "boost volume")) {
            return true
        }
        if (t.containsAny("turn the volume down", "turn volume down", "volume down", "decrease volume", "quieter", "lower the volume", "lower volume", "turn down the music", "reduce volume")) {
            return false
        }
        return null
    }

    private fun resolveMusicPauseOrStop(text: String): Pair<VoiceIntent, String>? {
        val t = " $text "
        if (t.containsAny("pause the music", "pause music", "pause song", "pause audio", "pause track")) {
            return Pair(VoiceIntent.PauseMusic, "Music paused.")
        }
        if (t.containsAny("stop the music", "stop music", "stop playing music", "turn off the music", "turn off music", "kill the music", "kill music")) {
            return Pair(VoiceIntent.StopMusic, "Music stopped.")
        }
        return null
    }

    private fun matchesMusicSkip(text: String): Boolean {
        val t = " $text "
        return t.containsAny(
            "skip this song", "skip the song", "skip song", "next song", "next track",
            "skip this track", "skip track", "skip to next song", "next music"
        )
    }

    private fun matchesMusicResume(text: String): Boolean {
        val t = " $text "
        return t.containsAny(
            "resume the music", "resume music", "resume playback", "unpause music",
            "unpause the music", "unpause song", "continue music", "continue the music"
        )
    }

    private fun resolveSetDefaultMusic(text: String): Pair<VoiceIntent, String>? {
        val prefixes = listOf(
            "set default music to ",
            "change default music to ",
            "make default music ",
            "set my default music to ",
            "default music to ",
            "set default song to ",
            "change default song to "
        )
        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                val query = text.removePrefix(prefix).trim()
                if (query.isNotBlank()) {
                    val isPlaylist = query.contains("playlist") || query.contains("station")
                    return Pair(
                        VoiceIntent.SetDefaultMusic(query, isPlaylist),
                        "Default music set to '$query' for YouTube Music."
                    )
                }
            }
        }
        return null
    }

    private fun resolvePlayMusic(text: String): Pair<VoiceIntent, String>? {
        val t = " $text "

        // Check playlist
        if (t.containsAny("play my workout playlist", "play workout playlist", "play my playlist", "play playlist")) {
            var playlistName = "workout playlist"
            if (text.contains("playlist")) {
                val candidate = text.substringAfter("playlist").trim()
                if (candidate.isNotBlank() && candidate.length > 2) {
                    playlistName = "$candidate playlist"
                }
            }
            return Pair(VoiceIntent.PlayMusic(playlistName, isPlaylist = true), "Loading $playlistName on YouTube Music.")
        }

        // Generic "play some music" / "play music"
        if (t.containsAny("play some music", "play music", "play some songs", "start some music", "start the music", "put on some music", "put on music", "play a song")) {
            return Pair(VoiceIntent.PlayMusic(null, isPlaylist = false), "Starting music on YouTube Music.")
        }

        // Specific song: "play [song] on youtube music", "play [song] on yt music", "play [song]"
        if (text.startsWith("play ") || text.startsWith("listen to ")) {
            var song = if (text.startsWith("play ")) text.removePrefix("play ").trim() else text.removePrefix("listen to ").trim()
            val suffixes = listOf("on youtube music", "on yt music", "on youtube", "in youtube music", "in yt music", "from youtube music")
            for (suffix in suffixes) {
                if (song.endsWith(suffix)) {
                    song = song.removeSuffix(suffix).trim()
                }
            }
            if (song.isNotBlank() && resolveExercise(song) == null && song.length >= 2) {
                return Pair(VoiceIntent.PlayMusic(song, isPlaylist = false), "Playing $song on YouTube Music.")
            }
        }

        return null
    }

    private fun normalize(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace(Regex("[.,!?;:\"'()\\-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        for (kw in keywords) {
            if (this.contains(kw)) return true
        }
        return false
    }
}
