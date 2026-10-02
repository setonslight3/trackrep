package com.setons.trackrep.exercise.catalog

import android.content.Context
import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.EquipmentType
import com.setons.trackrep.exercise.model.Exercise
import com.setons.trackrep.exercise.model.MuscleGroup
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages persistent storage and lifecycle for user-created Custom Exercises.
 * Custom movements are stored in internal JSON storage and seamlessly merged
 * into the main ExerciseCatalog with full AI Vision tracking enabled.
 */
class CustomExerciseRepository private constructor(private val context: Context) {

    private val customExercises = CopyOnWriteArrayList<Exercise>()
    private val storageFile = File(context.filesDir, FILE_NAME)

    init {
        loadFromDisk()
    }

    companion object {
        private const val FILE_NAME = "custom_exercises.json"

        @Volatile
        private var instance: CustomExerciseRepository? = null

        fun getInstance(context: Context): CustomExerciseRepository {
            return instance ?: synchronized(this) {
                instance ?: CustomExerciseRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun getAllCustom(): List<Exercise> {
        return customExercises.toList()
    }

    fun getById(id: String): Exercise? {
        return customExercises.find { it.id == id }
    }

    fun isCustom(id: String): Boolean {
        return customExercises.any { it.id == id }
    }

    fun save(exercise: Exercise) {
        val existingIndex = customExercises.indexOfFirst { it.id == exercise.id }
        if (existingIndex >= 0) {
            customExercises[existingIndex] = exercise
        } else {
            customExercises.add(exercise)
        }
        persistToDisk()
    }

    fun delete(exerciseId: String): Boolean {
        val removed = customExercises.removeIf { it.id == exerciseId }
        if (removed) {
            persistToDisk()
        }
        return removed
    }

    private fun loadFromDisk() {
        if (!storageFile.exists()) return

        try {
            val content = storageFile.readText(Charsets.UTF_8)
            if (content.isBlank()) return

            val array = JSONArray(content)
            val loaded = mutableListOf<Exercise>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val targetMuscle = try {
                    MuscleGroup.valueOf(obj.getString("targetMuscle"))
                } catch (_: Exception) {
                    MuscleGroup.CHEST
                }

                val secondaryMuscles = mutableListOf<MuscleGroup>()
                val secArray = obj.optJSONArray("secondaryMuscles")
                if (secArray != null) {
                    for (j in 0 until secArray.length()) {
                        try {
                            secondaryMuscles.add(MuscleGroup.valueOf(secArray.getString(j)))
                        } catch (_: Exception) {}
                    }
                }

                val difficulty = try {
                    DifficultyLevel.valueOf(obj.getString("difficulty"))
                } catch (_: Exception) {
                    DifficultyLevel.INTERMEDIATE
                }

                val equipment = try {
                    EquipmentType.valueOf(obj.optString("equipment", "BODYWEIGHT"))
                } catch (_: Exception) {
                    EquipmentType.BODYWEIGHT
                }

                val framingMode = if (obj.has("framingMode") && !obj.isNull("framingMode")) {
                    try {
                        ExerciseFramingMode.valueOf(obj.getString("framingMode"))
                    } catch (_: Exception) {
                        ExerciseFramingMode.PUSH_UP
                    }
                } else null

                val defaultSets = obj.optInt("defaultSets", 3)
                val defaultReps = obj.optInt("defaultReps", 10)
                val defaultHoldSeconds = obj.optInt("defaultHoldSeconds", 0)
                val restSeconds = obj.optInt("restSeconds", 60)

                val instructions = mutableListOf<String>()
                val instArray = obj.optJSONArray("instructions")
                if (instArray != null) {
                    for (j in 0 until instArray.length()) {
                        instructions.add(instArray.getString(j))
                    }
                }

                val commonFlaws = mutableListOf<String>()
                val flawsArray = obj.optJSONArray("commonFlaws")
                if (flawsArray != null) {
                    for (j in 0 until flawsArray.length()) {
                        commonFlaws.add(flawsArray.getString(j))
                    }
                }

                val proTip = obj.optString("proTip", "Focus on clean range of motion and core bracing.")

                loaded.add(
                    Exercise(
                        id = id,
                        name = name,
                        targetMuscle = targetMuscle,
                        secondaryMuscles = secondaryMuscles,
                        difficulty = difficulty,
                        equipment = equipment,
                        framingMode = framingMode,
                        defaultSets = defaultSets,
                        defaultReps = defaultReps,
                        defaultHoldSeconds = defaultHoldSeconds,
                        restSeconds = restSeconds,
                        instructions = instructions.ifEmpty { listOf("Execute with steady control and full range of motion.") },
                        commonFlaws = commonFlaws.ifEmpty { listOf("Loss of core tension") },
                        proTip = proTip
                    )
                )
            }

            customExercises.clear()
            customExercises.addAll(loaded)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistToDisk() {
        try {
            val array = JSONArray()
            for (ex in customExercises) {
                val obj = JSONObject().apply {
                    put("id", ex.id)
                    put("name", ex.name)
                    put("targetMuscle", ex.targetMuscle.name)
                    put("secondaryMuscles", JSONArray(ex.secondaryMuscles.map { it.name }))
                    put("difficulty", ex.difficulty.name)
                    put("equipment", ex.equipment.name)
                    if (ex.framingMode != null) {
                        put("framingMode", ex.framingMode.name)
                    }
                    put("defaultSets", ex.defaultSets)
                    put("defaultReps", ex.defaultReps)
                    put("defaultHoldSeconds", ex.defaultHoldSeconds)
                    put("restSeconds", ex.restSeconds)
                    put("instructions", JSONArray(ex.instructions))
                    put("commonFlaws", JSONArray(ex.commonFlaws))
                    put("proTip", ex.proTip)
                }
                array.put(obj)
            }
            storageFile.writeText(array.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
