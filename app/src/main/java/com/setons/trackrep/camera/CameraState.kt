package com.setons.trackrep.camera

import androidx.camera.core.CameraSelector

enum class ExerciseFramingMode(val displayName: String, val recommendedDistance: String, val angleTip: String) {
    PUSH_UP("Push-up", "5–7 normal paces", "Floor level, tilted up ~15°"),
    SQUAT("Squat", "6–8 normal paces", "Knee/waist level, upright"),
    PLANK("Plank", "5–7 normal paces", "Floor level, tilted up ~15°")
}

enum class FramingStatus(val message: String, val isPassing: Boolean) {
    CALIBRATING("Aligning framing to exercise area...", false),
    TOO_CLOSE("Move back 5–7 steps for full body visibility", false),
    FRAMING_ALIGNED("Framing Aligned! Full body framed.", true),
    COUNTDOWN("Get ready! Starting workout...", true)
}

enum class CameraLens(val selector: CameraSelector) {
    BACK(CameraSelector.DEFAULT_BACK_CAMERA),
    FRONT(CameraSelector.DEFAULT_FRONT_CAMERA)
}
